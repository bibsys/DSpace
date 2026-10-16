/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.script;

import java.lang.management.ManagementFactory;

import org.dspace.uclouvain.administer.AbstractCLICommand;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command-line tool processing one batch of the enhancement queue ('uclouvain_item_authority_metadata_enhancement')
 * with the {@link UCLouvainItemEnhancerPoller}, then exiting.
 *
 * It is meant to be launched periodically by an external scheduler (cron, Kubernetes CronJob...), which must forbid
 * overlapping executions: only one instance may process the queue at a time.
 * The command exits with a non-zero status if the run fails.
 *
 * Usage:
 *   dspace dsrun org.dspace.uclouvain.itemEnhancer.script.ItemEnhancerPollerCLI
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ItemEnhancerPollerCLI extends AbstractCLICommand {

    private static final String USAGE_DESCRIPTION = "A command-line tool to trigger the enhancement of items.";
    private static final Logger logger = LoggerFactory.getLogger(ItemEnhancerPollerCLI.class);

    // CLASS ATTRIBUTES
    private final UCLouvainItemEnhancerPoller itemEnhancerPoller;

    // CONSTRUCTOR & MAIN
    protected ItemEnhancerPollerCLI() {
        itemEnhancerPoller = UCLouvainServiceFactory.getInstance().getItemEnhancerUpdatePoller();
    }

    /**
     * For invoking via the command line.
     *
     * @param argv the command line arguments given
     */
    public static void main(String[] argv) {
        ItemEnhancerPollerCLI cli = new ItemEnhancerPollerCLI();
        cli.validateCLIArgument(argv);
        cli.processBatch();
    }

    @Override
    protected void buildOptions() {
        infoOptions.addOption(OPT_HELP);
    }

    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    /**
     * Process one batch of the enhancement queue.
     * A failure is logged and rethrown, so that the command exits with a non-zero status (visible to the scheduler).
     */
    private void processBatch() {
        // "pid@hostname" identifies the process: useful to spot overlapping executions.
        logger.info("Enhancement run started on {}", ManagementFactory.getRuntimeMXBean().getName());
        long start = System.currentTimeMillis();
        try {
            itemEnhancerPoller.run();
        } catch (RuntimeException e) {
            logger.error("Enhancement run failed after {} ms", System.currentTimeMillis() - start, e);
            throw e;
        }
        logger.info("Enhancement run done in {} ms", System.currentTimeMillis() - start);
    }
}
