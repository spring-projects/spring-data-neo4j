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
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Stephen Jackson
 */
@Node
public class CityModel {

	@Id
	@GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
	private UUID cityId;

	@Relationship("MAYOR")
	private PersonModel mayor;

	@Relationship("CITIZEN")
	private List<PersonModel> citizens = new ArrayList<>();

	@Relationship("EMPLOYEE")
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

	public void setCityId(UUID cityId) {
		this.cityId = cityId;
	}

	public PersonModel getMayor() {
		return this.mayor;
	}

	public void setMayor(PersonModel mayor) {
		this.mayor = mayor;
	}

	public List<PersonModel> getCitizens() {
		return this.citizens;
	}

	public void setCitizens(List<PersonModel> citizens) {
		this.citizens = citizens;
	}

	public List<JobRelationship> getCityEmployees() {
		return this.cityEmployees;
	}

	public void setCityEmployees(List<JobRelationship> cityEmployees) {
		this.cityEmployees = cityEmployees;
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

	public Map<String, String> getCompositeProperty() {
		return this.compositeProperty;
	}

	public void setCompositeProperty(Map<String, String> compositeProperty) {
		this.compositeProperty = compositeProperty;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof CityModel;
	}

	@Override
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
		if (!Objects.equals(this$cityId, other$cityId)) {
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
		if (!Objects.equals(this$cityEmployees, other$cityEmployees)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (!Objects.equals(this$name, other$name)) {
			return false;
		}
		final Object this$exoticProperty = this.getExoticProperty();
		final Object other$exoticProperty = other.getExoticProperty();
		return Objects.equals(this$exoticProperty, other$exoticProperty);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $cityId = this.getCityId();
		result = result * PRIME + (($cityId != null) ? $cityId.hashCode() : 43);
		final Object $mayor = this.getMayor();
		result = result * PRIME + (($mayor != null) ? $mayor.hashCode() : 43);
		final Object $citizens = this.getCitizens();
		result = result * PRIME + (($citizens != null) ? $citizens.hashCode() : 43);
		final Object $cityEmployees = this.getCityEmployees();
		result = result * PRIME + (($cityEmployees != null) ? $cityEmployees.hashCode() : 43);
		final Object $name = this.getName();
		result = result * PRIME + (($name != null) ? $name.hashCode() : 43);
		final Object $exoticProperty = this.getExoticProperty();
		result = result * PRIME + (($exoticProperty != null) ? $exoticProperty.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "CityModel(cityId=" + this.getCityId() + ", mayor=" + this.getMayor() + ", citizens="
				+ this.getCitizens() + ", cityEmployees=" + this.getCityEmployees() + ", name=" + this.getName()
				+ ", exoticProperty=" + this.getExoticProperty() + ")";
	}

}
