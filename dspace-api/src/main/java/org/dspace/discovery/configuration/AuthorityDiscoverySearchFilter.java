/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

/**
 * Specific discovery facet to index authority values in SOLR.
 * This is particularly useful to create relation basis in solr between two entity types.
 * EX: Publication <-> Author: We can create an AuthorityDiscoverySearchFilter that indexes the authority value of
 * each author as a field in the publication document.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class AuthorityDiscoverySearchFilter extends DiscoverySearchFilter {
}
