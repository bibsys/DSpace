/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.services;

import java.util.List;

import org.dspace.uclouvain.profileIngester.exceptions.IDMCheckException;

public interface IDMPersonValidityService {
    public boolean isPersonIDMValid(String fgs) throws IDMCheckException;
    public boolean isPersonIDMValid(List<Integer> idmRows) throws IDMCheckException;
}
