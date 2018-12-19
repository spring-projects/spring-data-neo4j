package org.springframework.data.neo4j.test;

import java.util.Map;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Helper for programmatically creating a {@link SessionFactory session factory} using a well defined
 * {@link org.neo4j.ogm.config.Configuration configuration} and a list of domain packages from the
 * {@link Neo4jIntegrationTest}-annotation.
 *
 * @author Michael J. Simons
 * @since 5.2
 * @soundtrack Die Ärzte - Nach uns die Sintflut
 */
class SessionFactoryRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata annotationMetadata,
			BeanDefinitionRegistry beanDefinitionRegistry) {

		Map<String, Object> t = annotationMetadata.getAnnotationAttributes(Neo4jIntegrationTest.class.getName());

		AbstractBeanDefinition sessionFactoryDefinition = BeanDefinitionBuilder //
				.rootBeanDefinition(SessionFactory.class) //
				.addConstructorArgReference("neo4jOGMConfiguration") //
				.addConstructorArgValue(t.get("domainPackages")) //
				.getBeanDefinition();

		beanDefinitionRegistry.registerBeanDefinition("sessionFactory", sessionFactoryDefinition);
	}
}
