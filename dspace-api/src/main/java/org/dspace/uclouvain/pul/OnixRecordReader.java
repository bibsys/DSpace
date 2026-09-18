/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dspace.services.ConfigurationService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;

/**
 * Reads a PUL ONIX 3.0 file and applies the {@code ONIX} submission crosswalk to obtain its DIM.
 * <p>
 * The stylesheet is the one declared as {@code crosswalk.submission.ONIX.stylesheet} and used by
 * {@code XSLTIngestionCrosswalk} at ingestion time; it is applied here directly because that class only exposes
 * the transformation through an existing item, while the import needs the DIM before deciding what to do.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class OnixRecordReader {

    /** Same property as the {@code ONIX} ingestion crosswalk; the path is relative to {@code ${dspace.dir}/config}. */
    public static final String STYLESHEET_PROPERTY = "crosswalk.submission.ONIX.stylesheet";
    private static final Namespace DIM = Namespace.getNamespace("dim", "http://www.dspace.org/xmlns/dspace/dim");

    private final Transformer transformer;

    public OnixRecordReader(ConfigurationService configurationService) {
        this(new File(
            configurationService.getProperty("dspace.dir") + File.separator + "config",
            Objects.requireNonNull(
                configurationService.getProperty(STYLESHEET_PROPERTY),
                "'" + STYLESHEET_PROPERTY + "' configuration property is not defined"
            )
        ));
    }

    public OnixRecordReader(File stylesheet) {
        try {
            transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(stylesheet));
        } catch (TransformerException e) {
            throw new IllegalStateException("Cannot load the ONIX stylesheet " + stylesheet, e);
        }
    }

    /**
     * @param file an ONIX 3.0 message holding one product.
     * @return the record, with its DIM.
     * @throws IOException   if the file cannot be read.
     * @throws JDOMException if the file is not well-formed XML.
     * @throws TransformerException if the stylesheet fails on it.
     */
    public OnixRecord read(File file) throws IOException, JDOMException, TransformerException {
        SAXBuilder builder = new SAXBuilder(XMLReaders.NONVALIDATING);
        builder.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        builder.setExpandEntities(false);
        Document document = builder.build(file);

        JDOMResult result = new JDOMResult();
        transformer.transform(new JDOMSource(document), result);
        Element dim = result.getDocument().detachRootElement();

        Element product = document.getRootElement().getChild("Product");
        String notificationType = (product == null) ? null : product.getChildTextTrim("NotificationType");
        boolean unnamedContributors = product != null && product.getChild("DescriptiveDetail") != null
            && product.getChild("DescriptiveDetail").getChildren("Contributor").stream()
                .anyMatch(contributor -> contributor.getChild("UnnamedPersons") != null);
        return new OnixRecord(
            file,
            notificationType,
            firstValue(dim, "identifier", "gcoi"),
            values(dim, "identifier", "isbn"),
            firstValue(dim, "title", null),
            dim,
            unnamedContributors,
            coverUrl(product)
        );
    }

    /**
     * The front cover ({@code ResourceContentType 01}) in its smallest version: PUL publishes a ~125 px wide
     * {@code THUMBNAIL} and a ~1000 px {@code HIGHQ}; the width is feature type 03 of each version.
     */
    private static String coverUrl(Element product) {
        Element collateral = product == null ? null : product.getChild("CollateralDetail");
        if (collateral == null) {
            return null;
        }
        return collateral.getChildren("SupportingResource").stream()
            .filter(resource -> "01".equals(resource.getChildTextTrim("ResourceContentType")))
            .flatMap(resource -> resource.getChildren("ResourceVersion").stream())
            .filter(version -> !version.getChildTextTrim("ResourceLink").isEmpty())
            .min(Comparator.comparingInt(OnixRecordReader::widthOf)) // keep the smallest
            .map(version -> version.getChildTextTrim("ResourceLink"))
            .orElse(null);
    }

    private static int widthOf(Element version) {
        return version.getChildren("ResourceVersionFeature").stream()
            .filter(feature -> "03".equals(feature.getChildTextTrim("ResourceVersionFeatureType")))
            .map(feature -> feature.getChildTextTrim("FeatureValue"))
            .filter(value -> value.matches("\\d+"))
            .mapToInt(Integer::parseInt)
            .findFirst()
            .orElse(Integer.MAX_VALUE);
    }

    private static List<String> values(Element dim, String element, String qualifier) {
        return dim.getChildren("field", DIM).stream()
            .filter(field -> "dc".equals(field.getAttributeValue("mdschema"))
                && element.equals(field.getAttributeValue("element"))
                && Objects.equals(qualifier, field.getAttributeValue("qualifier")))
            .map(Element::getTextTrim)
            .toList();
    }

    private static String firstValue(Element dim, String element, String qualifier) {
        return values(dim, element, qualifier).stream().findFirst().orElse(null);
    }
}
