package org.springframework.persistence.test;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

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

	// @Property(serialize=SerializationPolicy.STRING, index=true, queryable=true, removeOnReset=true)
	// Date birthday;
	
	/*
	{
		Field f;
		if (f.getGenericType() instanceof ParameterizedType) {
			((ParameterizedType)(f.getGenericType())).getActualTypeArguments();
		} 
	}
	@Relationship( target=Person.class, cardinality="", type="children",)
	Collection<Person> children;
	*/
	
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
