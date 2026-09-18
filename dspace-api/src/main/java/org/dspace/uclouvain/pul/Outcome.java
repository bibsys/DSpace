/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import java.io.File;

import org.dspace.content.Item;

/**
 * What the import decided, then did, for one ONIX file.
 *
 * @param decision what the file leads to
 * @param file     the ONIX file
 * @param record   the file once read; null for a PDF or when the file could not be read
 * @param item     the publication the decision applies to: the existing one for {@link Decision#UPDATE}, the new one
 *                 once a {@link Decision#CREATE} has been applied; null otherwise
 * @param message  how the decision was reached, what was done, or the error
 */
public record Outcome(Decision decision, File file, OnixRecord record, Item item, String message) {

    public enum Decision {
        CREATE,         // No publication carries the identifiers: a new one is created.
        UPDATE,         // One publication carries the identifiers: it is completed.
        AMBIGUOUS,      // The GCOI and an ISBN point to different publications: nothing is done, a human must look.
        DELETED_NOTICE, // ONIX notification type 05 (removed from the catalogue): reported only, nothing is done.
        PENDING,        // A PDF whose publication is not imported yet: left in place for a later run.
        ERROR           // The file could not be read, transformed, matched or written.
    }

    public String title() {
        return (record == null)
            ? null
            : record.title();
    }

    /** The same outcome, once applied: the item written and what was done. */
    public Outcome applied(Item writtenItem, String done) {
        return new Outcome(decision, file, record, writtenItem, message + "; " + done);
    }

    /** The same file, failed while being applied. */
    public Outcome failed(String error) {
        return new Outcome(Decision.ERROR, file, record, null, error);
    }

    /** One report line, e.g. {@code UPDATE     29303100021680.xml  "Mons dans la tourmente" -- matched by GCOI ...}. */
    public String describe() {
        // Build base description with decision and file name
        String base = "%-14s %s".formatted(decision, file.getName());

        // Format optional components cleanly
        String titlePart = (title() != null) ? "  \"%s\"".formatted(title()) : "";
        String itemPart = (item != null) ? " -> item %s [%s]".formatted(item.getID(), state(item)) : "";
        String messagePart = (message != null) ? " -- %s".formatted(message) : "";

        return base + titlePart + itemPart + messagePart;
    }

    /** The visibility of an item, for the report: the import never filters on it. */
    private static String state(Item item) {
        if (item.isWithdrawn()) {
            return "withdrawn";
        }
        String state = item.isArchived() ? "archived" : "not archived";
        return item.isDiscoverable() ? state : state + ", not discoverable";
    }
}
