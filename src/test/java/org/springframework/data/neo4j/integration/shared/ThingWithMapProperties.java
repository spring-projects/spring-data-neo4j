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
package org.springframework.data.neo4j.integration.shared;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Test class verifying composite propeties behaviour.
 *
 * @author Michael J. Simons
 */
@Node("MapProperties")
public class ThingWithMapProperties {

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
		VALUE_BA,
		VALUE_BB {
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

	@Id @GeneratedValue
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

	public Long getId() {
		return id;
	}

	public Map<String, LocalDate> getSomeDates() {
		return someDates;
	}

	public void setSomeDates(Map<String, LocalDate> someDates) {
		this.someDates = someDates;
	}

	public Map<String, LocalDate> getSomeOtherDates() {
		return someOtherDates;
	}

	public void setSomeOtherDates(Map<String, LocalDate> someOtherDates) {
		this.someOtherDates = someOtherDates;
	}

	public Map<String, ThingWithCustomTypes.CustomType> getCustomTypeMap() {
		return customTypeMap;
	}

	public void setCustomTypeMap(
			Map<String, ThingWithCustomTypes.CustomType> customTypeMap) {
		this.customTypeMap = customTypeMap;
	}

	public Map<EnumA, LocalDate> getSomeDatesByEnumA() {
		return someDatesByEnumA;
	}

	public void setSomeDatesByEnumA(
			Map<EnumA, LocalDate> someDatesByEnumA) {
		this.someDatesByEnumA = someDatesByEnumA;
	}

	public Map<EnumB, LocalDate> getSomeDatesByEnumB() {
		return someDatesByEnumB;
	}

	public void setSomeDatesByEnumB(
			Map<EnumB, LocalDate> someDatesByEnumB) {
		this.someDatesByEnumB = someDatesByEnumB;
	}

	public Map<String, LocalDate> getDatesWithTransformedKey() {
		return datesWithTransformedKey;
	}

	public void setDatesWithTransformedKey(Map<String, LocalDate> datesWithTransformedKey) {
		this.datesWithTransformedKey = datesWithTransformedKey;
	}

	public Map<EnumB, LocalDate> getDatesWithTransformedKeyAndEnum() {
		return datesWithTransformedKeyAndEnum;
	}

	public void setDatesWithTransformedKeyAndEnum(
			Map<EnumB, LocalDate> datesWithTransformedKeyAndEnum) {
		this.datesWithTransformedKeyAndEnum = datesWithTransformedKeyAndEnum;
	}

	static class LowerCasePropertiesFilter implements BiFunction<CompositeProperty.Phase, String, String> {

		@Override
		public String apply(CompositeProperty.Phase phase, String s) {
			if (s == null) {
				return null;
			}

			switch (phase) {
				case WRITE:
					return s.toLowerCase(Locale.ENGLISH);
				case READ:
					return s.toUpperCase(Locale.ENGLISH);
				default:
					throw new IllegalArgumentException();
			}
		}
	}
}
