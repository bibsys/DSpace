/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

import java.util.List;
import java.util.UUID;

/**
 * Internal class for working with a list of item citations. Very similar to the Rest model.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public record ItemCitations(UUID id, List<CitationEntry> citations) { }