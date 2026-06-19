/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.element;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.SnapshotElementType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.w3c.dom.Element;

/**
 * Factory class used to build {@link SnapshotElement} from external source.
 * The SnapshotElement will be used to build {@link ItemSnapshot} and deal with item comparaison.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SnapshotElementFactory {

    private final Map<String, Constructor<? extends SnapshotElement>> registry = new HashMap<>();

    public SnapshotElementFactory(List<String> scanPackages) {
        // Scan all classes from `scanPackages` that use the `SnapshotElementType` decorator.
        // For each found class, store it into the registry. It could be used next to create the correct concrete
        // instance of the correct SnapshotElement subclass.
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SnapshotElementType.class));
        for (String basePackage : scanPackages) {
            for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
                try {
                    Class<?> rawClazz = Class.forName(bd.getBeanClassName());
                    if (SnapshotElement.class.isAssignableFrom(rawClazz)) {
                        Class<? extends SnapshotElement> clazz = (Class<? extends SnapshotElement>) rawClazz;
                        SnapshotElementType annotation = clazz.getAnnotation(SnapshotElementType.class);
                        String type = annotation.value();
                        Constructor<? extends SnapshotElement> constructor = clazz.getConstructor(Element.class);
                        registry.put(type, constructor);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to register scanned class: " + bd.getBeanClassName(), e);
                }
            }
        }
    }

    /**
     * This method allow to parse XML Element (found into a stored snapshot) to create the corresponding SnapshotElement
     * @param element the Element to parse
     * @return a concrete SnapshotElement instance depending on Element
     * @throws IllegalArgumentException if the Element cannot be parsed
     */
    public SnapshotElement parse(Element element) {
        String nodeName = element.getNodeName();
        Constructor<? extends SnapshotElement> constructor = registry.get(nodeName);
        if (constructor == null) {
            throw new IllegalArgumentException("No SnapshotElement found for node name: " + nodeName);
        }
        try {
            return constructor.newInstance(element);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate SnapshotElement for node: " + nodeName, e);
        }
    }
}
