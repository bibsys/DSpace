package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.AccessConditionDTO;
import org.dspace.app.rest.model.BitstreamAccessConditionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.app.rest.model.BitstreamAccessConditions;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This is the converter from/to the BitstreamAccessCondition in the DSpace API data model and
 * the REST data model
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class BitstreamAccessConditionConverter implements DSpaceConverter<BitstreamAccessConditions, BitstreamAccessConditionRest> {

    @Autowired
    UCLouvainResourcePolicyService uclouvainResourcePolicyService;

    @Override
    public BitstreamAccessConditionRest convert(BitstreamAccessConditions bac, Projection projection) {
        BitstreamAccessConditionRest response = new BitstreamAccessConditionRest();

        UUID bitstreamID = (bac.getPolicies().isEmpty()) ? UUID.randomUUID() : bac.getPolicies().get(0).getdSpaceObject().getID();
        ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(bac.getPolicies());

        response.setProjection(projection);
        response.setId(bitstreamID);
        response.setPolicies(convertPoliciesToAccessConditions(bac.getPolicies()));
        if (masterPolicy != null) {
            response.setMasterPolicy(createAccessConditionFromResourcePolicy(masterPolicy));
        }
        return response;
    }

    @Override
    public Class<BitstreamAccessConditions> getModelClass() {
        return BitstreamAccessConditions.class;
    }

    /** Utils method used to convert a list of {@link org.dspace.authorize.ResourcePolicy} to a list of
     * serializable {@link org.dspace.app.rest.model.AccessConditionDTO}
     * @param policies: The policies to convert
     * @return the corresponding `AccessConditionDTO` list
     */
    private List<AccessConditionDTO> convertPoliciesToAccessConditions(List<ResourcePolicy> policies) {
        return policies
                .stream()
                .map(this::createAccessConditionFromResourcePolicy)
                .collect(Collectors.toList());
    }

    private AccessConditionDTO createAccessConditionFromResourcePolicy(ResourcePolicy rp) {
        AccessConditionDTO accessCondition = new AccessConditionDTO();
        accessCondition.setId(rp.getID());
        accessCondition.setName(rp.getRpName());
        accessCondition.setDescription(rp.getRpDescription());
        accessCondition.setStartDate(rp.getStartDate());
        accessCondition.setEndDate(rp.getEndDate());
        return accessCondition;
    }
}
