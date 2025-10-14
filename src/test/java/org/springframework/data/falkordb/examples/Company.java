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
package org.springframework.data.falkordb.examples;

import java.util.List;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Example Company entity demonstrating FalkorDB node mapping.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node("Company")
public class Company {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String industry;

	private int foundedYear;

	@Property("employee_count")
	private int employeeCount;

	@Relationship(type = "EMPLOYS", direction = Relationship.Direction.INCOMING)
	private List<Person> employees;

	// Default constructor
	public Company() {
	}

	public Company(String name, String industry) {
		this.name = name;
		this.industry = industry;
	}

	// Getters and setters

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIndustry() {
		return this.industry;
	}

	public void setIndustry(String industry) {
		this.industry = industry;
	}

	public int getFoundedYear() {
		return this.foundedYear;
	}

	public void setFoundedYear(int foundedYear) {
		this.foundedYear = foundedYear;
	}

	public int getEmployeeCount() {
		return this.employeeCount;
	}

	public void setEmployeeCount(int employeeCount) {
		this.employeeCount = employeeCount;
	}

	public List<Person> getEmployees() {
		return this.employees;
	}

	public void setEmployees(List<Person> employees) {
		this.employees = employees;
	}

	@Override
	public String toString() {
		return "Company{" + "id=" + this.id + ", name='" + this.name + '\'' + ", industry='" + this.industry + '\''
				+ ", foundedYear=" + this.foundedYear + ", employeeCount=" + this.employeeCount + '}';
	}

}
