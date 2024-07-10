/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class Entity implements Comparable<Entity> {

    private String code;
    private String name;
    private EntityType type;
    private Set<Entity> children;
    private Entity parent;

    public Entity(@NotNull @NotEmpty String code, String name, @NotNull @NotEmpty EntityType type) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.children = new TreeSet<>();
    }
    public Entity(@NotNull @NotEmpty String code, @NotNull @NotEmpty EntityType type) {
        this(code, null, type);
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }

    public EntityType getType() {
        return type;
    }
    public void setType(EntityType type) {
        this.type = type;
    }

    public Set<Entity> getChildren() {
        return children;
    }
    public void addChild(Entity child) {
        this.children.add(child);
    }

    public Entity getParent() {
        return this.parent;
    }
    public void setParent(Entity parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        String output = "Entity{" +
            "code='" + code + "'" +
            ", name='" + name + "'" +
            ", type='" + type + "'";
        if (parent != null) {
            output += ", parent='" + parent.getCode() + "'";
        }
        output += '}';
        return output;
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity entity = (Entity) o;
        return entity.getCode().equals(code) && entity.getType().equals(type);
    }

    @Override
    public int compareTo(@NotNull Entity o) {
        return code.compareTo(o.getCode());
    }
}
