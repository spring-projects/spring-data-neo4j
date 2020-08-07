package org.springframework.data.neo4j.repository.support;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

/**
 * @author Michael J. Simons
 * @soundtrack Juse Ju - Millennium
 * @since 6.0
 */
public class SomeOtherExtension extends CdiRepositoryExtensionSupport {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(SomeOtherExtension.class));

	public SomeOtherExtension() {
		log.info("Activating CDI extension for Spring Data Neo4j repositories.");
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

		for (Map.Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {

			Class<?> repositoryType = entry.getKey();
			Set<Annotation> qualifiers = entry.getValue();

			Neo4jCdiRepositoryBean<?> repositoryBean = new Neo4jCdiRepositoryBean<>(qualifiers, repositoryType,
					beanManager, Optional
					.ofNullable(getCustomImplementationDetector()));

			registerBean(repositoryBean);
			afterBeanDiscovery.addBean(repositoryBean);
		}
	}
}
