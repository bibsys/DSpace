/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility methods for the type bind functionality.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 *
 */
public class TypeBindUtils {

    private static final ConfigurationService configurationService = DSpaceServicesFactory
            .getInstance().getConfigurationService();
    private static final ItemService itemService = ContentServiceFactory
            .getInstance().getItemService();
    private static final MetadataAuthorityService metadataAuthorityService = ContentAuthorityServiceFactory
            .getInstance().getMetadataAuthorityService();

    private TypeBindUtils() {}

    /**
     * This method gets the fields used for type-bind.
     * @return the field used for type-bind.
     */
    public static List<String> getTypeBindField() {
        return Arrays.asList(
            configurationService.getArrayProperty("submit.type-bind.field", new String[]{"dc.type"})
        );
    }

    /**
     * This method gets the values of the type-bind fields from the current item.
     * 
     * @param obj The object to extract type-bind values from.
     * @return the values for each type-bind fields from the current item.
     */
    public static HashMap<String, String> getTypeBindValues(InProgressSubmission<?> obj) {
        HashMap<String, String> response = new HashMap<>();
        for (String field: getTypeBindField()) {
            List<MetadataValue> typeBindFieldValues =
                itemService.getMetadataByMetadataString(obj.getItem(), field);

            if (typeBindFieldValues == null || typeBindFieldValues.isEmpty()
                || StringUtils.isBlank(typeBindFieldValues.get(0).getValue())) {
                continue;
            }

            MetadataValue typeBindValue = typeBindFieldValues.get(0);

            boolean isAuthorityAllowed = metadataAuthorityService.isAuthorityAllowed(
                field.replace(".","_"), Constants.ITEM, obj.getCollection()
            );
            if (isAuthorityAllowed && typeBindValue.getAuthority() != null) {
                response.put(field, typeBindValue.getAuthority());
                continue;
            }
            response.put(field, typeBindValue.getValue());
        }
        return response;
    }

}
