/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import java.util.Optional;

import org.springframework.core.convert.converter.Converter;

/**
 * Used to unwrap optionals before further processing by a
 * {@link org.springframework.data.repository.query.ResultProcessor}.
 *
 * @author Michael J. Simons
 */
enum OptionalUnwrappingConverter implements Converter<Object, Object> {
	INSTANCE;

	@Override
	public Object convert(Object source) {
		if (source instanceof Optional) {
			return ((Optional<?>) source).orElse(null);
		}
		return source;
	}
}
