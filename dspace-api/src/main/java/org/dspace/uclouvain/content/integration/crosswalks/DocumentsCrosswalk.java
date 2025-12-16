/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Iterator;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.integration.crosswalks.DocumentCrosswalk;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.core.Context;

/**
 * Implementation of {@link ItemExportCrosswalk} to produce a document in a
 * specific format (pdf, rtf etc...) from a list of item.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DocumentsCrosswalk extends DocumentCrosswalk {

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {
        ByteArrayInputStream xmlInputStream = getItemsAsXml(context, dsoIterator);
        try {
            transformToDocument(out, xmlInputStream);
        } catch (Exception e) {
            throw new CrosswalkException(e);
        }
    }

    private ByteArrayInputStream getItemsAsXml(Context context, Iterator<? extends DSpaceObject> items)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, items, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public CrosswalkMode getCrosswalkMode() {
        return CrosswalkMode.MULTIPLE;
    }
}
