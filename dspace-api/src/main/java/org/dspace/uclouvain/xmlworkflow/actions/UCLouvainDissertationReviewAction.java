/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.util.ArrayList;
import java.util.List;

import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;

/**
 * Custom review action for dissertation workflow.
 * Here we only allow the manager to edit or accept.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainDissertationReviewAction extends ReviewAction {

    /**
     * Override the getOptions() method to only allow for validation and edit actions.
     */
    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(RETURN_TO_POOL);
        // Edit item button
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }
}
