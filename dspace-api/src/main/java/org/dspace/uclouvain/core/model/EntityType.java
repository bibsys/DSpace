package org.dspace.uclouvain.core.model;

public enum EntityType {

    DEGREE("degree"),
    FACULTY("faculty");

    public final String label;

    EntityType(String label) {
        this.label = label;
    }

    /**
     * Retrieve an EntityType from its label
     * @param label the entity type label to search.
     * @return The corresponding EntityType.
     * @throws IllegalArgumentException if no EntityType could be found.
     */
    public static EntityType fromLabel(String label) {
        for (EntityType type : EntityType.values()) {
            if (type.label.equals(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with label " + label);
    }
}