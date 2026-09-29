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
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.uclouvain.external.importer.UCLouvainImportSourceService;
import org.dspace.web.ContextUtil;

public class UCLouvainDataProvider extends AbstractExternalDataProvider {

    private UCLouvainImportSourceService metadataSource;
    private String sourceIdentifier;

    /**
     * Upstream entry point, reached from REST only (external source lookup, workspace item creation from an
     * external entry): the Context is the one of the current request.
     *
     * @throws IllegalStateException when no request is bound to the current thread. Code running outside a
     *         request (consumers, pollers, scripts) must call {@link #getExternalDataObject(Context, String)}.
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        if (context == null) {
            throw new IllegalStateException(
                "No request Context available for external source '" + sourceIdentifier
                + "': use getExternalDataObject(Context, String) outside of a request");
        }
        return getExternalDataObject(context, id);
    }

    /**
     * Retrieve the external data object using an explicit Context.
     *
     * @param context The DSpace Context of the caller (never a new one: a Context opened in a thread that already
     *                owns one shares its Hibernate session).
     * @param id The external identifier.
     * @return The external data object built from the source metadata.
     */
    @Override
    public Optional<ExternalDataObject> getExternalDataObject(Context context, String id) {
        return Optional.of(getExternalDataObject(metadataSource.getMetadataList(context, id), id));
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
