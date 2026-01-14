/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.utils;

import java.util.Objects;

import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;

/**
 * Utils for standard FNRS export.
 * Use 'isFNRSValid()' to validate a specific publication for a specific author following FNRS rules.
 * 
 * Authored-by: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class FNRSExportUtils {

    protected FNRSExportUtils() {
        throw new UnsupportedOperationException();  // required by "(design) HideUtilityClassConstructor" code checker
    }

    /**
     * Check if a given publication is considered "FNRS valid" for the given author.
     * 
     * @param authorId The id of the author to check FNRS validity of.
     * @param publication The publication to check FNRS validity of.
     * @return True if the publication and the author are considered "FNRS valid".
     */
    public static boolean isFNRSValid(String authorId, Publication publication) {
        String publicationType = publication.getMainType();
        switch (publicationType) {
            case "text::book":
                return validateBook(authorId, publication);
            case "text::book-chapter":
                return validateBookChapter(authorId, publication);
            case "text::conference-speech":
                return validateConferenceSpeech(publication);
            default:
                return true;
        }
    }

    /**
     * Validate a book based on fnrs criteria.
     * The author has to have one of the following roles:
     * - Author
     * - Collaborator
     * - Director
     */
    private static boolean validateBook(String authorId, Publication publication) {
        return hasRole(authorId, publication, PublicationAuthor.ROLE_AUTHOR)
            || hasRole(authorId, publication, PublicationAuthor.ROLE_COLLABORATOR)
            || hasRole(authorId, publication, PublicationAuthor.ROLE_DIRECTOR);
    }

    /**
     * Validate a book-chapter based on fnrs criteria.
     * The author has to have one of the following roles:
     * - Author
     * - Collaborator
     */
    private static boolean validateBookChapter(String authorId, Publication publication) {
        return hasRole(authorId, publication, PublicationAuthor.ROLE_AUTHOR)
            || hasRole(authorId, publication, PublicationAuthor.ROLE_COLLABORATOR)
            || hasRole(authorId, publication, PublicationAuthor.ROLE_PREFACE_WRITER)
            || hasRole(authorId, publication, PublicationAuthor.ROLE_DIRECTOR);
    }

    /**
     * Validate a conference speech based on fnrs criteria.
     * The conference has to be one of the following:
     * - an abstract
     * - a keynote
     * - a conference with a selection speech
     * - a conference poster
     * If one of those criteria is met, then we consider the publication as valid.
     */
    private static boolean validateConferenceSpeech(Publication publication) {
        boolean isAbstract = publication.getMetadataValues("publication.isAbstract")
            .stream()
            .anyMatch(mv -> Objects.equals(mv, "true"));
        boolean validSubtype = publication.getMetadataValues(Publication.SUB_TYPE_FIELD).stream().anyMatch(mv -> {
            return Objects.equals(mv, "keynote")
                || Objects.equals(mv, "with-selection-speech")
                || Objects.equals(mv, "conference-poster");
        });
        return isAbstract || validSubtype;
    }

    /**
     * Check if an author has the given role in the given publication.
     */
    private static boolean hasRole(String authorId, Publication publication, String role) {
        return publication.getAuthors().stream()
            .filter(author -> author.getAuthority() != null)
            .anyMatch((PublicationAuthor author) -> {
                return Objects.equals(author.getAuthority().getID().toString(), authorId)
                    && Objects.equals(author.getRole(), role);
            });
    }
}
