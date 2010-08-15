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

	private Short height;

	Person spouse;
	
	@Relationship(type="mother", direction=Direction.OUTGOING)
	Person mother;
	
	@Relationship(type="boss", direction=Direction.INCOMING)
	Person boss;
	
	@Relationship(type="friend", direction=Direction.BOTH)
	Person friend;

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
	
	public Short getHeight() {
		return height;
	}

	public void setHeight(Short height) {
		this.height = height;
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
	
	public Person getBoss() {
		return boss;
	}
	public void setBoss(Person boss) {
		this.boss = boss;
	}

	public Person getFriend() {
		return friend;
	}

	public void setFriend(Person friend) {
		this.friend = friend;
	}
	@Override
	public String toString() {
		return name;
	}
}
