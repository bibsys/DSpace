/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

/**
 * Representation model for a FedoraCommons legacy comment.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public class LegacyComment {

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",  // DateTimeFormatter.ISO_FORMAT
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "MM-dd-yyyy HH:mm:ss",
        "yyyy-MM-dd"
    };


    private String writer;
    private String content;
    private Date created;


    public String getWriter() {
        return writer;
    }
    public void setWriter(String writer) {
        this.writer = writer;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreated() {
        return created;
    }
    public void setCreated(String created) {
        try {
            this.created = DateUtils.parseDate(created, DATE_PATTERNS);
        } catch (ParseException e) {
            this.created = new Date(0);
        }
    }
}
