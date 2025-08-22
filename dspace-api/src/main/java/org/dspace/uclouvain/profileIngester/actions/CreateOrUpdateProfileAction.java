/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.actions;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.external.esb.model.ESBPersonProfile;
import org.dspace.uclouvain.profileIngester.actions.configuration.ActionField;
import org.dspace.uclouvain.profileIngester.actions.configuration.ActionFieldMappingConfiguration;
import org.dspace.uclouvain.profileIngester.exceptions.ProfileActionException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to create or update a profile based on data present in a person event model.
 * A new profile is created only if no other profile with the same fgs exists.
 * If a profile already exists with this fgs identifier, update its metadata.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class CreateOrUpdateProfileAction extends ProfileAction {
    @Autowired
    ActionFieldMappingConfiguration actionFieldMappingConfiguration;

    /**
     * Extract the fgs from the provided event:
     * - If no other profile exist with the fgs, create one using the provided data.
     * - If a profile already exists, exit the action.
     * Perform a commit in the database only if a change occurred.
     * 
     * @param context The current DSpace context.
     * @param event The data of the event to process.
     * @throws ProfileActionException If an error occurred and the profile could not be created.
     */
    public void process(Context context, PersonEventModel event) throws ProfileActionException {
        String fgs = event.getFgs();
        try {
            Item profile = uclouvainProfileService.findById(context, fgs);
            ESBPersonProfile profileData = esbClient.getProfileForFGS(fgs);
            if (profileData.getEmail() == null) {
                logger.info("[CANCEL PROCESSING] No email found for the fgs \"" + fgs + "\" aborting processing...");
                return;
            }
            boolean changed = false;
            if (profile == null) {
                profile = uclouvainProfileService.createEmptyProfile(context, fgs);
                changed = processFields(context, profileData, profile, true);
                logger.info("[CREATE PROFILE] Created fresh new profile for fgs " + fgs);
            } else {
                changed = processFields(context, profileData, profile, false);
                logger.info("[UPDATE PROFILE] Updated profile for fgs " + fgs);
            }

            if (changed) {
                // Update and commit to apply changes
                itemService.update(context, profile);
                context.commit();
            }
        } catch (Exception e) {
            throw new ProfileActionException("Could not create the desired profile: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * For a given profile data and profile item, perform an action on each data mapped in the configuration.
     * Each field in the ESBPersonProfile class can be bound to a specific metadata field.
     * The property value and the metadata field go to a 'alterField()' method that has to be implemented separately.
     * This method can perform a change on the profile item.
     * If any changes happened, the 'alterField()' method has to return true.
     * 
     * @param context The current DSpace context.
     * @param profileData The data to use to update the person profile.
     * @param profile The profile that exists in DSpace.
     * @param createMode Is the profile being created, otherwise it will be updated?
     * @return True if the profile was modified, false otherwise.
     */
    protected boolean processFields(Context context, ESBPersonProfile profileData, Item profile, boolean createMode) {
        boolean changed = false;
        for (Entry<String, ActionField> entry: actionFieldMappingConfiguration.getFieldMapping().entrySet()) {
            String classField = entry.getKey();
            ActionField metadataFields = entry.getValue();

            PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(ESBPersonProfile.class, classField);
            if (descriptor == null || descriptor.getReadMethod() == null) {
                throw new RuntimeException("Bad configuration for action field mapping: \""
                    + classField + "\" does not exists on class " + ESBPersonProfile.class);
            }
            try {
                Object propertyValue = descriptor.getReadMethod().invoke(profileData);
                if (propertyValue != null) {
                    if (createMode) {
                        changed = createField(
                            context, profile,
                            propertyValue.toString(), metadataFields
                        ) || changed;
                    } else {
                        changed = updateField(
                            context, profile,
                            propertyValue.toString(), metadataFields
                        ) || changed;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                logger.warn("Cannot access configured property \"" + classField + "\" value on EBSProfileClass", e);
                continue;
            }
        }
        return changed;
    }

    /**
     * Create a metadata value for a specific action field in a new profile.
     * 
     * @param context The current DSpace context.
     * @param profile The profile to create the field in.
     * @param value The value to set to the new metadata value.
     * @param actionFields A field configuration.
     * @return A boolean to indicate if a change occurred on the profile item.
     */
    protected boolean createField(Context context, Item profile, String value, ActionField actionField) {
        MetadataField mdField = new MetadataField(actionField.getField());
        String currentMetadataValue = itemService.getMetadataFirstValue(profile, mdField, null);
        try {
            applyCopyFields(context, profile, currentMetadataValue, actionField.getCopyFields(), value);
            itemService.addSecuredMetadata(
                context, profile,
                mdField.schema, mdField.element, mdField.qualifier,
                null, value, null, 0, actionField.getSecurity()
            );
            return true;
        } catch (SQLException e) {
            logger.error(
                "[CREATE PROFILE ERROR] Cannot add value to configured metadata field", e
            );
        }
        return false;
    }

    /**
     * Update a metadata value for a specific action field in an existing profile.
     * 
     * @param context The current DSpace context.
     * @param profile The profile to update the field of.
     * @param value The value to set to the existing metadata value.
     * @param actionFields A field configuration.
     * @return A boolean to indicate if a change occurred on the profile item.
     */
    protected boolean updateField(Context context, Item profile, String value, ActionField actionField) {
        MetadataField mdField = new MetadataField(actionField.getField());
        String currentMetadataValue = itemService.getMetadataFirstValue(profile, mdField, null);
        if (!value.equals(currentMetadataValue)) {
            try {
                applyCopyFields(context, profile, currentMetadataValue, actionField.getCopyFields(), value);
                MetadataUtils.setSecuredMetadataSingleValue(
                    context, profile,
                    mdField.schema, mdField.element, mdField.qualifier,
                    null, value, null, 0, actionField.getSecurity()
                );
                logger.info("[UPDATE PROFILE] Updated " + mdField.getFullString() + " with new value.");
                return true;
            } catch (SQLException e) {
                logger.error(
                    "[UPDATE PROFILE ERROR] Could not update configured metadata field", e
                );
            }
        }
        return false;
    }

    /**
     * For a given {@link ActionField}, browse the configured copy fields and update their value if they still are in sync with the 'source' field value.
     * 
     * @param context The current DSpace context.
     * @param profile The profile to update the metadata of.
     * @param sourceMetadataValue The value of the source metadata field.
     * @param targetFields The fields that potentially need to be updated.
     * @param value The value to use to update a field.
     * @throws SQLException
     */
    private void applyCopyFields(
        Context context, Item profile, String sourceMetadataValue, List<ActionField> targetFields, String value
    ) throws SQLException {
        if (!targetFields.isEmpty()) {
            for (ActionField field: targetFields) {
                MetadataField targetField = new MetadataField(field.getField());
                String targetMetadataValue = itemService.getMetadataFirstValue(profile, targetField, null);
                if (Objects.equals(targetMetadataValue, sourceMetadataValue)) {
                    // Update the field value to match the source metadata field.
                    MetadataUtils.setSecuredMetadataSingleValue(
                        context, profile,
                        targetField.schema, targetField.element, targetField.qualifier,
                        null, value, null, 0, field.getSecurity()
                    );
                }
            }
        }
    }
}
