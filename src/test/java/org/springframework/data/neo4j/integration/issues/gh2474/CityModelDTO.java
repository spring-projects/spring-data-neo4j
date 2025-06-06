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
package org.springframework.data.neo4j.integration.issues.gh2474;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Stephen Jackson
 */
public class CityModelDTO {

	public PersonModelDTO mayor;

	public List<PersonModelDTO> citizens = new ArrayList<>();

	public List<JobRelationshipDTO> cityEmployees = new ArrayList<>();

	private UUID cityId;

	private String name;

	private String exoticProperty;

	public CityModelDTO() {
	}

	public UUID getCityId() {
		return this.cityId;
	}

	public void setCityId(UUID cityId) {
		this.cityId = cityId;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExoticProperty() {
		return this.exoticProperty;
	}

	public void setExoticProperty(String exoticProperty) {
		this.exoticProperty = exoticProperty;
	}

	public PersonModelDTO getMayor() {
		return this.mayor;
	}

	public void setMayor(PersonModelDTO mayor) {
		this.mayor = mayor;
	}

	public List<PersonModelDTO> getCitizens() {
		return this.citizens;
	}

	public void setCitizens(List<PersonModelDTO> citizens) {
		this.citizens = citizens;
	}

	public List<JobRelationshipDTO> getCityEmployees() {
		return this.cityEmployees;
	}

	public void setCityEmployees(List<JobRelationshipDTO> cityEmployees) {
		this.cityEmployees = cityEmployees;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof CityModelDTO;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof CityModelDTO)) {
			return false;
		}
		final CityModelDTO other = (CityModelDTO) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$cityId = this.getCityId();
		final Object other$cityId = other.getCityId();
		if (!Objects.equals(this$cityId, other$cityId)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (!Objects.equals(this$name, other$name)) {
			return false;
		}
		final Object this$exoticProperty = this.getExoticProperty();
		final Object other$exoticProperty = other.getExoticProperty();
		if (!Objects.equals(this$exoticProperty, other$exoticProperty)) {
			return false;
		}
		final Object this$mayor = this.getMayor();
		final Object other$mayor = other.getMayor();
		if (!Objects.equals(this$mayor, other$mayor)) {
			return false;
		}
		final Object this$citizens = this.getCitizens();
		final Object other$citizens = other.getCitizens();
		if (!Objects.equals(this$citizens, other$citizens)) {
			return false;
		}
		final Object this$cityEmployees = this.getCityEmployees();
		final Object other$cityEmployees = other.getCityEmployees();
		return Objects.equals(this$cityEmployees, other$cityEmployees);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $cityId = this.getCityId();
		result = result * PRIME + (($cityId != null) ? $cityId.hashCode() : 43);
		final Object $name = this.getName();
		result = result * PRIME + (($name != null) ? $name.hashCode() : 43);
		final Object $exoticProperty = this.getExoticProperty();
		result = result * PRIME + (($exoticProperty != null) ? $exoticProperty.hashCode() : 43);
		final Object $mayor = this.getMayor();
		result = result * PRIME + (($mayor != null) ? $mayor.hashCode() : 43);
		final Object $citizens = this.getCitizens();
		result = result * PRIME + (($citizens != null) ? $citizens.hashCode() : 43);
		final Object $cityEmployees = this.getCityEmployees();
		result = result * PRIME + (($cityEmployees != null) ? $cityEmployees.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "CityModelDTO(cityId=" + this.getCityId() + ", name=" + this.getName() + ", exoticProperty="
				+ this.getExoticProperty() + ", mayor=" + this.getMayor() + ", citizens=" + this.getCitizens()
				+ ", cityEmployees=" + this.getCityEmployees() + ")";
	}

	/**
	 * Nested projection
	 */
	public static class PersonModelDTO {

		private UUID personId;

		public PersonModelDTO() {
		}

		public UUID getPersonId() {
			return this.personId;
		}

		public void setPersonId(UUID personId) {
			this.personId = personId;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PersonModelDTO;
		}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof PersonModelDTO)) {
				return false;
			}
			final PersonModelDTO other = (PersonModelDTO) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$personId = this.getPersonId();
			final Object other$personId = other.getPersonId();
			return Objects.equals(this$personId, other$personId);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $personId = this.getPersonId();
			result = result * PRIME + (($personId != null) ? $personId.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "CityModelDTO.PersonModelDTO(personId=" + this.getPersonId() + ")";
		}

	}

	/**
	 * Nested projection
	 */
	public static class JobRelationshipDTO {

		private PersonModelDTO person;

		public JobRelationshipDTO() {
		}

		public PersonModelDTO getPerson() {
			return this.person;
		}

		public void setPerson(PersonModelDTO person) {
			this.person = person;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof JobRelationshipDTO;
		}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof JobRelationshipDTO)) {
				return false;
			}
			final JobRelationshipDTO other = (JobRelationshipDTO) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$person = this.getPerson();
			final Object other$person = other.getPerson();
			return Objects.equals(this$person, other$person);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $person = this.getPerson();
			result = result * PRIME + (($person != null) ? $person.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "CityModelDTO.JobRelationshipDTO(person=" + this.getPerson() + ")";
		}

	}

}
