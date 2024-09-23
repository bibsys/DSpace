package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutSectionRest.CrisLayoutFacetFocusComponentRest;
import org.dspace.layout.CrisLayoutFacetFocusComponent;
import org.dspace.layout.CrisLayoutSectionComponent;
import org.springframework.stereotype.Component;

/**
 * Rest Converter for the layout component {@link CrisLayoutFacetFocusComponent}.
 */
@Component
public class CrisLayoutFacetFocusComponentConverter implements CrisLayoutSectionComponentConverter {
    @Override
    public boolean support(CrisLayoutSectionComponent component) {
        return component instanceof CrisLayoutFacetFocusComponent;
    }

    @Override
    public CrisLayoutFacetFocusComponentRest convert(CrisLayoutSectionComponent component) {
        CrisLayoutFacetFocusComponent facetFocusComponent = (CrisLayoutFacetFocusComponent) component;
        CrisLayoutFacetFocusComponentRest rest = new CrisLayoutFacetFocusComponentRest();
        rest.setDiscoveryConfigurationName(facetFocusComponent.getDiscoveryConfigurationName());
        rest.setStyle(facetFocusComponent.getStyle());
        rest.setTargetFacet(facetFocusComponent.getTargetFacet());
        return rest;
    }
}
