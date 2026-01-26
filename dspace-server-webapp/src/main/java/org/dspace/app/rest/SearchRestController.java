/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.exception.InvalidSearchRequestException;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.service.GroupService;
import org.dspace.uclouvain.search.model.SolrSearchResponse;
import org.dspace.uclouvain.search.services.UCLouvainSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uclouvain/search")
public class SearchRestController {
    @Autowired
    UCLouvainSearchService uclouvainSearchService;
    @Autowired
    GroupService groupService;
    @Autowired
    AuthorizeService authorizeService;

    // MAIN ENDPOINTS --------------------------------------------------------------------------------------------------

    @GetMapping
    // Allow user to access to the search endpoint if he is a member of the configured group.
    @PreAuthorize("@groupSecurity.isMemberOf('Publication API Search')")
    public ResponseEntity<?> search(
        HttpServletResponse response, HttpServletRequest request,
        @RequestParam(value = "q", defaultValue = "*:*") String query,
        @RequestParam(value = "fq", required = false) List<String> filterQueries,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Context context = ContextUtil.obtainContext(request);
        try {
            SolrSearchResponse res = uclouvainSearchService
                .searchPublications(context, query, filterQueries, page, size);
            return ResponseEntity.ok(res);
        } catch (SearchServiceException e) {
            throw new InvalidSearchRequestException(e.getMessage(), e);
        }
    }
}
