/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

/**
 * Table-driven tests of {@link IdentifierNormalizer}: raw value as typed or imported, expected canonical forms.
 */
public class IdentifierNormalizerTest {

    @Test
    public void isbn13IsIndexedWithItsIsbn10Form() {
        List<String> expected = List.of("9782390611387", "2390611389");
        assertEquals(expected, IdentifierNormalizer.ISBN.normalize("978-2-39061-138-7"));
        assertEquals(expected, IdentifierNormalizer.ISBN.normalize("9782390611387"));
        assertEquals(expected, IdentifierNormalizer.ISBN.normalize("978-2390611387"));
        assertEquals(expected, IdentifierNormalizer.ISBN.normalize(" 978 2 39061 138 7 "));
    }

    @Test
    public void isbn10IsIndexedWithItsIsbn13Form() {
        assertEquals(List.of("2390611389", "9782390611387"), IdentifierNormalizer.ISBN.normalize("2-39061-138-9"));
        assertEquals(List.of("080442957X", "9780804429573"), IdentifierNormalizer.ISBN.normalize("0-8044-2957-x"));
    }

    @Test
    public void isbn979HasNoIsbn10Form() {
        assertEquals(List.of("9791032300001"), IdentifierNormalizer.ISBN.normalize("979-10-323-0000-1"));
    }

    @Test
    public void invalidIsbnIsIndexedAsIsWithoutConversion() {
        assertEquals(List.of("9782390611388"), IdentifierNormalizer.ISBN.normalize("978-2-39061-138-8"));
        assertEquals(List.of("12345"), IdentifierNormalizer.ISBN.normalize("12345"));
    }

    @Test
    public void issnKeepsDigitsAndCheckCharacter() {
        assertEquals(List.of("20493630"), IdentifierNormalizer.ISSN.normalize("2049-3630"));
        assertEquals(List.of("1050124X"), IdentifierNormalizer.ISSN.normalize("issn 1050-124x"));
    }

    @Test
    public void numericIdentifiersKeepDigitsOnly() {
        assertEquals(List.of("12345678"), IdentifierNormalizer.PMID.normalize("PMID: 12345678"));
        assertEquals(List.of("12345678"), IdentifierNormalizer.PMID.normalize("pmid://12345678"));
        assertEquals(List.of("29303100123456"), IdentifierNormalizer.GCOI.normalize("GCOI 29303100123456"));
    }

    @Test
    public void scopusEidAndBareIdAreTheSame() {
        assertEquals(List.of("85012345678"), IdentifierNormalizer.SCOPUS.normalize("2-s2.0-85012345678"));
        assertEquals(List.of("85012345678"), IdentifierNormalizer.SCOPUS.normalize("SCOPUS: 2-S2.0-85012345678"));
        assertEquals(List.of("85012345678"), IdentifierNormalizer.SCOPUS.normalize("85012345678"));
    }

    @Test
    public void isiDropsPrefixAndUppercases() {
        assertEquals(List.of("000123456700001"), IdentifierNormalizer.ISI.normalize("WOS:000123456700001"));
        assertEquals(List.of("A1995QF12300004"), IdentifierNormalizer.ISI.normalize("isi: a1995qf12300004"));
    }

    @Test
    public void arxivDropsPrefixUrlAndVersion() {
        List<String> expected = List.of("2509.00838");
        assertEquals(expected, IdentifierNormalizer.ARXIV.normalize("2509.00838v1"));
        assertEquals(expected, IdentifierNormalizer.ARXIV.normalize("arXiv:2509.00838"));
        assertEquals(expected, IdentifierNormalizer.ARXIV.normalize("https://arxiv.org/abs/2509.00838v2"));
        assertEquals(expected, IdentifierNormalizer.ARXIV.normalize("http://www.arxiv.org/pdf/2509.00838v2.pdf"));
        assertEquals(List.of("math.gt/0309136"), IdentifierNormalizer.ARXIV.normalize("math.GT/0309136v3"));
    }

    @Test
    public void blankOrUnusableValuesProduceNothing() {
        for (IdentifierNormalizer normalizer : IdentifierNormalizer.values()) {
            assertTrue(normalizer.name(), normalizer.normalize(null).isEmpty());
            assertTrue(normalizer.name(), normalizer.normalize("   ").isEmpty());
        }
        assertTrue(IdentifierNormalizer.ISBN.normalize("n/a").isEmpty());
        assertTrue(IdentifierNormalizer.PMID.normalize("unknown").isEmpty());
    }

    @Test
    public void resolvesConfiguredNameCaseInsensitively() {
        assertEquals(Optional.of(IdentifierNormalizer.ISBN), IdentifierNormalizer.of("isbn"));
        assertEquals(Optional.of(IdentifierNormalizer.ARXIV), IdentifierNormalizer.of(" ArXiv "));
        assertEquals(Optional.empty(), IdentifierNormalizer.of("doi"));
        assertEquals(Optional.empty(), IdentifierNormalizer.of(null));
    }
}
