/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core.support;

/**
 * Interface to implement for a concrete implementation (not an inherited class or
 * interface). Returns if an entity and its relationships needs to be processed for
 * updates or not.
 *
 * @param <T> type to implement the needs update logic for
 * @author Gerrit Meier
 */
public interface NeedsUpdateEvaluator<T> {

	/**
	 * Report if this entity needs to be considered for an update. This includes possible
	 * relationships.
	 * @param instance instance of type `T` to check
	 * @return true, if it should be processed
	 */
	default boolean needsUpdate(T instance) {
		return true;
	}

	Class<T> getEvaluatingClass();

}
