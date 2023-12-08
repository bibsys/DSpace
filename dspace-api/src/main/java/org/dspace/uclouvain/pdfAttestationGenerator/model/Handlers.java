package org.dspace.uclouvain.pdfAttestationGenerator.model;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/** 
* Model representing the PDF attestation configuration file shape.
*/
@XStreamAlias("handlers")
public class Handlers {
    
    @XStreamImplicit(itemFieldName="handler")
    public List<Handler> handlers = new ArrayList<Handler>();

    public void addHandler(String type, String className, String templateName) {
        this.handlers.add(new Handler(type, className, templateName));
    }
}
