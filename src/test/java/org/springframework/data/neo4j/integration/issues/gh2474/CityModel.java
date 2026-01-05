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

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Stephen Jackson
 */
@Node
public class CityModel {
	@Id
	@GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
	private UUID cityId;

	@Relationship(value = "MAYOR")
	private PersonModel mayor;

	@Relationship(value = "CITIZEN")
	private List<PersonModel> citizens = new ArrayList<>();

	@Relationship(value = "EMPLOYEE")
	private List<JobRelationship> cityEmployees = new ArrayList<>();

	private String name;

	@Property("exotic.property")
	private String exoticProperty;

	@CompositeProperty
	private Map<String, String> compositeProperty;

	public CityModel() {
	}

	public UUID getCityId() {
		return this.cityId;
	}

	public PersonModel getMayor() {
		return this.mayor;
	}

	public List<PersonModel> getCitizens() {
		return this.citizens;
	}

	public List<JobRelationship> getCityEmployees() {
		return this.cityEmployees;
	}

	public String getName() {
		return this.name;
	}

	public String getExoticProperty() {
		return this.exoticProperty;
	}

	public void setCityId(UUID cityId) {
		this.cityId = cityId;
	}

	public void setMayor(PersonModel mayor) {
		this.mayor = mayor;
	}

	public void setCitizens(List<PersonModel> citizens) {
		this.citizens = citizens;
	}

	public void setCityEmployees(List<JobRelationship> cityEmployees) {
		this.cityEmployees = cityEmployees;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setExoticProperty(String exoticProperty) {
		this.exoticProperty = exoticProperty;
	}

	public Map<String, String> getCompositeProperty() {
		return compositeProperty;
	}

	public void setCompositeProperty(Map<String, String> compositeProperty) {
		this.compositeProperty = compositeProperty;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof CityModel)) {
			return false;
		}
		final CityModel other = (CityModel) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$cityId = this.getCityId();
		final Object other$cityId = other.getCityId();
		if (this$cityId == null ? other$cityId != null : !this$cityId.equals(other$cityId)) {
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
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof CityModel;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $cityId = this.getCityId();
		result = result * PRIME + ($cityId == null ? 43 : $cityId.hashCode());
		final Object $mayor = this.getMayor();
		result = result * PRIME + ($mayor == null ? 43 : $mayor.hashCode());
		final Object $citizens = this.getCitizens();
		result = result * PRIME + ($citizens == null ? 43 : $citizens.hashCode());
		final Object $cityEmployees = this.getCityEmployees();
		result = result * PRIME + ($cityEmployees == null ? 43 : $cityEmployees.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $exoticProperty = this.getExoticProperty();
		result = result * PRIME + ($exoticProperty == null ? 43 : $exoticProperty.hashCode());
		return result;
	}

	public String toString() {
		return "CityModel(cityId=" + this.getCityId() + ", mayor=" + this.getMayor() + ", citizens=" + this.getCitizens() + ", cityEmployees=" + this.getCityEmployees() + ", name=" + this.getName() + ", exoticProperty=" + this.getExoticProperty() + ")";
	}
}
