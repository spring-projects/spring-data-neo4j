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
package org.springframework.data.neo4j.core.mapping;

import org.apiguardian.api.API;
import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * Thrown when required information about a class or primary label is requested from the {@link Schema} and that
 * information is not available.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class UnknownEntityException extends InvalidDataAccessApiUsageException {

	private final Class<?> targetClass;

	public UnknownEntityException(Class<?> targetClass) {
		super(String.format("%s is not a known entity", targetClass.getName()));
		this.targetClass = targetClass;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}
}
