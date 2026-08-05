/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.element;

import java.util.Map;
import java.util.Objects;
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

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return isLogicallyEqualTo((SnapshotElement) o);
    }
    /**
     * DEV NOTE :: hashed on the path ALONE, deliberately.
     *   `equals` delegates to {@link #isLogicallyEqualTo}, which compares the path and the meaningful values -- never
     *   the raw `attributes` map. Hashing that map broke the equals/hashCode contract: the very same element carries
     *   `attributes` when it comes from the stored XML but not when it was just built in memory, so two elements that
     *   `equals` each other produced different hash codes. Path only is enough (equal elements always share it) and
     *   discriminating enough, since a path identifies one metadata occurrence or one bitstream.
     */
    @Override
    public final int hashCode() {
        return Objects.hashCode(path);
    }

    /**
     * Detect if an element is logically similar to this element.
     * @param element the element to compare
     * @return true if the element is the same, false otherwise
     */
    public abstract boolean isLogicallyEqualTo(SnapshotElement element);

    /**
     * The key uniquely identifying this element inside a snapshot.
     * DEV NOTE ::
     *   The path alone is NOT enough: nothing forbids a metadata occurrence and a bitstream from sharing one.
     *   This is the single definition of that key -- both the snapshot comparison and the diff index must use it,
     *   otherwise two elements would be paired on one side and collide on the other.
     *
     * @return the key identifying this element among the elements of a same snapshot
     */
    public final String getKey() {
        return getClass().getName() + "::" + path;
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
