package org.dspace.uclouvain.pdfAttestationGenerator.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("handler")
public class Handler {
    public String itemType;
    @XStreamAlias("beanName")
    public String className;
    @XStreamAlias("template")
    public String templateName;

    public Handler(String type, String className, String templateName) {
        this.itemType = type;
        this.className = className;
        this.templateName = templateName;
     }
}
