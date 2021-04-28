/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;

import java.util.List;

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.MetadataValue;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;

/**
 * Implementation of {@link OrcidProfileSectionFactory} that model an personal
 * external id.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidPersonExternalIdentifierFactory extends OrcidSimpleValueObjectFactory {

    private final List<String> externalIdTypes;

    public OrcidPersonExternalIdentifierFactory(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference, String metadataFields, String externalIdType) {

        super(sectionType, preference, metadataFields);
        this.externalIdTypes = externalIdType != null ? asList(externalIdType.split(",")) : emptyList();

        if (getExternalIdTypes().size() != getMetadataFields().size()) {
            throw new IllegalArgumentException("The external id types configuration is not compliance with "
                + "the metadata fields configuration");
        }
    }

    public List<String> getExternalIdTypes() {
        return externalIdTypes;
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(EXTERNAL_IDS);
    }

    @Override
    protected Object create(MetadataValue metadataValue) {

        String currentMetadataField = metadataValue.getMetadataField().toString('.');
        int metadataFieldIndex = metadataFields.indexOf(currentMetadataField);

        if (metadataFieldIndex < 0) {
            throw new IllegalArgumentException("Metadata field not supported: " + currentMetadataField);
        }

        PersonExternalIdentifier externalId = new PersonExternalIdentifier();
        externalId.setValue(metadataValue.getValue());
        externalId.setType(externalIdTypes.get(metadataFieldIndex));
        externalId.setRelationship(Relationship.SELF);
        externalId.setUrl(new Url(metadataValue.getValue()));

        return externalId;
    }

}
