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
package org.springframework.data.neo4j.integration.shared.common;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class Person {

	@Id
	@GeneratedValue
	private Long id;
	private String firstName;
	private String lastName;

	private int primitiveValue; // never used but always null

	@Relationship("LIVES_AT")
	private Address address;

	/**
	 * Address of a person.
	 */
	@Node
	public static class Address {
		@Id
		private Long id;
		private String zipCode;
		private String city;
		private String street;

		@Relationship("BASED_IN")
		private Country country;

		public Long getId() {
			return id;
		}

		public String getZipCode() {
			return zipCode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public Country getCountry() {
			return country;
		}

		public void setCountry(Country country) {
			this.country = country;
		}

		/**
		 * Just another country to persist
		 */
		@Node("YetAnotherCountryEntity")
		public static class Country {
			@Id
			@GeneratedValue
			private Long id;
			private String name;
			private String countryCode;

			public String getCountryCode() {
				return countryCode;
			}

			public void setCountryCode(String countryCode) {
				this.countryCode = countryCode;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}
	}

	public Long getId() {
		return id;
	}

	// The getters are needed for Spring Expression Language in `NamesOnly`
	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Address getAddress() {
		return address;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public String toString() {
		return "Person{" + "id=" + id + ", firstName='" + firstName + '\'' + ", lastName='" + lastName + '\'' + ", address="
				+ address + '}';
	}
}
