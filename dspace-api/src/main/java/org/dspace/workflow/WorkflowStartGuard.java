/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

/**
 * Consulted before a {@link WorkspaceItem} is turned into a workflow item.
 *
 * <p>
 * Every bean implementing this interface is collected by the workflow service and asked, in turn,
 * whether a given workspace item is allowed to enter the workflow. Declaring the bean is the only
 * wiring needed: there is no registry to keep up to date. An implementation refuses the deposit by
 * throwing {@link WorkflowStartVetoException}; returning normally means "no objection".
 * </p>
 *
 * <p>
 * Guards are consulted before anything has been created or deleted, so a veto leaves the workspace
 * item exactly as it was and nothing has to be rolled back. They are on the critical path of every
 * deposit, whatever the channel (submission form, batch import, SWORD, harvester), so they must
 * stay cheap. A silent deposit, made through
 * {@code WorkflowService#startWithoutNotify(Context, WorkspaceItem)}, skips the guards protecting
 * a notification (see {@link #isGuardForNotification()}); the other guards always run. Annotate
 * implementations with {@code @Order} to make the evaluation order explicit when several guards
 * coexist.
 * </p>
 *
 * @author Renaud Michotte (renaud.michotte at uclouvain.be)
 */
public interface WorkflowStartGuard {

    /**
     * Does this guard protect a notification? A guard that answers true is skipped for the silent
     * deposits, where no notification will be sent anyway.
     *
     * @return True if this guard only matters when notifications are sent, False otherwise
     */
    boolean isGuardForNotification();

    /**
     * Check whether the given workspace item is allowed to enter the workflow.
     *
     * @param context the current DSpace context
     * @param wsi     the workspace item about to be deposited
     * @throws WorkflowStartVetoException if this guard refuses the deposit
     */
    void check(Context context, WorkspaceItem wsi) throws WorkflowStartVetoException;
}
