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

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Example entity demonstrating FalkorDB object mapping annotations. Shows how to
 * use @Node, @Id, @Property, and @Relationship annotations.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node(labels = { "Person", "Individual" })
public class Person {

	@Id
	@GeneratedValue
	private Long id;

	@Property("full_name")
	private String name;

	private String email;

	private LocalDate birthDate;

	private int age;

	@Relationship(type = "KNOWS", direction = Relationship.Direction.OUTGOING)
	private List<Person> friends;

	@Relationship(type = "WORKS_FOR", direction = Relationship.Direction.OUTGOING)
	private Company company;

	@Relationship(type = "LIVES_IN", direction = Relationship.Direction.OUTGOING)
	private Address address;

	// Default constructor for object mapping
	public Person() {
	}

	public Person(String name, String email) {
		this.name = name;
		this.email = email;
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

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDate getBirthDate() {
		return this.birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public List<Person> getFriends() {
		return this.friends;
	}

	public void setFriends(List<Person> friends) {
		this.friends = friends;
	}

	public Company getCompany() {
		return this.company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public Address getAddress() {
		return this.address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Person{" + "id=" + this.id + ", name='" + this.name + '\'' + ", email='" + this.email + '\'' + ", age="
				+ this.age + '}';
	}

}
