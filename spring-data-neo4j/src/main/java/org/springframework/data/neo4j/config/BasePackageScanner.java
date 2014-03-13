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
        return scanBasePackages(basePackage.split(","));
    }

    public static Set<String> scanBasePackages(String...basePackages) {
        ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
//        componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));
        componentProvider.addIncludeFilter(new AnnotationTypeFilter(NodeEntity.class));
        componentProvider.addIncludeFilter(new AnnotationTypeFilter(RelationshipEntity.class));

        Set<String> classes = new ManagedSet<String>();
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {
                classes.add(candidate.getBeanClassName());
            }
        }

        return classes;
    }

    public static Set<? extends Class<?>> scanBasePackageForClasses(String...basePackages) throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<>();
        for (String basePackage : basePackages) {
            for (String className : scanBasePackage(basePackage)){
                classes.add(loadClass(className));
            }
        }
        return classes;
    }

    private static Class loadClass(String className) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(className);
    }
}
