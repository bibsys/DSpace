package org.dspace.uclouvain.pdfAttestationGenerator.handlers;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.PDFGenerationException;


public interface PDFAttestationGeneratorHandler {
    public void getAttestation(OutputStream out, UUID uuid) throws PDFGenerationException;
    public InputStream getAttestationAsInputStream(UUID uuid) throws PDFGenerationException;
    public String getAttestationTemplateName();
}
