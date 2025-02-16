/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.event.behavior.ConsumerActivationRule;
import org.dspace.event.behavior.MetadataActivationRule;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * An instance of this class contains the configuration profile of a specific,
 * named Consumer, <em>in the context of a specific Dispatcher</em>. This
 * includes the name, the class to instantiate and event filters. Note that all
 * characteristics are "global" and the same for all dispatchers.
 */
public class ConsumerProfile {
    /**
     * log4j category
     */
    private static final Logger log = LogManager.getLogger(ConsumerProfile.class);

    /**
     * Name matching the key in DSpace Configuration
     */
    private final String name;

    /**
     * Instance of configured consumer class
     */
    private Consumer consumer;

    /**
     * Filters - each is an array of 2 bitmasks, action mask and subject mask
     */
    private List<int[]> filters;

    /**
     * ActivationRules - list of enable rules and disable rules
     */
    private List<ConsumerActivationRule> enableRules;
    private List<ConsumerActivationRule> disableRules;

    // Prefix of keys in DSpace Configuration.
    private static final String CONSUMER_PREFIX = "event.consumer.";

    /**
     * Constructor.
     */
    private ConsumerProfile(String name) {
        this.name = name;
    }

    /**
     * Factory method, create new profile from configuration.
     *
     * @param name configuration name of the consumer profile
     * @return a new ConsumerProfile; never null.
     * @throws IllegalArgumentException if no class or no filters configured for the specified consumer
     * @throws ClassNotFoundException    passed through.
     * @throws InstantiationException    passed through.
     * @throws IllegalAccessException    passed through.
     * @throws NoSuchMethodException     passed through.
     * @throws InvocationTargetException passed through.
     */
    public static ConsumerProfile makeConsumerProfile(String name)
        throws IllegalArgumentException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException,
            InvocationTargetException {
        ConsumerProfile result = new ConsumerProfile(name);
        result.readConfiguration();
        return result;
    }

    /**
     * Get class and filters from DSpace Configuration.
     *
     * @throws IllegalArgumentException if no class or no filters configured for the specified consumer
     * @throws ClassNotFoundException    passed through.
     * @throws InstantiationException    passed through.
     * @throws IllegalAccessException    passed through.
     * @throws NoSuchMethodException     passed through.
     * @throws InvocationTargetException passed through.
     */
    private void readConfiguration() throws IllegalArgumentException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String className = configService.getProperty(CONSUMER_PREFIX + name + ".class");
        String filterString = configService.getProperty(CONSUMER_PREFIX + name + ".filters");

        if (className == null) {
            throw new IllegalArgumentException("No class configured for consumer named: " + name);
        }
        if (filterString == null) {
            throw new IllegalArgumentException("No filters configured for consumer named: " + name);
        }

        consumer = Class.forName(className.trim())
                .asSubclass(Consumer.class)
                .getDeclaredConstructor().newInstance();

        // Each "filter" is <objectTypes> + <eventTypes> : ...
        filters = new ArrayList<>();
        for (String part : filterString.trim().split(":")) {
            String[] fparts = part.split("\\+");
            if (fparts.length != 2) {
                log.error("Bad Filter clause in consumer stanza in Configuration entry for " + CONSUMER_PREFIX + name
                          + ".consumers: " + part);
                continue;
            }

            int[] filter = {0, 0};
            String[] objectNames = fparts[0].split("\\|");
            for (String objectName : objectNames) {
                int ot = Event.parseObjectType(objectName);
                if (ot == 0) {
                    log.error("Bad ObjectType in Consumer Stanza in Configuration entry for " + CONSUMER_PREFIX + name
                              + ".consumers: " + objectName);
                } else {
                    filter[Event.SUBJECT_MASK] |= ot;
                }
            }

            String[] eventNames = fparts[1].split("\\|");
            for (String eventName : eventNames) {
                int et = Event.parseEventType(eventName);
                if (et == 0) {
                    log.error("Bad EventType in Consumer Stanza in Configuration entry for " + CONSUMER_PREFIX + name
                              + ".consumers: " + eventName);
                } else {
                    filter[Event.EVENT_MASK] |= et;
                }
            }
            filters.add(filter);
        }

        enableRules = Arrays
            .stream(configService.getArrayProperty(CONSUMER_PREFIX + name + ".rule.enable", new String[] {}))
            .map(MetadataActivationRule::new)  // TODO :: should use a factory to build ActivationRule
            .collect(Collectors.toList());
        disableRules = Arrays
            .stream(configService.getArrayProperty(CONSUMER_PREFIX + name + ".rule.disable", new String[] {}))
            .map(MetadataActivationRule::new)  // TODO :: should use a factory to build ActivationRule
            .collect(Collectors.toList());
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public List<int[]> getFilters() {
        return filters;
    }

    public String getName() {
        return name;
    }

    public List<ConsumerActivationRule> getEnableRules() {
        return enableRules;
    }

    public List<ConsumerActivationRule> getDisableRules() {
        return disableRules;
    }
}
