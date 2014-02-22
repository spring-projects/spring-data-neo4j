package org.springframework.data.neo4j.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * @author mh
 * @since 23.02.14
 */
public class BasePackageScanner {

    public static Set<String> scanBasePackage(String basePackage) {
        ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
        componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));

        Set<String> classes = new ManagedSet<String>();
        for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
            classes.add(candidate.getBeanClassName());
        }

        return classes;
    }

    static Set<? extends Class<?>> scanBasePackageForClasses(String basePackage) throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        for (String className : scanBasePackage(basePackage)){
            classes.add(loadClass(className));
        }
        return classes;
    }

    private static Class loadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }
}
