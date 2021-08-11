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

import java.util.Map;
import java.util.function.Function;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.ChainedPersistenceExceptionTranslator;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.Assert;

/**
 * This method interceptor is modelled somewhat after
 * {@link org.springframework.dao.support.PersistenceExceptionTranslationInterceptor}, but caters for reactive needs: If
 * the method identified by the pointcut returns a supported reactive type (either {@link Mono} or {@link Flux}), it
 * installs an error mapping function with {@code onErrorMap} that tries to translate the given exception into Spring's
 * hierarchy.
 * <p>
 * The interceptor uses all {@link PersistenceExceptionTranslator persistence exception translators} it finds in the
 * context through a {@link ChainedPersistenceExceptionTranslator}. Translations is eventually done with
 * {@link DataAccessUtils#translateIfNecessary(RuntimeException, PersistenceExceptionTranslator)} which returns the
 * original exception in case translation is not possible (the translator returned null).
 *
 * @author Michael J. Simons
 * @soundtrack Fatoni - Andorra
 * @since 6.0
 */
final class ReactivePersistenceExceptionTranslationInterceptor implements MethodInterceptor {

	private final ListableBeanFactory beanFactory;

	private volatile PersistenceExceptionTranslator persistenceExceptionTranslator;

	/**
	 * Create a new PersistenceExceptionTranslationInterceptor, autodetecting PersistenceExceptionTranslators in the given
	 * BeanFactory.
	 *
	 * @param beanFactory the ListableBeanFactory to obtaining all PersistenceExceptionTranslators from
	 */
	ReactivePersistenceExceptionTranslationInterceptor(ListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {

		// Invoke the method potentially returning a reactive type
		Object m = mi.proceed();

		PersistenceExceptionTranslator translator = getPersistenceExceptionTranslator();
		// Add the translation. Nothing will happen if no-one subscribe the reactive result.
		Function<RuntimeException, Throwable> errorMappingFunction = t -> t instanceof DataAccessException ? t
				: DataAccessUtils.translateIfNecessary(t, translator);
		if (m instanceof Mono) {
			return ((Mono<?>) m).onErrorMap(RuntimeException.class, errorMappingFunction);
		} else if (m instanceof Flux) {
			return ((Flux<?>) m).onErrorMap(RuntimeException.class, errorMappingFunction);
		} else {
			return m;
		}
	}

	PersistenceExceptionTranslator getPersistenceExceptionTranslator() {

		PersistenceExceptionTranslator translator = this.persistenceExceptionTranslator;
		if (translator == null) {
			synchronized (this) {
				translator = this.persistenceExceptionTranslator;
				if (translator == null) {
					this.persistenceExceptionTranslator = detectPersistenceExceptionTranslators();
					translator = this.persistenceExceptionTranslator;
				}
			}
		}
		return translator;
	}

	/**
	 * Detect all PersistenceExceptionTranslators in the given BeanFactory.
	 *
	 * @return a chained PersistenceExceptionTranslator, combining all PersistenceExceptionTranslators found in the
	 *         factory
	 * @see ChainedPersistenceExceptionTranslator
	 */
	private PersistenceExceptionTranslator detectPersistenceExceptionTranslators() {
		// Find all translators, being careful not to activate FactoryBeans.
		Map<String, PersistenceExceptionTranslator> pets = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory,
				PersistenceExceptionTranslator.class, false, false);
		ChainedPersistenceExceptionTranslator cpet = new ChainedPersistenceExceptionTranslator();
		pets.values().forEach(cpet::addDelegate);
		return cpet;
	}
}
