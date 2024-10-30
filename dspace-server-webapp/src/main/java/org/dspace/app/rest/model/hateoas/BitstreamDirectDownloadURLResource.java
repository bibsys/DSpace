/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.BitstreamDirectDownloadURLRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(BitstreamDirectDownloadURLRest.NAME)
public class BitstreamDirectDownloadURLResource extends DSpaceResource<BitstreamDirectDownloadURLRest> {
    public BitstreamDirectDownloadURLResource(BitstreamDirectDownloadURLRest bdu, Utils utils) {
        super(bdu, utils);
    }
}
