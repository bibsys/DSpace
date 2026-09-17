/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.commons.validator.routines.checkdigit.CheckDigitException;
import org.apache.commons.validator.routines.checkdigit.ISBNCheckDigit;

/**
 * Reduce a bibliographic identifier, as typed by a human or delivered by an import, to the canonical
 * form(s) used to compare two identifiers regardless of their encoding (separators, prefixes, case, ...).
 * <p>
 * The same normalizer must be applied to the indexed values and to the searched value: it is the single
 * definition of "these two identifiers are the same".
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public enum IdentifierNormalizer {

    /**
     * Keeps digits and the {@code X} check digit. A valid ISBN is indexed under both its ISBN-10 and
     * ISBN-13 forms, so that a search only needs to strip the separators of its input.
     */
    ISBN {
        @Override
        public List<String> normalize(String raw) {
            String digits = keep(raw, "0-9X");
            if (digits.isEmpty()) {
                return List.of();
            }
            Set<String> forms = new LinkedHashSet<>();
            forms.add(digits);
            if (ISBN_VALIDATOR.isValidISBN10(digits)) {
                forms.add(ISBN_VALIDATOR.convertToISBN13(digits));
            } else if (ISBN_VALIDATOR.isValidISBN13(digits) && digits.startsWith(ISBN13_BOOKLAND_PREFIX)) {
                // Only the 978 prefix has an ISBN-10 counterpart: drop the prefix, recompute the check digit.
                String core = digits.substring(ISBN13_BOOKLAND_PREFIX.length(), digits.length() - 1);
                try {
                    forms.add(core + ISBNCheckDigit.ISBN10_CHECK_DIGIT.calculate(core));
                } catch (CheckDigitException e) {
                    // Cannot happen on a valid ISBN-13 core, but the identifier is still indexed as-is.
                }
            }
            return List.copyOf(forms);
        }
    },

    /** Keeps digits and the {@code X} check digit: {@code 1234-567x} becomes {@code 1234567X}. */
    ISSN {
        @Override
        public List<String> normalize(String raw) {
            return single(keep(raw, "0-9X"));
        }
    },

    /** GiantChair identifier: digits only. */
    GCOI {
        @Override
        public List<String> normalize(String raw) {
            return single(keep(raw, "0-9"));
        }
    },

    /** PubMed identifier: digits only, so {@code PMID: 12345678} becomes {@code 12345678}. */
    PMID {
        @Override
        public List<String> normalize(String raw) {
            return single(keep(raw, "0-9"));
        }
    },

    /** Scopus identifier: the EID {@code 2-s2.0-85012345678} and the bare {@code 85012345678} are the same. */
    SCOPUS {
        @Override
        public List<String> normalize(String raw) {
            String value = lower(raw).replaceFirst("^scopus:\\s*", "").replaceFirst("^2-s2\\.0-", "");
            return single(keep(value, "0-9"));
        }
    },

    /** Web of Science / ISI accession number: {@code WOS:000123456700001} becomes {@code 000123456700001}. */
    ISI {
        @Override
        public List<String> normalize(String raw) {
            String value = StringUtils.trimToEmpty(raw).toUpperCase(Locale.ROOT)
                .replaceFirst("^(WOS|ISI|UT):\\s*", "");
            return single(keep(value, "0-9A-Z"));
        }
    },

    /**
     * arXiv identifier: {@code arXiv:2509.00838v1}, {@code https://arxiv.org/abs/2509.00838v1} and
     * {@code 2509.00838} are the same paper (a version is not a distinct paper).
     */
    ARXIV {
        @Override
        public List<String> normalize(String raw) {
            String value = lower(raw)
                .replaceFirst("^arxiv:\\s*", "")
                .replaceFirst("^https?://(www\\.)?arxiv\\.org/(abs|pdf)/", "")
                .replaceFirst("\\.pdf$", "")
                .replaceFirst("v\\d+$", "");
            return single(value);
        }
    };

    // CONSTANTS =======================================================================================================
    private static final ISBNValidator ISBN_VALIDATOR = ISBNValidator.getInstance(false);
    private static final String ISBN13_BOOKLAND_PREFIX = "978";

    // METHODS =========================================================================================================
    /**
     * Normalize a raw identifier value.
     *
     * @param raw the value as stored in the metadata; may be null or blank.
     * @return the canonical form(s) of the identifier, in a stable order; empty if nothing usable remains.
     */
    public abstract List<String> normalize(String raw);

    /**
     * Resolve a normalizer from its configured name, case-insensitively.
     *
     * @param name the enum constant name, e.g. {@code isbn}.
     * @return the normalizer, or empty if the name matches none.
     */
    public static Optional<IdentifierNormalizer> of(String name) {
        try {
            return Optional.of(valueOf(StringUtils.trimToEmpty(name).toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String keep(String raw, String characterClass) {
        return StringUtils.trimToEmpty(raw).toUpperCase(Locale.ROOT).replaceAll("[^" + characterClass + "]", "");
    }

    private static String lower(String raw) {
        return StringUtils.trimToEmpty(raw).toLowerCase(Locale.ROOT);
    }

    private static List<String> single(String value) {
        return value.isEmpty()
            ? List.of()
            : List.of(value);
    }
}
