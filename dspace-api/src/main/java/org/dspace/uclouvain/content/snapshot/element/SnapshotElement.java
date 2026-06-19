/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.element;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Basic representation of a snapshot element.
 * Any snapshot element must define a `path` allowing to identify the related item element:
 *   - metadata field name
 *   - filename
 *   - ...
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class SnapshotElement {

    protected Element originalElement;
    protected Map<String, String> attributes;
    protected String path;

    // CONSTRUCTOR =====================================================================================================
    protected SnapshotElement() { }
    public SnapshotElement(Element element) {
        this.originalElement = element;
        this.attributes = this.getAllAttributes(element);
        this.path = this.buildPath();
    }

    protected abstract String buildPath();

    // CLASS METHODS ===================================================================================================
    /**
     * Extract all attributes from an Element as a Map
     * @param element The Element to analyze
     * @return the Map containing all attributes
     */
    private Map<String, String> getAllAttributes(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        return IntStream.range(0, attributes.getLength())
            .mapToObj(i -> (Attr) attributes.item(i))
            .collect(Collectors.toMap(Attr::getName, Attr::getValue));
    }

    // GETTER & SETTER =================================================================================================
    public String getPath() {
        return this.path;
    }
    public Map<String, String> getAttributes() {
        return this.attributes;
    }
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }
}
