/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class acts as Data transfer object in which we can store data like in a
 * regular Bitstream object, but this one isn't saved in the DB. This can freely
 * be used to represent Bitstream without it being saved in the database, this
 * will typically be used when transferring data.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 *
 */
public class BitstreamDTO {

    private final String bundleName;

    private final int position;

    private final String location;

    private final List<MetadataValueDTO> metadataValues;

    private final List<ResourcePolicyDTO> resourcePolicies;

    public BitstreamDTO(String bundleName, int position, String location, List<MetadataValueDTO> metadataValues,
        List<ResourcePolicyDTO> resourcePolicies) {
        this.bundleName = bundleName;
        this.position = position;
        this.location = location;
        this.metadataValues = metadataValues;
        this.resourcePolicies = resourcePolicies;
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getLocation() {
        return location;
    }

    public List<MetadataValueDTO> getMetadataValues() {
        return metadataValues;
    }

    public List<ResourcePolicyDTO> getResourcePolicies() {
        return resourcePolicies;
    }

    public List<MetadataValueDTO> getMetadataValues(String metadataField) {
        return metadataValues.stream()
            .filter(metadataValue -> metadataValue.getMetadataField().equals(metadataField))
            .collect(Collectors.toList());
    }

    public int getPosition() {
        return position;
    }

}
