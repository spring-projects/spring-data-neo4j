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
package org.springframework.data.neo4j.integration.shared.conversion;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Test class verifying composite properties behaviour.
 *
 * @author Michael J. Simons
 */
@Node("CompositeProperties")
public class ThingWithCompositeProperties {

	@Id
	@GeneratedValue
	private Long id;

	@CompositeProperty
	private Map<String, LocalDate> someDates;

	@CompositeProperty(prefix = "in_another_time")
	private Map<String, LocalDate> someOtherDates;

	@CompositeProperty(transformKeysWith = LowerCasePropertiesFilter.class)
	private Map<String, LocalDate> datesWithTransformedKey;

	@CompositeProperty
	private Map<String, ThingWithCustomTypes.CustomType> customTypeMap;

	@CompositeProperty
	private Map<EnumA, LocalDate> someDatesByEnumA;

	@CompositeProperty
	private Map<EnumB, LocalDate> someDatesByEnumB;

	@CompositeProperty(transformKeysWith = LowerCasePropertiesFilter.class)
	private Map<EnumB, LocalDate> datesWithTransformedKeyAndEnum;

	@Relationship("IRRELEVANT_TYPE")
	private RelationshipWithCompositeProperties relationship;

	@CompositeProperty(converter = SomeOtherDTOToMapConverter.class, prefix = "dto")
	private SomeOtherDTO someOtherDTO;

	public Long getId() {
		return this.id;
	}

	public Map<String, LocalDate> getSomeDates() {
		return this.someDates;
	}

	public void setSomeDates(Map<String, LocalDate> someDates) {
		this.someDates = someDates;
	}

	public Map<String, LocalDate> getSomeOtherDates() {
		return this.someOtherDates;
	}

	public void setSomeOtherDates(Map<String, LocalDate> someOtherDates) {
		this.someOtherDates = someOtherDates;
	}

	public Map<String, ThingWithCustomTypes.CustomType> getCustomTypeMap() {
		return this.customTypeMap;
	}

	public void setCustomTypeMap(Map<String, ThingWithCustomTypes.CustomType> customTypeMap) {
		this.customTypeMap = customTypeMap;
	}

	public Map<EnumA, LocalDate> getSomeDatesByEnumA() {
		return this.someDatesByEnumA;
	}

	public void setSomeDatesByEnumA(Map<EnumA, LocalDate> someDatesByEnumA) {
		this.someDatesByEnumA = someDatesByEnumA;
	}

	public Map<EnumB, LocalDate> getSomeDatesByEnumB() {
		return this.someDatesByEnumB;
	}

	public void setSomeDatesByEnumB(Map<EnumB, LocalDate> someDatesByEnumB) {
		this.someDatesByEnumB = someDatesByEnumB;
	}

	public Map<String, LocalDate> getDatesWithTransformedKey() {
		return this.datesWithTransformedKey;
	}

	public void setDatesWithTransformedKey(Map<String, LocalDate> datesWithTransformedKey) {
		this.datesWithTransformedKey = datesWithTransformedKey;
	}

	public Map<EnumB, LocalDate> getDatesWithTransformedKeyAndEnum() {
		return this.datesWithTransformedKeyAndEnum;
	}

	public void setDatesWithTransformedKeyAndEnum(Map<EnumB, LocalDate> datesWithTransformedKeyAndEnum) {
		this.datesWithTransformedKeyAndEnum = datesWithTransformedKeyAndEnum;
	}

	public SomeOtherDTO getSomeOtherDTO() {
		return this.someOtherDTO;
	}

	public void setSomeOtherDTO(SomeOtherDTO someOtherDTO) {
		this.someOtherDTO = someOtherDTO;
	}

	public RelationshipWithCompositeProperties getRelationship() {
		return this.relationship;
	}

	public void setRelationship(RelationshipWithCompositeProperties relationship) {
		this.relationship = relationship;
	}

	/**
	 * Map by enum key.
	 */
	public enum EnumA {

		VALUE_AA

	}

	/**
	 * Map by enum key.
	 */
	public enum EnumB {

		VALUE_BA, VALUE_BB {
			@Override
			public String toString() {
				return "Ich bin superwitzig.";
			}
		};

		@Override
		public String toString() {
			return super.name() + " deliberately screw the enum combo toString/name.";
		}

	}

	/**
	 * Arbitrary DTO.
	 */
	public static class SomeOtherDTO {

		final String x;

		final Long y;

		final Double z;

		public SomeOtherDTO(String x, Long y, Double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SomeOtherDTO that = (SomeOtherDTO) o;
			return this.x.equals(that.x) && this.y.equals(that.y) && this.z.equals(that.z);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.x, this.y, this.z);
		}

	}

	static class LowerCasePropertiesFilter implements BiFunction<CompositeProperty.Phase, String, String> {

		@Override
		public String apply(CompositeProperty.Phase phase, String s) {
			if (s == null) {
				return null;
			}

			return switch (phase) {
				case WRITE -> s.toLowerCase(Locale.ENGLISH);
				case READ -> s.toUpperCase(Locale.ENGLISH);
				default -> throw new IllegalArgumentException();
			};
		}

	}

	/**
	 * Some arbitrary converter.
	 */
	static class SomeOtherDTOToMapConverter implements Neo4jPersistentPropertyToMapConverter<String, SomeOtherDTO> {

		@Override
		public Map<String, Value> decompose(SomeOtherDTO property, Neo4jConversionService conversionService) {

			final HashMap<String, Value> decomposed = new HashMap<>();
			if (property == null) {
				decomposed.put("x", Values.NULL);
				decomposed.put("y", Values.NULL);
				decomposed.put("z", Values.NULL);
			}
			else {
				decomposed.put("x", Values.value(property.x));
				decomposed.put("y", Values.value(property.y));
				decomposed.put("z", Values.value(property.z));
			}
			return decomposed;
		}

		@Override
		public SomeOtherDTO compose(Map<String, Value> source, Neo4jConversionService conversionService) {
			return source.isEmpty() ? null : new SomeOtherDTO(source.get("x").asString(), source.get("y").asLong(),
					source.get("z").asDouble());
		}

	}

}
