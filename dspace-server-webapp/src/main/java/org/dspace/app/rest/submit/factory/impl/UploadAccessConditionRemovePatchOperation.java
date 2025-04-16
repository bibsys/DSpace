/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import static org.dspace.validation.UploadValidator.DEFAULT_ACCESS_CONDITIONS_ACK_FIELD;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Submission file access conditions acknowledgement "remove" patch operation.

 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/submission/workspaceitems/31599 -H "Content-Type:
 * application/json" -d '[{ "op": "remove", "path": "/sections/upload/acknowledgement"}]'
 * </code>
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UploadAccessConditionRemovePatchOperation extends RemovePatchOperation<String> {

    @Autowired
    ItemService itemService;
    @Autowired
    MetadataFieldService metadataFieldService;
    @Autowired
    ConfigurationService configurationService;

    @Override
    void remove(
        Context context,
        HttpServletRequest currentRequest,
        InProgressSubmission source, String path,
        Object value
    ) throws Exception {
        Item item = source.getItem();
        MetadataFieldName mdField = new MetadataFieldName(configurationService.getProperty(
            "webui.submit.upload.acknowledgement.field",
            DEFAULT_ACCESS_CONDITIONS_ACK_FIELD
        ));
        itemService.clearMetadata(context, item, mdField.schema, mdField.element, mdField.qualifier, Item.ANY);
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
