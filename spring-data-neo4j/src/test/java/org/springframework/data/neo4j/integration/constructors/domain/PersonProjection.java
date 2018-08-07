package org.springframework.data.neo4j.integration.constructors.domain;

import java.util.List;

import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * An arbitrary {@link QueryResult} used as a projection of a person.
 *
 * @author Michael J. Simons
 */
@QueryResult
public class PersonProjection {
	private String name;

	private long numberOfFriends;

	private List<Person> mutualFriends;

	public PersonProjection(String name,
			long numberOfFriends,
			List<Person> mutualFriends) {
		this.name = name;
		this.numberOfFriends = numberOfFriends;
		this.mutualFriends = mutualFriends;
	}

	public String getName() {
		return name;
	}

	public long getNumberOfFriends() {
		return numberOfFriends;
	}

	public List<Person> getMutualFriends() {
		return mutualFriends;
	}
}
