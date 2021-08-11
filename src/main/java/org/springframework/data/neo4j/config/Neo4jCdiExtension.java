/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.config;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryCdiBean;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;
import org.springframework.data.repository.config.CustomRepositoryImplementationDetector;

/**
 * This CDI extension enables Spring Data Neo4j on a CDI 2.0 compatible CDI container. It creates a Neo4j client, template
 * and brings in the Neo4j repository mechanism as well. It is the main entry point to our CDI support.
 * <p/>
 * It requires the presence of a Neo4j Driver bean. Other beans, like the {@link org.springframework.data.neo4j.core.convert.Neo4jConversions}
 * can be overwritten by providing a producer of it. If such a producer or bean is added, it must not use any {@link javax.inject.Qualifier @Qualifier}
 * on the bean.
 * <p/>
 * This CDI extension can be used either via a build in service loader mechanism or through building a context manually.
 *
 * @author Michael J. Simons
 * @soundtrack Juse Ju - Millennium
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jCdiExtension extends CdiRepositoryExtensionSupport {

	/**
	 * An annotation literal used for selecting default CDI beans.
	 */
	public static final AnnotationLiteral<Default> DEFAULT_BEAN = new AnnotationLiteral<Default>() {
		@Override public Class<? extends Annotation> annotationType() {
			return Default.class;
		}
	};
	/**
	 * An annotation literal used for selecting {@link Any @Any} annotated beans.
	 */
	public static final AnnotationLiteral<Any> ANY_BEAN = new AnnotationLiteral<Any>() {
		@Override public Class<? extends Annotation> annotationType() {
			return Any.class;
		}
	};

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jCdiExtension.class));

	public Neo4jCdiExtension() {
		log.info("Activating CDI extension for Spring Data Neo4j repositories.");
	}

	void addNeo4jBeansProducer(@Observes BeforeBeanDiscovery event) {
		event.addAnnotatedType(Neo4jCdiConfigurationSupport.class, "Neo4jCDIConfigurationSupport");
	}

	void registerRepositoryFactoryBeanPerRepositoryType(@Observes AfterBeanDiscovery event, BeanManager beanManager) {

		CustomRepositoryImplementationDetector optionalCustomRepositoryImplementationDetector = getCustomImplementationDetector();

		for (Map.Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {

			Class<?> repositoryType = entry.getKey();
			Set<Annotation> qualifiers = entry.getValue();

			Neo4jRepositoryFactoryCdiBean<?> repositoryBean = new Neo4jRepositoryFactoryCdiBean<>(
					qualifiers,
					repositoryType,
					beanManager,
					optionalCustomRepositoryImplementationDetector
			);

			registerBean(repositoryBean);
			event.addBean(repositoryBean);
		}
	}
}
