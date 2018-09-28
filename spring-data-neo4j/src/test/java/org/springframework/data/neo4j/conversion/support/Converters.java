/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.conversion.support;

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
			return null;
		}

		@Override
		public Double toEntityAttribute(String value) {
			return null;
		}
	}

	public static class ListToStringConverter implements AttributeConverter<List<Double>, String> {

		@Override
		public String toGraphProperty(List<Double> value) {
			return null;
		}

		@Override
		public List<Double> toEntityAttribute(String value) {
			return null;
		}
	}
}
