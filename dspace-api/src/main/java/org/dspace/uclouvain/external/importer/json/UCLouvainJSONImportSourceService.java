/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer.json;

import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.ReadContext;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.uclouvain.external.importer.UCLouvainImportSourceServiceImpl;
public abstract class UCLouvainJSONImportSourceService extends UCLouvainImportSourceServiceImpl {
    public abstract List<MetadataValueDTO> getMetadataList(String query);

    /**
     * Get the first string value for a given json context and path.
     * 
     * @param root THe root json node to extract string from.
     * @param path The path to follow to get the string value.
     * @return The first string value of the found node based on the given path and root node.
     */
    protected String getFirst(ReadContext root, String path) {
        return Optional.ofNullable(root.read(path))
            .map(node -> {
                if (node instanceof String s) {
                    return (String) s;
                }

                if (node instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    return first instanceof String s ? s : null;
                }
                return null;
            }).orElse(null);
    }
}
