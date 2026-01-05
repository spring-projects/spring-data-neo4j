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
package org.springframework.data.neo4j.integration.issues.gh2474;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Stephen Jackson
 */
public class CityModelDTO {
	private UUID cityId;
	private String name;
	private String exoticProperty;

	public PersonModelDTO mayor;
	public List<PersonModelDTO> citizens = new ArrayList<>();
	public List<JobRelationshipDTO> cityEmployees = new ArrayList<>();

	public CityModelDTO() {
	}

	public UUID getCityId() {
		return this.cityId;
	}

	public String getName() {
		return this.name;
	}

	public String getExoticProperty() {
		return this.exoticProperty;
	}

	public PersonModelDTO getMayor() {
		return this.mayor;
	}

	public List<PersonModelDTO> getCitizens() {
		return this.citizens;
	}

	public List<JobRelationshipDTO> getCityEmployees() {
		return this.cityEmployees;
	}

	public void setCityId(UUID cityId) {
		this.cityId = cityId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setExoticProperty(String exoticProperty) {
		this.exoticProperty = exoticProperty;
	}

	public void setMayor(PersonModelDTO mayor) {
		this.mayor = mayor;
	}

	public void setCitizens(List<PersonModelDTO> citizens) {
		this.citizens = citizens;
	}

	public void setCityEmployees(List<JobRelationshipDTO> cityEmployees) {
		this.cityEmployees = cityEmployees;
	}

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
		if (this$cityId == null ? other$cityId != null : !this$cityId.equals(other$cityId)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		final Object this$exoticProperty = this.getExoticProperty();
		final Object other$exoticProperty = other.getExoticProperty();
		if (this$exoticProperty == null ? other$exoticProperty != null : !this$exoticProperty.equals(other$exoticProperty)) {
			return false;
		}
		final Object this$mayor = this.getMayor();
		final Object other$mayor = other.getMayor();
		if (this$mayor == null ? other$mayor != null : !this$mayor.equals(other$mayor)) {
			return false;
		}
		final Object this$citizens = this.getCitizens();
		final Object other$citizens = other.getCitizens();
		if (this$citizens == null ? other$citizens != null : !this$citizens.equals(other$citizens)) {
			return false;
		}
		final Object this$cityEmployees = this.getCityEmployees();
		final Object other$cityEmployees = other.getCityEmployees();
		if (this$cityEmployees == null ? other$cityEmployees != null : !this$cityEmployees.equals(other$cityEmployees)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof CityModelDTO;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $cityId = this.getCityId();
		result = result * PRIME + ($cityId == null ? 43 : $cityId.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $exoticProperty = this.getExoticProperty();
		result = result * PRIME + ($exoticProperty == null ? 43 : $exoticProperty.hashCode());
		final Object $mayor = this.getMayor();
		result = result * PRIME + ($mayor == null ? 43 : $mayor.hashCode());
		final Object $citizens = this.getCitizens();
		result = result * PRIME + ($citizens == null ? 43 : $citizens.hashCode());
		final Object $cityEmployees = this.getCityEmployees();
		result = result * PRIME + ($cityEmployees == null ? 43 : $cityEmployees.hashCode());
		return result;
	}

	public String toString() {
		return "CityModelDTO(cityId=" + this.getCityId() + ", name=" + this.getName() + ", exoticProperty=" + this.getExoticProperty() + ", mayor=" + this.getMayor() + ", citizens=" + this.getCitizens() + ", cityEmployees=" + this.getCityEmployees() + ")";
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
			if (this$personId == null ? other$personId != null : !this$personId.equals(other$personId)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof PersonModelDTO;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $personId = this.getPersonId();
			result = result * PRIME + ($personId == null ? 43 : $personId.hashCode());
			return result;
		}

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
			if (this$person == null ? other$person != null : !this$person.equals(other$person)) {
				return false;
			}
			return true;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof JobRelationshipDTO;
		}

		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $person = this.getPerson();
			result = result * PRIME + ($person == null ? 43 : $person.hashCode());
			return result;
		}

		public String toString() {
			return "CityModelDTO.JobRelationshipDTO(person=" + this.getPerson() + ")";
		}
	}
}
