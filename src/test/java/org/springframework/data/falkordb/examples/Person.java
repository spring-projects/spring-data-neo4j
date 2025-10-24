/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
