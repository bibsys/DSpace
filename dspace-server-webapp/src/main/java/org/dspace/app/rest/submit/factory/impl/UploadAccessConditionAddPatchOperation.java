/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.dspace.validation.UploadValidator.DEFAULT_ACCESS_CONDITIONS_ACK_FIELD;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.BooleanUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission file access conditions acknowledgement "add" PATCH operation

 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "add", "path": "/sections/upload/acknowledgement", "value":"true"}]'
 * </code>
 *
 * Please note that according to the JSON Patch specification RFC6902, a later add operation on the
 * "acknowledgement" path will have the effect to replace the previous granted acknowledgement with a new one.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UploadAccessConditionAddPatchOperation extends AddPatchOperation<String> {

    @Autowired
    ItemService itemService;
    @Autowired
    MetadataFieldService metadataFieldService;
    @Autowired
    ConfigurationService configurationService;

    @Override
    void add(
        Context context,
        HttpServletRequest currentRequest,
        InProgressSubmission source,
        String path,
        Object value
    ) throws Exception {

        Boolean acknowledgement = (value instanceof String)
            ? BooleanUtils.toBooleanObject((String) value)
            : (Boolean) value;

        if (acknowledgement == null) {
            throw new IllegalArgumentException("Value is not a valid boolean expression");
        }
        Item item = source.getItem();
        MetadataFieldName mdField = new MetadataFieldName(configurationService.getProperty(
            "webui.submit.upload.acknowledgement.field",
            DEFAULT_ACCESS_CONDITIONS_ACK_FIELD
        ));
        // remove any existing acknowledgement (just in case the user accepted it previously)
        itemService.clearMetadata(context, item, mdField.schema, mdField.element, mdField.qualifier, Item.ANY);
        if (acknowledgement) {
            itemService.setMetadataSingleValue(
                context, item,
                mdField.schema, mdField.element, mdField.qualifier, Item.ANY,
                acknowledgement.toString()
            );
        }
    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }
}
