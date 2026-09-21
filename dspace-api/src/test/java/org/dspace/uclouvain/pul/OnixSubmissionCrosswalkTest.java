/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.transform.JDOMResult;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks the ONIX 3.0 -> DIM stylesheet. The stylesheet is read from the live configuration
 * ({@code dspace/config/crosswalks/onix-submission.xsl}); the five records in {@code src/test/data/pul} are real PUL
 * notices enriched with fictional data (contributors, a related product, a sub-collection, ...) so that every mapping
 * rule is exercised by at least one of them.
 */
public class OnixSubmissionCrosswalkTest {

    private static final Path STYLESHEET = Path.of("..", "dspace", "config", "crosswalks", "onix-submission.xsl");
    private static final Path SAMPLES = Path.of("src", "test", "data", "pul");
    private static final Namespace DIM = Namespace.getNamespace("dim", "http://www.dspace.org/xmlns/dspace/dim");
    private static final String PLACEHOLDER = "#PLACEHOLDER_PARENT_METADATA_VALUE#";
    private static final String EDITOR = "scientific_director_editor";

    private static Transformer transformer;

    @BeforeClass
    public static void loadStylesheet() throws Exception {
        assertTrue("stylesheet not found: " + STYLESHEET.toAbsolutePath(), STYLESHEET.toFile().isFile());
        transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(STYLESHEET.toFile()));
    }

    /** E-book: print ISBN through RelatedProduct 13, editors B01, preface A15, level 03 ignored, EpubLicense. */
    @Test
    public void ebookWithEditorsAndSeries() throws Exception {
        Dim dim = transform("29303100021680");
        assertEquals(List.of("29303100021680"), dim.values("dc.identifier.gcoi"));
        assertEquals(List.of("9782875584533", "9782875584526"), dim.values("dc.identifier.isbn"));
        assertEquals(
            List.of("Mons dans la tourmente : Justice et société à l'épreuve des guerres mondiales (1914-1961)"),
            dim.values("dc.title"));
        assertEquals(List.of("Dumont, Amandine", "Thiry, Amandine", "Rousseaux, Xavier", "Campion, Jonas",
            "Préfacier, Paul"), dim.values("dc.contributor.author"));
        assertEquals(List.of(EDITOR, EDITOR, EDITOR, EDITOR, "preface_writer"), dim.values("authors.role"));
        // the rest of the CRIS author group, one placeholder per contributor
        for (String field : List.of("authors.email", "authors.identifier.orcid", "authors.identifier.fgs",
                "authors.institution.code")) {
            assertEquals(field, Collections.nCopies(5, PLACEHOLDER), dim.values(field));
        }
        assertEquals(List.of("fre"), dim.values("dc.language.iso"));
        assertEquals(List.of("2016-04-11"), dim.values("dc.date.issued"));
        assertEquals(List.of("232"), dim.values("publication.numberOfPages"));
        assertEquals(List.of("Histoire, justice, sociétés"), dim.values("publication.collection.name"));
        assertEquals(List.of("19"), dim.values("publication.collection.number"));
        assertEquals(List.of("Presses universitaires de Louvain"), dim.values("publication.editor.name"));
        assertEquals(List.of("Louvain-la-Neuve"), dim.values("publication.editor.location"));
        // the record lists "Histoire du droit et des institutions" twice
        assertEquals(List.of("Histoire Contemporaine", "Histoire du droit et des institutions"),
            dim.values("dc.subject"));
        assertEquals(List.of("fr"), dim.langs("dc.description.abstract"));
        assertTrue(dim.values("dc.description.abstract").get(0).startsWith("<p>L'objectif de ce livre"));
        assertEquals(List.of("text::book"), dim.values("dc.type.maintype"));
        assertEquals(List.of("book"), dim.values("dc.type.subtype"));
        assertEquals(List.of("PUL"), dim.values("dcterms.source"));
        // EpubLicense: the expression link wins over the name
        assertEquals(List.of("https://creativecommons.org/licenses/by-nc-nd/3.0/"), dim.values("dcterms.license"));
    }

    @Test
    public void printRecordsCarryNoLicense() throws Exception {
        assertEquals(List.of(), transform("29303100293170").values("dcterms.license"));
    }

    /** TitlePrefix "L'" glued to the title, managing editor B16, page count only as ExtentType 07. */
    @Test
    public void elidedTitlePrefixAndPageCountFallback() throws Exception {
        Dim dim = transform("29303100123320");
        assertTrue(dim.values("dc.title").get(0)
            .startsWith("L'Apprentissage en situation de travail : Itinéraires"));
        assertEquals(List.of("9782875584113", "9782875584106"), dim.values("dc.identifier.isbn")); // RelatedProduct 06
        assertEquals(List.of(EDITOR), dim.values("authors.role"));
        assertEquals(List.of("10"), dim.values("publication.collection.number"));
        assertEquals(List.of("160"), dim.values("publication.numberOfPages"));
    }

    /** Edited volume: contributors sorted by SequenceNumber, A32 contributions, same abstract tagged fre and eng. */
    @Test
    public void editedVolumeSortsContributorsAndDeduplicatesAbstracts() throws Exception {
        Dim dim = transform("29303100293170");
        List<String> authors = dim.values("dc.contributor.author");
        List<String> roles = dim.values("authors.role");
        assertEquals(26, authors.size());
        assertEquals(26, roles.size());
        assertEquals("Cunningham, Tim", authors.get(0));
        // SequenceNumber 26 comes first in the document and must end up last, with its own role
        assertEquals("Doe, Jane", authors.get(25));
        assertEquals("author", roles.get(25));
        assertEquals(2, roles.stream().filter(EDITOR::equals).count()); // B01
        assertEquals(23, roles.stream().filter("collaborator"::equals).count()); // A32
        assertEquals(List.of("fr"), dim.langs("dc.description.abstract"));
    }

    /** Spaced TitlePrefix "La", UnnamedPersons skipped, no series, RelatedProduct 27, abstract without language. */
    @Test
    public void spacedTitlePrefixUnnamedContributorAndNoSeries() throws Exception {
        Dim dim = transform("29303100808420");
        assertEquals(List.of("La Pédagogie en questions : Guide de l'enseignant"), dim.values("dc.title"));
        assertEquals(List.of(), dim.values("dc.contributor.author"));
        assertEquals(List.of(), dim.values("authors.role"));
        assertEquals(List.of(), dim.values("publication.collection.name"));
        assertEquals(List.of("9782874630972", "9782874630989"), dim.values("dc.identifier.isbn"));
        // two identical French abstracts emitted once, plus one without @language that takes the book language
        assertEquals(List.of("fr", "fr"), dim.langs("dc.description.abstract"));
        assertTrue(dim.values("dc.description.abstract").get(1).startsWith("<p>Second résumé fictif"));
    }

    /** Rare roles, corporate contributor, unknown role code, original language and trade-only texts ignored. */
    @Test
    public void rareRolesCorporateNameAndUnknownRole() throws Exception {
        Dim dim = transform("29303100971260");
        assertEquals(List.of("Theoretical Aspects of Rationality and Knowledge : "
            + "Proceedings of the Eleventh Conference (TARK 2007)"), dim.values("dc.title"));
        assertEquals(List.of("Samet, Dov", "Conducteur, Diego", "Groupe Bidon", "Inconnu, Zoé", "Texte, Anna"),
            dim.values("dc.contributor.author"));
        // B01, D03, C01 (CorporateName), Z99 unknown, A14
        assertEquals(List.of(EDITOR, "collaborator", EDITOR, PLACEHOLDER, "author"), dim.values("authors.role"));
        assertEquals(List.of("eng"), dim.values("dc.language.iso")); // LanguageRole 02 ignored
        // two different English texts kept (one is a truncated version); ContentAudience 02 dropped
        assertEquals(List.of("en", "en"), dim.langs("dc.description.abstract"));
    }

    @Test
    public void transformsEveryRecordWithoutGaps() throws Exception {
        File[] files = SAMPLES.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
        assertEquals(5, files.length);
        for (File file : files) {
            Dim dim = transform(file);
            String name = file.getName();
            for (String required : List.of("dc.identifier.gcoi", "dc.identifier.isbn", "dc.title", "dc.date.issued",
                    "dc.language.iso", "publication.editor.name", "dc.type.maintype", "dcterms.source")) {
                assertFalse(name + " lacks " + required, dim.values(required).isEmpty());
            }
            assertEquals(name + ": file name is the GCOI", name.replace(".xml", ""),
                dim.values("dc.identifier.gcoi").get(0));
            int authors = dim.values("dc.contributor.author").size();
            for (String field : List.of("authors.role", "authors.email", "authors.identifier.orcid",
                    "authors.identifier.fgs", "authors.institution.code")) {
                assertEquals(name + ": one " + field + " per author", authors, dim.values(field).size());
            }
            assertEquals(name + ": placeholder only for the unknown role code",
                name.startsWith("29303100971260") ? 1 : 0,
                Collections.frequency(dim.values("authors.role"), PLACEHOLDER));
            assertFalse(name + ": empty value", dim.fields.values().stream().flatMap(List::stream)
                .anyMatch(f -> f.value.isBlank()));
        }
    }

    // HELPERS =========================================================================================================

    private Dim transform(String gcoi) throws Exception {
        return transform(SAMPLES.resolve(gcoi + ".xml").toFile());
    }

    private Dim transform(File onix) throws Exception {
        JDOMResult result = new JDOMResult();
        transformer.transform(new StreamSource(onix), result);
        return new Dim(result.getDocument());
    }

    /** The DIM fields of one transformed record, keyed by {@code schema.element[.qualifier]}, in document order. */
    private static class Dim {
        private record Field(String lang, String value) { }

        private final Map<String, List<Field>> fields = new LinkedHashMap<>();

        Dim(Document document) {
            for (Element field : document.getRootElement().getChildren("field", DIM)) {
                String name = Stream.of(field.getAttributeValue("mdschema"), field.getAttributeValue("element"),
                        field.getAttributeValue("qualifier"))
                    .filter(part -> part != null && !part.isEmpty())
                    .reduce((a, b) -> a + "." + b).orElseThrow();
                fields.computeIfAbsent(name, k -> new ArrayList<>())
                    .add(new Field(field.getAttributeValue("lang"), field.getText()));
            }
        }

        List<String> values(String name) {
            return fields.getOrDefault(name, List.of()).stream().map(Field::value).toList();
        }

        List<String> langs(String name) {
            return fields.getOrDefault(name, List.of()).stream().map(Field::lang).toList();
        }
    }
}
