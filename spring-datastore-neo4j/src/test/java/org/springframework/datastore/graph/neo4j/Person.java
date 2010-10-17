package org.springframework.datastore.graph.neo4j;

import org.springframework.datastore.graph.annotations.GraphEntityProperty;
import org.springframework.datastore.graph.annotations.NodeEntity;
import org.springframework.datastore.graph.annotations.NodeId;
import org.springframework.datastore.graph.annotations.Relationship;
import org.springframework.datastore.graph.annotations.RelationshipWithEntity;
import org.springframework.datastore.graph.api.*;
import java.util.Date;


@NodeEntity(useShortNames = false)
public class Person {

	@NodeId
	private Long graphId;

    @GraphEntityProperty(index = true)
	private String name;
	
	private int age;

	private Short height;
	
	private transient String thought;
	
	private Personality personality;
	
	private Date birthdate;

	private Person spouse;

	private Car car;
	
	@Relationship(type = "mother", direction = Direction.OUTGOING)
	private Person mother;
	
	@Relationship(type = "boss", direction = Direction.INCOMING)
	private Person boss;

	@RelationshipWithEntity(type = "knows", elementClass = Friendship.class)
	private Iterable<Friendship> friendships;

    public Person() {
    }

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
	
	@Override
	public String toString() {
		return name;
	}
	
	public Iterable<Friendship> getFriendships() {
		return friendships;
	}
	
	public void setFriendships(Iterable<Friendship> f) {
		friendships = f;
	}

	public Friendship knows(Person p) {
        return (Friendship)relateTo(p, Friendship.class,"knows");
	}
	
	public void setPersonality(Personality personality) {
		this.personality = personality;
	}
	
	public Personality getPersonality() {
		return personality;
	}
	
	public void setThought(String thought) {
		this.thought = thought;
	}
	
	public String getThought() {
		return thought;
	}

	public Date getBirthdate() {
		return birthdate;
	}

	public void setBirthdate(Date birthdate) {
		this.birthdate = birthdate;
	}

	public long getId() {
		return graphId;
	}

	public void setCar(Car car) {
		this.car = car;
	}

	public Car getCar() {
		return car;
	}
	
}
