/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

import java.util.ArrayList;
import java.util.List;


public class MasterThesisAuthor {

    public String name;
    public String email;
    public List<MasterThesisAuthorIdentifier> identifiers = new ArrayList<>();
    public String institution;

    public void addIdentifier(String id_type, String value) {
        identifiers.stream().filter(id -> id.type.equalsIgnoreCase(id_type)).findFirst().ifPresent(identifiers::remove);
        identifiers.add(new MasterThesisAuthorIdentifier(id_type, value));
    }
}

class MasterThesisAuthorIdentifier {

    public String type;
    public String value;

    public MasterThesisAuthorIdentifier(String type, String value) {
        this.type = type;
        this.value = value;
    }
}
