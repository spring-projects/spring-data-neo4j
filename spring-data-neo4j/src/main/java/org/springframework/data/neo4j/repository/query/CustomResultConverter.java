/*
 * Copyright 2011-2019 the original author or authors.
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

import static java.lang.reflect.Proxy.*;

import java.util.Map;

import org.neo4j.ogm.context.SingleUseEntityMapper;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.reflect.EntityFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * Convert SDN special type {@link QueryResult} into SD understandable type.
 *
 * @author Nicolas Mervaillie
 */
class CustomResultConverter implements Converter<Object, Object> {

	private final MetaData metaData;
	private final Class returnedType;

	CustomResultConverter(MetaData metaData, Class<?> returnedType) {

		this.metaData = metaData;
		this.returnedType = returnedType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object convert(Object source) {
		if (returnedType.getAnnotation(QueryResult.class) == null) {
			return source;
		}
		SingleUseEntityMapper mapper = new SingleUseEntityMapper(metaData, new EntityFactory(metaData));
		if (returnedType.isInterface()) {
			Class<?>[] interfaces = new Class<?>[] { returnedType };
			return newProxyInstance(returnedType.getClassLoader(), interfaces,
					new QueryResultProxy((Map<String, Object>) source));
		}
		return mapper.map(returnedType, (Map<String, Object>) source);
	}
}
