/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer.xml;

import java.util.List;
import java.util.Optional;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.uclouvain.external.importer.UCLouvainImportSourceServiceImpl;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * External import service specialized for XML sources.
 */
public abstract class UCLouvainXMLImportSourceService extends UCLouvainImportSourceServiceImpl {
    public abstract List<MetadataValueDTO> getMetadataList(String query);

    /**
     * Get the value of the first matching element of a given XML tree.
     * 
     * @param root  The XML element tree to extract the value from.
     * @param xpath The path to the element to extract the value of.
     * @return The value of the first found element. Can return null if no element is found.
     */
    protected String getFirstText(Element root, String xpath) {
        return Optional
            .ofNullable(buildXpath(xpath).evaluateFirst(root))
            .map(Element::getTextTrim)
            .orElse(null);
    }

    /**
     * Get all the value of every element matching a given xpath for a given XML tree.
     * 
     * @param root  The XML element tree to extract the value from.
     * @param xpath The path to the elements to extract the value of.
     * @return The values of the all found elements. Can return an empty list if no elements are found.
     */
    protected List<String> getAllText(Element root, String xpath) {
        return buildXpath(xpath).evaluate(root)
            .stream()
            .map(Element::getTextTrim)
            .toList();
    }

    /**
     * Little helper to build an XPath expression.
     * 
     * @param path The xpath expression.
     * @return An new XpathExpression object.
     */
    protected XPathExpression<Element> buildXpath(String path) {
        return XPathFactory.instance().compile(path, Filters.element(), null, getNamespaces());
    }

    protected XPathExpression<Element> buildXpath(String path, List<Namespace> namespaces) {
        return XPathFactory.instance().compile(path, Filters.element(), null, namespaces);
    }

    /**
     * Get available namespace for a specific XML source.
     * @return A list of all needed namespace for a specific XML source.
     */
    protected List<Namespace> getNamespaces() {
        return List.of();
    }
}
