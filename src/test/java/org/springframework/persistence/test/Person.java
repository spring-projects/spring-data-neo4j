package org.springframework.persistence.test;

import java.util.Collection;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Relationship;
import org.springframework.persistence.graph.Direction;
import org.springframework.persistence.graph.Graph;

@Graph.Entity
public class Person {
	
	private Long id;
	
	private String name;
	
	private int age;

	private Short height;

	private Person spouse;
	
	@Graph.Entity.Relationship(type = "mother", direction = Direction.OUTGOING)
	private Person mother;
	
	@Graph.Entity.Relationship(type = "boss", direction = Direction.INCOMING)
	private Person boss;

	@Graph.Entity.RelationshipEntity(type = "knows", elementClass = Friendship.class)
	private Iterable<Friendship> friendships;

	// @Property(serialize=SerializationPolicy.STRING, index=true, queryable=true, removeOnReset=true)
	// Date birthday;
	
	
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
		Relationship rel = this.getUnderlyingNode().createRelationshipTo(p.getUnderlyingNode(), DynamicRelationshipType.withName("knows"));
		Friendship friendship = new Friendship();
		friendship.setUnderlyingRelationship(rel);
		return friendship;
	}
}
