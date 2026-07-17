/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh3109;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

/**
 * @author Francesco Chicchiriccò
 */
abstract class SerializableListConverter<T extends Serializable> implements Neo4jPersistentPropertyConverter<List<T>> {

	private static final Logger LOG = LoggerFactory.getLogger(SerializableListConverter.class);

	private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

	private static String serialize(final Object object) {
		String result = null;

		try {
			result = MAPPER.writeValueAsString(object);
		}
		catch (Exception ex) {
			LOG.error("During serialization", ex);
		}

		return result;
	}

	private static <O> O deserialize(final String serialized, final TypeReference<O> reference) {
		O result = null;

		try {
			result = MAPPER.readValue(serialized, reference);
		}
		catch (Exception ex) {
			LOG.error("During deserialization", ex);
		}

		return result;
	}

	protected abstract TypeReference<List<T>> typeRef();

	@Override
	public Value write(final List<T> source) {
		return Optional.ofNullable(source)
			.map(SerializableListConverter::serialize)
			.map(Values::value)
			.orElse(Values.value(List.of()));
	}

	@Override
	public List<T> read(final Value source) {
		return Optional.ofNullable(source)
			.map(data -> deserialize(source.asString(), typeRef()))
			.orElseGet(ArrayList::new);
	}

}
