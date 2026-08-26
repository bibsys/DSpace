/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

/**
 * Thrown by a {@link WorkflowStartGuard} that refuses to let a workspace item enter the workflow.
 *
 * <p>
 * The message is a technical one, meant for the logs and for whoever reads the API answer; it is
 * deliberately not localized. Callers facing a user turn it into a 422 answer, callers that don't
 * - batch imports, harvesters - simply let it propagate and abort.
 * </p>
 *
 * <p>
 * It is unchecked on purpose: the guards are consulted from
 * {@code WorkflowService#start(Context, WorkspaceItem)}, whose signature must stay untouched so
 * that every existing caller keeps compiling.
 * </p>
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class WorkflowStartVetoException extends RuntimeException {

    /**
     * Build a veto.
     *
     * @param message the reason why the deposit is refused
     */
    public WorkflowStartVetoException(String message) {
        super(message);
    }
}
