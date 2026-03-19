/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

public class DatabaseBenchmarkCLI extends AbstractCLICommand {
    private static final Option OPT_UUID_FILE = Option.builder("f")
        .longOpt("file")
        .hasArg(true)
        .desc("The email address of eperson doing the import.")
        .required(true)
        .build();
    private static final Option OPT_ITERATION_NUMBER = Option.builder("i")
        .longOpt("iterate")
        .hasArg(true)
        .desc("The number of iteration to do on the uuids of the file.")
        .required(true)
        .build();
    public static final String USAGE_DESCRIPTION = "A command-line tool to benchmark database performances.";

    private Context context;
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    protected DatabaseBenchmarkCLI() {
        context = new Context();
    }

    /**
     * Prepare the script and run it for a CLI execution.
     * @param argv The arguments given to the command line.
     * @throws Exception if any error occurred while running the benchmark script.
     */
    public static void main(String[] argv) throws Exception {
        DatabaseBenchmarkCLI script = new DatabaseBenchmarkCLI();
        CommandLine cli = script.validateCLIArgument(argv);

        script.run(cli.getOptionValue("f"), Integer.parseInt(cli.getOptionValue("i")));
    }

    private void run(String filePath, int iterate) {
        System.out.println("------------------------------------------------");
        System.out.println("---------------DATABASE BENCHMARK---------------");
        System.out.println("------------------------------------------------");

        List<UUID> uuids = loadUUIDs(filePath);

        if (uuids.isEmpty()) {
            System.out.println("No UUIDs found in file.");
            return;
        }

        long totalCalls = 0;
        long totalTimeNs = 0;
        long minTimeNs = Long.MAX_VALUE;
        long maxTimeNs = 0;

        try {
            context.turnOffAuthorisationSystem();

            for (int i = 0; i < iterate; i++) {
                context.clear();
                System.out.println("Iteration " + (i + 1) + "/" + iterate);

                for (UUID uuid : uuids) {
                    long start = System.nanoTime();

                    itemService.find(context, uuid);

                    long duration = System.nanoTime() - start;

                    totalCalls++;
                    totalTimeNs += duration;
                    minTimeNs = Math.min(minTimeNs, duration);
                    maxTimeNs = Math.max(maxTimeNs, duration);
                }
            }

            context.complete();

        } catch (Exception e) {
            context.abort();
            e.printStackTrace();
            return;
        }

        // ---- Metrics ----
        double totalTimeMs = totalTimeNs / 1_000_000.0;
        double avgTimeMs = totalTimeMs / totalCalls;
        double minTimeMs = minTimeNs / 1_000_000.0;
        double maxTimeMs = maxTimeNs / 1_000_000.0;
        double throughput = totalCalls / (totalTimeNs / 1_000_000_000.0);

        System.out.println("\n--------------- RESULTS ---------------");
        System.out.println("Total iterations : " + iterate);
        System.out.println("Total UUIDs in file: " + uuids.size());
        System.out.println("Total calls: " + totalCalls);
        System.out.println("Total time: " + totalTimeMs + " ms");
        System.out.println("Average time: " + avgTimeMs + " ms");
        System.out.println("Min time: " + minTimeMs + " ms");
        System.out.println("Max time: " + maxTimeMs + " ms");
        System.out.println("Throughput: " + throughput + " calls/sec");
    }

    private List<UUID> loadUUIDs(String filePath) {
        List<UUID> uuids = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (!line.isEmpty()) {
                    try {
                        uuids.add(UUID.fromString(line));
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid UUID skipped: " + line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return uuids;
    }

    @Override
    protected void buildOptions() {
        serviceOptions.addOption(OPT_UUID_FILE);
        serviceOptions.addOption(OPT_ITERATION_NUMBER);
        infoOptions.addOption(OPT_HELP);
    }

    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }
}
