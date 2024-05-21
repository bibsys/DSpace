/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.BitstreamAccessConditionRest;
import org.dspace.app.rest.model.BitstreamAccessConditions;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/** Link repository for "access" subresource of an individual bitstream.
 *
 * We created this rest endpoint to list all useful resource policies on a `Bitstream` object. The existing
 * `api/authz/resourcepolicies` endpoint filter resource policies depending on current user (ex: an anonymous user
 * doesn't show any 'administrator' policy); but for the bitstream access condition display on UI we need to get
 * these policies.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.NAME + "." + BitstreamRest.ACCESS)
public class BitstreamAccessConditionsLinkRepository extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    BitstreamService bitstreamService;
    @Autowired
    UCLouvainResourcePolicyService uclouvainResourcePolicyService;

    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'METADATA_READ')")
    public BitstreamAccessConditionRest getAccessCondition(
            @Nullable HttpServletRequest request,
            UUID bitstreamId,
            @Nullable Pageable optionalPageable,
            Projection projection
    ) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            }
            BitstreamAccessConditions bac = new BitstreamAccessConditions();
            bac.setPolicies(uclouvainResourcePolicyService.find(context, bitstream));
            return converter.toRest(bac, projection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
