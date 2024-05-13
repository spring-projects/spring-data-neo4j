/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.core;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

import org.springframework.core.KotlinDetector;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.PreferredConstructorDiscoverer;

import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;

/**
 * @author Michael J. Simons
 */
final class KPropertyFilterSupport {

	/**
	 * Determines all required constructor args for a Kotlin type
	 *
	 * @param type the type for which required constructor args must be determined
	 * @return a list of property names that need to be fetched
	 */
	static Collection<String> getRequiredProperties(Class<?> type) {
		if (!(KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type))) {
			return Collections.emptyList();
		}

		PreferredConstructor<?, ?> discover = PreferredConstructorDiscoverer.discover(type);
		if (discover == null) {
			return Collections.emptyList();
		}

		var preferredConstructor = ReflectJvmMapping.getKotlinFunction(discover.getConstructor());
		if (preferredConstructor == null) {
			return Collections.emptyList();
		}

		return preferredConstructor.getParameters().stream()
				.filter(Predicate.not(KParameter::isOptional))
				.map(KParameter::getName)
				.toList();
	}

	private KPropertyFilterSupport() {
	}
}
