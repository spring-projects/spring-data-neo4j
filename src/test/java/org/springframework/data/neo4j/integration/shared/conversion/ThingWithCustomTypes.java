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
package org.springframework.data.neo4j.integration.shared.conversion;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.internal.value.StringValue;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.DateLong;
import org.springframework.data.neo4j.core.support.DateString;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Node("CustomTypes")
public class ThingWithCustomTypes {

	@Id @GeneratedValue private final Long id;

	private CustomType customType;

	@DateLong
	private Date dateAsLong;

	@DateString("yyyy-MM-dd")
	private Date dateAsString;

	public ThingWithCustomTypes(Long id, CustomType customType, Date dateAsLong, Date dateAsString) {
		this.id = id;
		this.customType = customType;
		this.dateAsLong = dateAsLong;
		this.dateAsString = dateAsString;
	}

	public ThingWithCustomTypes withId(Long newId) {
		return new ThingWithCustomTypes(newId, this.customType, this.dateAsLong, this.dateAsString);
	}

	public CustomType getCustomType() {
		return customType;
	}

	public void setDateAsLong(Date dateAsLong) {
		this.dateAsLong = dateAsLong;
	}

	public Long getId() {
		return id;
	}

	public Date getDateAsLong() {
		return dateAsLong;
	}

	public Date getDateAsString() {
		return dateAsString;
	}

	/**
	 * Custom type to convert
	 */
	public static class CustomType {

		private final String value;

		public static CustomType of(String value) {
			return new CustomType(value);
		}

		public String getValue() {
			return value;
		}

		private CustomType(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CustomType that = (CustomType) o;
			return value.equals(that.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}
	}

	/**
	 * Converter that converts the custom type.
	 */
	public static class CustomTypeConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Set<ConvertiblePair> convertiblePairs = new HashSet<>();
			convertiblePairs.add(new ConvertiblePair(Value.class, CustomType.class));
			convertiblePairs.add(new ConvertiblePair(CustomType.class, Value.class));
			return convertiblePairs;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (StringValue.class.isAssignableFrom(sourceType.getType())) {
				return CustomType.of(((StringValue) source).asString());
			} else {
				return Values.value(((CustomType) source).getValue());
			}
		}
	}

	/**
	 * A type that is not bound anywhere but has a converter
	 */
	public static class DifferentType {

		private final String value;

		public static DifferentType of(String value) {
			return new DifferentType(value);
		}

		private DifferentType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	/**
	 * Converter for an arbitrary type not bound to any property
	 */
	public static class DifferentTypeConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			Set<ConvertiblePair> convertiblePairs = new HashSet<>();
			convertiblePairs.add(new ConvertiblePair(Value.class, DifferentType.class));
			convertiblePairs.add(new ConvertiblePair(DifferentType.class, Value.class));
			return convertiblePairs;
		}

		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {

			if (Value.class.isAssignableFrom(sourceType.getType())) {
				return CustomType.of(((Value) source).asString());
			} else {
				return Values.value(((DifferentType) source).getValue());
			}
		}
	}
}
