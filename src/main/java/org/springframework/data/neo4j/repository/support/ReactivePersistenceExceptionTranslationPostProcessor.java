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
package org.springframework.data.neo4j.repository.support;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.apiguardian.api.API;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.autoproxy.AbstractBeanFactoryAwareAdvisingPostProcessor;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

/**
 * Bean post-processor that automatically applies persistence exception translation to all methods returning either
 * {@link reactor.core.publisher.Mono} or {@link reactor.core.publisher.Flux} of any bean marked with
 * Spring's @{@link Repository Repository} annotation, adding a corresponding
 * {@link AbstractPointcutAdvisor} to the exposed proxy (either an existing AOP proxy or a newly
 * generated proxy that implements all of the target's interfaces).
 * <p>
 * That proxy will modify the reactive types by the matched method and inject an exception translation into the reactive
 * flow.
 * <p>
 * This class can be declared as a standard bean if you run a lot of custom repositories in which you use either the
 * {@link ReactiveNeo4jTemplate} or the {@link ReactiveNeo4jClient}.
 *
 * @author Michael J. Simons
 * @soundtrack Fatoni - Andorra
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class ReactivePersistenceExceptionTranslationPostProcessor
		extends AbstractBeanFactoryAwareAdvisingPostProcessor {

	private final Class<? extends Annotation> repositoryAnnotationType;

	public ReactivePersistenceExceptionTranslationPostProcessor() {

		this(Repository.class);
	}

	public ReactivePersistenceExceptionTranslationPostProcessor(Class<? extends Annotation> repositoryAnnotationType) {

		Assert.notNull(repositoryAnnotationType, "'repositoryAnnotationType' must not be null");
		this.repositoryAnnotationType = repositoryAnnotationType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);

		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalArgumentException(
					"Cannot use PersistenceExceptionTranslator autodetection without ListableBeanFactory");
		}
		this.advisor = new ReactivePersistenceExceptionTranslationAdvisor((ListableBeanFactory) beanFactory,
				this.repositoryAnnotationType);
	}

	/**
	 * Spring AOP exception translation aspect for use at Repository or DAO layer level. Translates native persistence
	 * exceptions into Spring's DataAccessException hierarchy, based on a given PersistenceExceptionTranslator.
	 */
	static final class ReactivePersistenceExceptionTranslationAdvisor extends AbstractPointcutAdvisor {

		private final ReactivePersistenceExceptionTranslationInterceptor advice;

		private final AnnotationMatchingPointcut pointcut;

		/**
		 * Create a new PersistenceExceptionTranslationAdvisor.
		 *
		 * @param beanFactory the ListableBeanFactory to obtaining all PersistenceExceptionTranslators from
		 * @param repositoryAnnotationType the annotation type to check for
		 */
		ReactivePersistenceExceptionTranslationAdvisor(ListableBeanFactory beanFactory,
				Class<? extends Annotation> repositoryAnnotationType) {

			this.advice = new ReactivePersistenceExceptionTranslationInterceptor(beanFactory);
			this.pointcut = new AnnotationMatchingPointcut(repositoryAnnotationType, true) {
				@Override
				public MethodMatcher getMethodMatcher() {
					return new StaticMethodMatcher() {

						@Override
						public boolean matches(Method method, Class<?> targetClass) {
							Class<?> returnType = method.getReturnType();
							return returnType == Mono.class || returnType == Flux.class;
						}
					};
				}
			};
		}

		@Override
		public Advice getAdvice() {
			return this.advice;
		}

		@Override
		public Pointcut getPointcut() {
			return this.pointcut;
		}
	}
}
