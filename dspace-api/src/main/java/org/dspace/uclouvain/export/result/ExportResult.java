/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.result;

import java.io.InputStream;

/**
 * Representation of an export result.
 * This is a 'AutoCloseable', the resource can be handled in a 'try-with-resource' block and
 * will be automatically closed (by java) when consumed.
 * 
 * Authored-by: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public interface ExportResult extends AutoCloseable {

    public InputStream readStream() throws Exception;
    public String getMimeType();
    public long getSize();
    public String getFileName();

    /**
     * The close has to be override to remove the resource once it has been consumed.
     */
    @Override
    public void close();
}
