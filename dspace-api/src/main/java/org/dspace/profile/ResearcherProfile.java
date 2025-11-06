/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile;

import static org.dspace.core.Constants.READ;
import static org.dspace.eperson.Group.ANONYMOUS;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.util.UUIDUtils;
import org.springframework.util.Assert;

/**
 * Object representing a Researcher Profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfile {

    private final Item item;

    private final MetadataValue dspaceObjectOwner;

    public static final String ENTITY_TYPE = "Person";

    /**
     * Create a new ResearcherProfile object from the given item.
     *
     * @param  item                     the profile item
     * @throws IllegalArgumentException if the given item has not a dspace.object.owner
     *                                  metadata with a valid authority
     */
    public ResearcherProfile(Item item) {
        this(item, true);
    }

    /**
     * Create a new ResearcherProfile object from the given item.
     *
     * @param item The item to use as profile
     * @param hasOwner Whenever to assign an owner to the profile item.
     * @throws IllegalArgumentException if the given item has no dspace.object.owner
     *                                  metadata with a valid authority
     */
    public ResearcherProfile(Item item, boolean hasOwner) {
        Assert.notNull(item, "A researcher profile requires an item");
        this.item = item;
        this.dspaceObjectOwner = hasOwner ? getDspaceObjectOwnerMetadata(item) : null;
    }

    public UUID getId() {
        return UUIDUtils.fromString(dspaceObjectOwner.getAuthority());
    }

    /**
     * A profile is considered visible if accessible by anonymous users. This method
     * returns true if the given item has a READ policy related to ANONYMOUS group,
     * false otherwise.
     */
    public boolean isVisible() {
        return item.getResourcePolicies().stream()
            .filter(policy -> policy.getGroup() != null)
            .anyMatch(policy -> READ == policy.getAction() && ANONYMOUS.equals(policy.getGroup().getName()));
    }

    public Item getItem() {
        return item;
    }

    public Optional<String> getName() {
        return getMetadataValue(item, "dc.title")
            .map(MetadataValue::getValue);
    }

    public Optional<String> getOrcid() {
        return getMetadataValue(item, "person.identifier.orcid")
            .map(MetadataValue::getValue);
    }

    public Optional<String> getFGS() {
        return getMetadataValue(item, "person.identifier.fgs")
            .map(MetadataValue::getValue);
    }

    public Optional<String> getEmail() {
        return getMetadataValue(item, "person.email.official")
            .map(MetadataValue::getValue);
    }

    public Optional<String> getInstitution() {
        return getMetadataValue(item, "person.affiliation.institution")
            .map(MetadataValue::getValue);
    }

    private MetadataValue getDspaceObjectOwnerMetadata(Item item) {
        return getMetadataValue(item, "dspace.object.owner")
            .filter(metadata -> UUIDUtils.fromString(metadata.getAuthority()) != null)
            .orElseThrow(
                () -> new IllegalArgumentException("A profile item must have a valid dspace.object.owner metadata")
            );
    }

    private Optional<MetadataValue> getMetadataValue(Item item, String metadataField) {
        return getMetadataValues(item, metadataField).findFirst();
    }

    private Stream<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return item.getMetadata().stream()
            .filter(metadata -> metadataField.equals(metadata.getMetadataField().toString('.')));
    }

}
