/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to retrieve the submitter of a given item.
 * This is typically accepted for users being author of an item or having a READ right.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@RestController
@RequestMapping("/api/uclouvain/submitter")
public class ItemSubmitterRestController {

    private ItemService itemService;

    ItemSubmitterRestController() {
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @PreAuthorize("@itemSecurity.isAuthor(#uuid) || hasPermission(#uuid, 'ITEM', 'READ')")
    @GetMapping(value = "/{uuid}")
    public ItemSubmitter getItemSubmitter(
        Context context, HttpServletResponse response, @PathVariable UUID uuid
    ) throws SQLException {
        Item item = itemService.find(context, uuid);
        if (item == null) {
            throw new ResourceNotFoundException("No such item: " + uuid);
        }
        return new ItemSubmitter(item.getSubmitter());
    }

    /**
     * We will return this class to the user.
     * It takes in an EPerson object and we can choose what to expose (here only full-name).
     */
    public class ItemSubmitter {
        private String submitterName;
        private String submitterEmail;

        ItemSubmitter(EPerson submitter) {
            this.submitterName = submitter.getFullName();
            this.submitterEmail = submitter.getEmail();
        }

        public String getSubmitterName() {
            return submitterName;
        }

        public String getSubmitterEmail() {
            return submitterEmail;
        }
    }
}
