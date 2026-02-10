/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.tracking;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;

/**
 * Simple record class used to create a snapshot of a {@link MetadataValue} to be not affected by hibernate changes
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public record MetadataValueSnapshot(
    MetadataField metadataField,
    String value,
    String language,
    int place,
    String authority,
    int confidence,
    int securityLevel
) {

    public MetadataValueSnapshot(MetadataValue mv) {
        this(
            mv.getMetadataField(),
            mv.getValue(),
            mv.getLanguage(),
            mv.getPlace(),
            mv.getAuthority(),
            mv.getConfidence(),
            (mv.getSecurityLevel() != null) ? mv.getSecurityLevel() : 0
        );
    }

    public String getFieldName() {
        return Stream.of(
            metadataField.getMetadataSchema().getName(),
            metadataField.getElement(),
            metadataField.getQualifier()
        ).filter(Objects::nonNull).collect(Collectors.joining("."));
    }

    public String getKey() {
        return "%s[%d]".formatted(getFieldName(), place);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MetadataValueSnapshot otherSnapshot)) {
            return false;
        }
        return Objects.equals(this.value, otherSnapshot.value) &&
            Objects.equals(this.language, otherSnapshot.language) &&
            Objects.equals(this.authority, otherSnapshot.authority) &&
            this.confidence == otherSnapshot.confidence &&
            this.securityLevel == otherSnapshot.securityLevel;
    }

    @Override
    public String toString() {
        return "%s :: %s".formatted(getKey(), value);
    }
}
