/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.ClaimedTaskRest;
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.ClaimedTaskService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository to return the claimed tasks related to a `WorkspaceItem`
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */

@Component(WorkflowItemRest.CATEGORY + "." + WorkflowItemRest.PLURAL_NAME + "." + WorkflowItemRest.CLAIMED)
public class WorkflowItemClaimedTaskLinkRepository
        extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    XmlWorkflowItemService workflowItemService;
    @Autowired
    ClaimedTaskService claimedTaskService;

    @PreAuthorize("hasPermission(#id, 'WORKFLOWITEM', 'READ')")
    public Page<ClaimedTaskRest> getWorkflowItemClaimedTasks(
            @Nullable HttpServletRequest request,
            Integer id,
            @Nullable Pageable optionalPageable,
            Projection projection
    ) {
        try {
            Context context = obtainContext();
            XmlWorkflowItem wfi = workflowItemService.find(context, id);
            if (wfi == null) {
                throw new ResourceNotFoundException("No such workflow item: " + id);
            }
            List<ClaimedTaskRest> list = getClaimedTasks(context, wfi, projection);
            Pageable pageable = utils.getPageable(optionalPageable);
            return utils.getPage(list, pageable);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ClaimedTaskRest> getClaimedTasks(Context context, XmlWorkflowItem wfi, Projection projection)
            throws SQLException {
        return claimedTaskService.findByWorkflowItem(context, wfi)
                .stream()
                .map(task -> (ClaimedTaskRest) converter.toRest(task, projection))
                .collect(Collectors.toList());
    }
}
