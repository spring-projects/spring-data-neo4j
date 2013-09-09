/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.cdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * CDI extension to export Neo4j repositories.
 * 
 * @author Nicki Watt
 */
public class Neo4jCdiRepositoryExtension extends CdiRepositoryExtensionSupport {

	private static final Logger LOG = LoggerFactory.getLogger(Neo4jCdiRepositoryExtension.class);

	private final Map<Set<Annotation>, Bean<Neo4jMappingContext>> neo4jMappingContexts = new HashMap<Set<Annotation>, Bean<Neo4jMappingContext>>();
    private final Map<Set<Annotation>, Bean<Neo4jTemplate>> neo4jTemplates = new HashMap<Set<Annotation>, Bean<Neo4jTemplate>>();

    public Neo4jCdiRepositoryExtension() {
		LOG.info("Activating CDI extension for Spring Data Neo4j repositories.");
	}

	@SuppressWarnings("unchecked")
	<X> void processBean(@Observes ProcessBean<X> processBean) {

		Bean<X> bean = processBean.getBean();

		for (Type type : bean.getTypes()) {
			if (type instanceof Class<?> && Neo4jMappingContext.class.isAssignableFrom((Class<?>) type)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Discovered %s with qualifiers %s.", Neo4jMappingContext.class.getName(),
							bean.getQualifiers()));
				}
				// Store the EntityManager bean using its qualifiers.
				neo4jMappingContexts.put(new HashSet<Annotation>(bean.getQualifiers()), (Bean<Neo4jMappingContext>) bean);
			}

            if (type instanceof Class<?> && Neo4jTemplate.class.isAssignableFrom((Class<?>) type)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format("Discovered %s with qualifiers %s.", Neo4jTemplate.class.getName(),
                            bean.getQualifiers()));
                }
                // Store the EntityManager bean using its qualifiers.
                neo4jTemplates.put(new HashSet<Annotation>(bean.getQualifiers()), (Bean<Neo4jTemplate>) bean);
            }
		}
	}

	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

		for (Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {

			Class<?> repositoryType = entry.getKey();
			Set<Annotation> qualifiers = entry.getValue();

			// Create the bean representing the repository.
			Bean<?> repositoryBean = createRepositoryBean(repositoryType, qualifiers, beanManager);

			if (LOG.isInfoEnabled()) {
				LOG.info(String.format("Registering bean for %s with qualifiers %s.", repositoryType.getName(), qualifiers));
			}

			// Register the bean to the container.
			afterBeanDiscovery.addBean(repositoryBean);
		}
	}

	/**
	 * Creates a {@link Bean}.
	 * 
	 * @param <T> The type of the repository.
	 * @param repositoryType The class representing the repository.
	 * @param beanManager The BeanManager instance.
	 * @return The bean.
	 */
	private <T> Bean<T> createRepositoryBean(Class<T> repositoryType, Set<Annotation> qualifiers, BeanManager beanManager) {

		// Determine the Neo4jOperations bean which matches the qualifiers of the repository.
		Bean<Neo4jMappingContext> neo4jMappingContextBean = this.neo4jMappingContexts.get(qualifiers);
        Bean<Neo4jTemplate> neo4jTemplateBean = this.neo4jTemplates.get(qualifiers);

		if (neo4jMappingContextBean == null) {
			throw new UnsatisfiedResolutionException(String.format("Unable to resolve a bean for '%s' with qualifiers %s.",
                    Neo4jMappingContext.class.getName(), qualifiers));
		}
        if (neo4jTemplateBean == null) {
            throw new UnsatisfiedResolutionException(String.format("Unable to resolve a bean for '%s' with qualifiers %s.",
                    Neo4jTemplate.class.getName(), qualifiers));
        }

		// Construct and return the repository bean.
		return new Neo4jCdiRepositoryBean<T>(neo4jMappingContextBean, neo4jTemplateBean, qualifiers, repositoryType, beanManager);
	}
}
