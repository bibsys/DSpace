/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.handlers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.PDFGenerationException;


public interface PDFAttestationGeneratorHandler {
    void getAttestation(OutputStream out, UUID uuid) throws PDFGenerationException;
    InputStream getAttestationAsInputStream(UUID uuid) throws PDFGenerationException;
    String getAttestationTemplateName();
}
