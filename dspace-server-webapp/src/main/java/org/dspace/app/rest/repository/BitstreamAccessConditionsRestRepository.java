package org.dspace.app.rest.repository;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.BitstreamAccessConditionRest;
import org.dspace.app.rest.model.BitstreamAccessConditions;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.UUID;


/**
 * This is the repository responsible to manage BitstreamAccessConditions Rest object
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component(BitstreamAccessConditionRest.CATEGORY + "." + BitstreamAccessConditionRest.NAME)
public class BitstreamAccessConditionsRestRepository extends DSpaceRestRepository<BitstreamAccessConditionRest, UUID> {

    @Autowired
    BitstreamService bitstreamService;
    @Autowired
    UCLouvainResourcePolicyService uclouvainResourcePolicyService;

    @Override
    @PreAuthorize("permitAll()")
    public BitstreamAccessConditionRest findOne(Context context, UUID uuid) {
        try {
            Bitstream bitstream = bitstreamService.find(context, uuid);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + uuid);
            }

            BitstreamAccessConditions bac = new BitstreamAccessConditions();
            bac.setPolicies(uclouvainResourcePolicyService.find(context, bitstream));

            return converter.toRest(bac, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<BitstreamAccessConditionRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Class<BitstreamAccessConditionRest> getDomainClass() {
        return BitstreamAccessConditionRest.class;
    }
}


