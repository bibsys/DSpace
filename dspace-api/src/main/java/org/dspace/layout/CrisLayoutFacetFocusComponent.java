package org.dspace.layout;

/**
 * This class represents a focus component for a given facet in a cris layout.
 * It is used to display the results with their number of occurrences for a specific facet in the form of a list.
 */
public class CrisLayoutFacetFocusComponent implements CrisLayoutSectionComponent {
    private String discoveryConfigurationName;

    private String style;

    private String targetFacet;

    // GETTERS AND SETTERS
    public String getDiscoveryConfigurationName() {
        return discoveryConfigurationName;
    }

    public void setDiscoveryConfigurationName(String discoveryConfigurationName) {
        this.discoveryConfigurationName = discoveryConfigurationName;
    }

    @Override
    public String getStyle() {
        return this.style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getTargetFacet() {
        return this.targetFacet;
    }

    public void setTargetFacet(String targetFacet) {
        this.targetFacet = targetFacet;
    }
}
