/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.element;

import java.util.Objects;

import org.dspace.uclouvain.content.snapshot.SnapshotElementType;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * This class represents a snapshot item comparaison element for an item metadata.
 * To be valid the original XML element must contain a "path" attribute and this path should be unique for the
 * corresponding item
 * Examples:
 *  <MetadataField @path="dc.contributor.author[0]">Doe, John</Field>
 *  <MetadataField @path="dc.subject[7]">foo</Field>
 *  <MetadataField @path="dc.subject[8]">bar</Field>
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@SnapshotElementType("MetadataField")
public class MetadataSnapshotElement extends SnapshotElement {

    private final String value;

    public MetadataSnapshotElement(Element element) {
        super(element);
        this.value = element.getTextContent();
    }
    public MetadataSnapshotElement(String path, String value) {
        this.path = path;
        this.value = value;
    }

    @Override
    protected String buildPath() {
        Assert.isTrue(attributes.containsKey("path"), "'path' is a required attribute");
        return this.attributes.get("path");
    }

    @Override
    public boolean isLogicallyEqualTo(SnapshotElement element) {
        return element != null
            && Objects.equals(path, element.getPath())
            && Objects.equals(getValue(), ((MetadataSnapshotElement) element).getValue());
    }

    @Override
    public String toString() {
        return "MetadataSnapshotElement{@path='" + path + "', @value='" + value + "'}";
    }

    // GETTER & SETTER =================================================================================================
    public String getValue() {
        return value;
    }
}
