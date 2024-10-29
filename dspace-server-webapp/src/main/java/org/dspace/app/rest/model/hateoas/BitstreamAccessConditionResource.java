/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BitstreamAccessConditionRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * BitstreamAccessConditionRest HAL Resource.
 * The HAL Resource wraps the REST Resource adding support for the links and embedded resources
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@RelNameDSpaceResource(BitstreamAccessConditionRest.NAME)
public class BitstreamAccessConditionResource extends DSpaceResource<BitstreamAccessConditionRest> {
    public BitstreamAccessConditionResource(BitstreamAccessConditionRest bac, Utils utils) {
        super(bac, utils);
    }
}
