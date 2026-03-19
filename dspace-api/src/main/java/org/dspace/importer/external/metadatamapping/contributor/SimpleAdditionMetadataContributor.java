/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.drew.lang.annotations.NotNull;
import org.dspace.content.MetadataFieldName;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;

/**
 * Metadata contributor that add as many as specific metadatum as super class creates.
 * It is useful to add linked metadata to a real-extracted metadata
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SimpleAdditionMetadataContributor extends SimpleMetadataContributor {

    protected Map<MetadataFieldName, String> additionalMetadata = new HashMap<>();

    @Override
    public Collection<MetadatumDTO> contributeMetadata(PlainMetadataSourceDto t) {
        Collection<MetadatumDTO> baseMetadata = super.contributeMetadata(t);
        // Ensure we work on a modifiable list to avoid UnsupportedOperationException
        List<MetadatumDTO> result = new ArrayList<>(baseMetadata);
        int baseCount = baseMetadata.size();
        if (baseCount == 0 || additionalMetadata.isEmpty()) {
            return result;
        }
        // For each entry in additionalMetadata, create N copies (N = baseCount)
        List<MetadatumDTO> additions = additionalMetadata.entrySet().stream()
            .flatMap(entry -> Stream
                .generate(() -> createAdditionField(entry.getKey(), entry.getValue()))
                .limit(baseCount))
            .toList();

        result.addAll(additions);
        return result;
    }

    private MetadatumDTO createAdditionField(MetadataFieldName mdField, String mdValues) {
        MetadatumDTO md = new MetadatumDTO();
        md.setSchema(mdField.schema);
        md.setElement(mdField.element);
        md.setQualifier(mdField.qualifier);
        md.setValue(mdValues);
        return md;
    }

    // SETTER ==========================================================================================================
    public void setAdditionalMetadata(@NotNull Map<String, String> inputMetadata) {
        this.additionalMetadata = Optional.ofNullable(inputMetadata)
            .orElse(Collections.emptyMap())
            .entrySet().stream()
            .collect(Collectors.toMap(
                    entry -> new MetadataFieldName(entry.getKey()),
                    Map.Entry::getValue
            ));
    }
}
