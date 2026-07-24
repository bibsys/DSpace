/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.explainer;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dspace.uclouvain.content.snapshot.diff.formats.DiffFormatter;
import org.dspace.uclouvain.content.snapshot.diff.formats.DiffFormatterTypeFor;
import org.dspace.uclouvain.content.snapshot.diff.formats.OutputFormat;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.springframework.stereotype.Component;

/**
 * Factory allowing to explain changes between two {@link SnapshotElement} for a specific {@link OutputFormat}
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class DiffExplainerFactory {

    // CLASS ATTRIBUTES ================================================================================================
    private final List<DiffFormatter<?>> formatters;
    private final Map<Class<? extends SnapshotElement>, Class<? extends DiffExplainer<?>>> explainers = new HashMap<>();

    // CONSTRUCTOR =====================================================================================================
    public DiffExplainerFactory(List<DiffFormatter<?>> formatters, List<Class<? extends DiffExplainer<?>>> explainers) {
        this.formatters = formatters;
        for (Class<? extends DiffExplainer<?>> clazz : explainers) {
            if (clazz.isAnnotationPresent(DiffExplainerFor.class)) {
                Class<? extends SnapshotElement> targetElement = clazz.getAnnotation(DiffExplainerFor.class).value();
                this.explainers.put(targetElement, clazz);
            }
        }
    }

    // CLASS METHODS ===================================================================================================

    /**
     * This is a factory method; it allows to explain changes between two `SnapshotElement` for a specific format
     * @param original the original snapshot to analyze
     * @param revised the revised snapshot version to analyze
     * @param format the format to use to render changes
     * @param locale the language ti used to render changes
     * @return the string representation of changes
     * @throws IllegalArgumentException if no explainer could be found for these SnapshotElement
     * @throws UnsupportedOperationException if format isn't supported for these SnapshotElement
     */
    public String explain(SnapshotElement original, SnapshotElement revised, OutputFormat format, Locale locale) {
        // 1. Create an instance of the correct explainer (based on SnapshotElement)
        DiffExplainer<?> explainer = getExplainerInstance(original, revised);
        // 2. Find the correct formatter based on format and explainer
        return formatters.stream()
            .filter(f -> f.getClass().isAnnotationPresent(DiffFormatterTypeFor.class))
            .filter(f -> {
                DiffFormatterTypeFor anno = f.getClass().getAnnotation(DiffFormatterTypeFor.class);
                return anno.format() == format && anno.clazz().isInstance(explainer);
            })
            .map(f -> ((DiffFormatter<DiffExplainer<?>>) f).format(explainer, locale))
            .findFirst()
            .orElseThrow(() -> new UnsupportedOperationException(
                    "No formatter found for %s with format %s".formatted(explainer.getClass().getSimpleName(), format)
            ));
    }

    /**
     * Find the correct explainer class and create a new instance of the class.
     * @param original the original SnapshotElement to explain
     * @param revised the updated SnapshotElement to explain
     * @throws IllegalArgumentException if arguments are invalid or no explainer can be found
     * @throws IllegalStateException if problems occurred during explainer instance creation
     */
    private DiffExplainer<?> getExplainerInstance(SnapshotElement original, SnapshotElement revised)
        throws IllegalArgumentException {
        // Find the explainer class exploring the explainers registry
        SnapshotElement reference = (original != null) ? original : revised;
        if (reference == null) {
            throw new IllegalArgumentException("Both SnapshotElements cannot be null");
        }
        Class<? extends SnapshotElement> clazz = reference.getClass();
        Class<? extends DiffExplainer<?>> explainerClass = explainers.get(clazz);
        if (explainerClass == null) {
            throw new IllegalArgumentException("No Explainer registered for SnapshotElement type: " + clazz.getName());
        }

        // Call the constructor using the dedicated SnapshotElement subclasses.
        // If the class doesn't define a dedicated constructor, call the parent class constructor with basic
        // SnapshotElement arguments.
        try {
            Constructor<? extends DiffExplainer<?>> constructor = explainerClass.getConstructor(clazz, clazz);
            return constructor.newInstance(original, revised);
        } catch (NoSuchMethodException e) {
            try {
                Constructor<? extends DiffExplainer<?>> constructor =
                    explainerClass.getConstructor(SnapshotElement.class, SnapshotElement.class);
                return constructor.newInstance(original, revised);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to find a valid constructor for " + clazz.getName(), ex);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate explainer: " + clazz.getName(), e);
        }
    }

}
