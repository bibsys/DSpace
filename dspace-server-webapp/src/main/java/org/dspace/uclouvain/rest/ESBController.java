/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.rest.model.thesis.ThesisSummary;
import org.dspace.uclouvain.services.MasterThesisService;
import org.dspace.web.ContextUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * Main Controller for uclouvain/esb endpoint
 *   * OSIS data retrieval endpoints: OSIS application needs some information from repository
 *   * other projects (todo)
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 *
 */
@RestController
@RequestMapping("/api/uclouvain/esb")
public class ESBController {

    private final Logger log = LogManager.getLogger(ESBController.class);

    private final MasterThesisService masterThesisService =
            UCLouvainServiceFactory.getInstance().getMasterThesisService();
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    // API ENDPOINTS ===================================================================================================
    @GetMapping("/theses")
    public ResponseEntity<List<ThesisSummary>> searchTheses(
            HttpServletRequest request,
            @RequestParam("fgs") String fgs,
            @RequestParam("academic_year") int academicYear
    ) throws Exception {
        Context context = ContextUtil.obtainContext(request);
        List<ThesisSummary> data = fetchThesisSummaries(context, fgs, academicYear);
        return (data.isEmpty())
            ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            : ResponseEntity.ok(data);
    }

    @GetMapping("/theses/{uuid}/summary")
    public ResponseEntity<ThesisSummary> getThesisSummary(
            HttpServletRequest request,
            @PathVariable String uuid
    ) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Item item = itemService.find(context, UUID.fromString(uuid));
        return (item == null)
            ? ResponseEntity.status(HttpStatus.NO_CONTENT).build()
            : ResponseEntity.ok(ThesisSummary.parse(item));
    }

    // PRIVATE FUNCTIONS ===============================================================================================
    private List<ThesisSummary> fetchThesisSummaries(Context context, String fgs, int academicYear)
            throws SearchServiceException {
        List<ThesisSummary> data = new ArrayList<>();
        Iterator<Item> searchResult = masterThesisService.search(
                context,
                Triple.of("authors.identifier.fgs", fgs, Boolean.TRUE),
                Triple.of("dateIssued.year", academicYear, Boolean.TRUE)
        );
        while (searchResult.hasNext()) {
            data.add(ThesisSummary.parse(searchResult.next()));
        }
        return data;
    }

}
