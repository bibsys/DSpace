/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
