package org.springframework.datastore.graph.neo4j;


import org.springframework.datastore.graph.api.GraphRelationship;
import org.springframework.datastore.graph.api.GraphRelationshipEndNode;
import org.springframework.datastore.graph.api.GraphRelationshipStartNode;

@GraphRelationship
public class Friendship {
	
	public Friendship() {
	}
	
	public Friendship(Person p1, Person p2) {
		this.p1 = p1;
		this.p2 = p2;
	}
	
	public Friendship(Person p1, Person p2, int years) {
		this.p1 = p1;
		this.p2 = p2;
		this.years = years;
	}

	@GraphRelationshipStartNode
	private Person p1;
	
	@GraphRelationshipEndNode
	private Person p2;
	
	private int years;
	
	public void setPerson1(Person p) {
		p1 = p;
	}
	
	public Person getPerson1() {
		return p1;
	}
	
	public void setPerson2(Person p) {
		p2 = p;
	}
	
	public Person getPerson2() {
		return p2;
	}
	
	public void setYears(int years) {
		this.years = years;
	}

	public int getYears() {
		return years;
	}
}
