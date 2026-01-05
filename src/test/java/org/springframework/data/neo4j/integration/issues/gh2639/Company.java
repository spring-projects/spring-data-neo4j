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
package org.springframework.data.neo4j.integration.issues.gh2639;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Root node
 */
@Node
public class Company {

	private final String name;

	@Relationship(type = "EMPLOYEE")
	private final List<CompanyPerson> employees;

	@Id
	@GeneratedValue
	private Long id;

	public Company(String name, List<CompanyPerson> employees) {
		this.name = name;
		this.employees = employees;
	}

	public void addEmployee(CompanyPerson person) {
		this.employees.add(person);
	}

	public List<CompanyPerson> getEmployees() {
		return this.employees;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", Company.class.getSimpleName() + "[", "]").add("id=" + this.id)
			.add("name='" + this.name + "'")
			.add("employees=" + this.employees)
			.toString();
	}

}
