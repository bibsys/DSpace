/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.provider;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.uclouvain.external.importer.UCLouvainImportSourceService;

public class UCLouvainDataProvider extends AbstractExternalDataProvider {

    private UCLouvainImportSourceService metadataSource;
    private String sourceIdentifier;

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        return Optional.of(getExternalDataObject(metadataSource.getMetadataList(id), id));
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        return null;
    }

    @Override
    public boolean supports(String source) {
        return Objects.equals(source, sourceIdentifier);
    }

    @Override
    public int getNumberOfResults(String query) {
        return metadataSource.getResultCount(query);
    }

    private ExternalDataObject getExternalDataObject(List<MetadataValueDTO> metadataList, String id) {
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        externalDataObject.setMetadata(metadataList);
        return externalDataObject;
    }

    // GETTERS AND SETTERS =============================================================================================

    public UCLouvainImportSourceService getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(UCLouvainImportSourceService metadataSource) {
        this.metadataSource = metadataSource;
    }

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }
}
