/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.conversion.support;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.neo4j.ogm.typeconversion.AttributeConverter;

/**
 * Wraps a some converters.
 *
 * @author Michael J. Simons
 * @soundtrack Murray Gold - Doctor Who Season 9
 */
public final class Converters {

	private Converters() {
	}

	/**
	 * Concrete implementation doesn't matter and is meaningless on purpose.
	 *
	 * @param <T>
	 */
	public static abstract class AbstractObjectToString<T> implements AttributeConverter<T, String> {

		@Override
		public String toGraphProperty(T value) {
			return "n/a";
		}

		@Override
		public T toEntityAttribute(String value) {
			try {
				return getTypeClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		protected abstract Class<T> getTypeClass();
	}

	public static class ConvertedClassToStringConverter extends AbstractObjectToString<ConvertedClass> {

		@Override
		protected Class<ConvertedClass> getTypeClass() {
			return ConvertedClass.class;
		}
	}

	public static class DoubleToStringConverter implements AttributeConverter<Double, String> {

		@Override
		public String toGraphProperty(Double value) {
			return "that has been a double";
		}

		@Override
		public Double toEntityAttribute(String value) {
			return null;
		}
	}

	public static class ListToStringConverter implements AttributeConverter<List<Double>, String> {

		@Override
		public String toGraphProperty(List<Double> value) {
			return value.stream().map(d -> d.toString()).collect(joining(","));
		}

		@Override
		public List<Double> toEntityAttribute(String value) {
			return null;
		}
	}
}
