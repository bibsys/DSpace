/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.converter;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.core.convert.converter.Converter;

/**
 * Data converter to used to decode HTML encoded character except for XML special characters.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 *
 */
public class SafeHTMLXmlValueConverter implements Converter<String, String> {

    private static final String P_LT = "__PRESERVE_LT__";
    private static final String P_GT = "__PRESERVE_GT__";
    private static final String P_AMP = "__PRESERVE_AMP__";

    @Override
    public String convert(String source) {
        //safe replace
        source = source
            .replace("&lt;", P_LT)
            .replace("&gt;", P_GT)
            .replace("&amp;", P_AMP);
        String unescaped = StringEscapeUtils.unescapeHtml4(source);
        // safe restore
        return unescaped
            .replace(P_LT, "&lt;")
            .replace(P_GT, "&gt;")
            .replace(P_AMP, "&amp;");
    }
}
