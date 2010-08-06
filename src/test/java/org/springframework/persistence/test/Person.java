package org.springframework.persistence.test;

import org.springframework.persistence.graph.Direction;
import org.springframework.persistence.graph.GraphEntity;
import org.springframework.persistence.graph.Relationship;

@GraphEntity
public class Person {
	
	private Long id;
	
	private String name;
	
	private int age;

	Person spouse;
	
	@Relationship(type="mother",direction=Direction.BOTH)
	Person mother;

	public Person(String name, int age) {
		this.name = name;
		this.age = age;
	}
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Person getSpouse() {
		return spouse;
	}


	public void setSpouse(Person spouse) {
		this.spouse = spouse;
	}


	public Person getMother() {
		return mother;
	}


	public void setMother(Person mother) {
		this.mother = mother;
	}
}
