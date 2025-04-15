/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.model.UploadConfiguration;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.validation.model.ValidationError;

/**
 * Execute file required check validation
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4sciente.it)
 */
public class UploadValidator implements SubmissionStepValidator {

    public static final String DEFAULT_ACCESS_CONDITIONS_ACK_FIELD = "dspace.file-access-condition.acknowledgement";
    private static final String ERROR_VALIDATION_FILEREQUIRED = "error.validation.filerequired";
    private static final String ERROR_VALIDATION_ACCESSCONDITIONSREQUIRED = "error.validation.accessconditionsrequired";

    private ItemService itemService;
    private UploadConfigurationService uploadConfigurationService;
    private ConfigurationService configurationService;
    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        //TODO MANAGE METADATA
        List<ValidationError> errors = new ArrayList<>();
        String IdSectionPath = "/" + OPERATION_PATH_SECTIONS + "/" + config.getId();
        UploadConfiguration uploadConfig = uploadConfigurationService.getMap().get(config.getId());
        if (uploadConfig.isRequired() && hasNotUploadedFiles(obj.getItem())) {
            addError(errors, ERROR_VALIDATION_FILEREQUIRED, IdSectionPath);
        }
        if (isAccessAcknowledgeRequired() && !isFileAccessConditionsValidated(obj.getItem())) {
            addError(errors, ERROR_VALIDATION_ACCESSCONDITIONSREQUIRED, IdSectionPath);
        }
        return errors;
    }

    private boolean hasNotUploadedFiles(Item item) {
        try {
            return !itemService.hasUploadedFiles(item);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private boolean isAccessAcknowledgeRequired() {
        return configurationService.getBooleanProperty("webui.submit.upload.acknowledgement.required", false);
    }

    private boolean isFileAccessConditionsValidated(Item item) {
        try {
            MetadataFieldName mdField = new MetadataFieldName(configurationService.getProperty(
                "webui.submit.upload.acknowledgement.field",
                DEFAULT_ACCESS_CONDITIONS_ACK_FIELD
            ));
            String metadataValue = itemService.getMetadataFirstValue(item, mdField, Item.ANY);
            return (metadataValue != null && Boolean.parseBoolean(metadataValue));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public UploadConfigurationService getUploadConfigurationService() {
        return uploadConfigurationService;
    }

    public void setUploadConfigurationService(UploadConfigurationService uploadConfigurationService) {
        this.uploadConfigurationService = uploadConfigurationService;
    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService service) {
        this.configurationService = service;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
