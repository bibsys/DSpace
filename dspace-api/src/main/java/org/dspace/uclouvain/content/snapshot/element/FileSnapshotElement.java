/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.element;

import java.util.Map;
import java.util.UUID;

import org.dspace.uclouvain.content.snapshot.SnapshotElementType;
import org.springframework.util.Assert;
import org.w3c.dom.Element;


/**
 * This class represent a snapshot about a bitstream.
 * This element could be used to compare changes on bitstream. Tracked element are
 *   - bitstream uuid: the primary key of the bitstream (used to identify uniquely bitstream)
 *   - bitstream name: the label of the bitstream
 *   - bitstream checksum: the checksum build from the bitstream content
 *   - access: the access level assigned to the bitstream (closed, embargo, restricted, open-access)
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@SnapshotElementType("File")
public class FileSnapshotElement extends SnapshotElement {

    public FileSnapshotElement(Element element) {
        super(element);
    }
    public FileSnapshotElement(UUID id, String name, String checksum, String access) {
        attributes = Map.of(
            "uuid", id.toString(),
            "name", name,
            "checksum", checksum,
            "access", access
        );
        this.path = buildPath();
    }

    @Override
    protected String buildPath() {
        Assert.isTrue(attributes.containsKey("name"), "'name' is a required attribute");
        return getFilename();
    }

    @Override
    public String toString() {
        return String.format(
            "FileSnapshotElement{@uuid=%s, @name=%s, @checksum=%s, @access=%s}",
            getUUID(), getFilename(), getChecksum(), getAccess()
        );
    }

    // GETTER & SETTER =================================================================================================
    public UUID getUUID() {
        return UUID.fromString(attributes.get("uuid"));
    }
    public String getFilename() {
        return attributes.get("name");
    }
    public String getChecksum() {
        return attributes.get("checksum");
    }
    public String getAccess() {
        return attributes.get("access");
    }
}
