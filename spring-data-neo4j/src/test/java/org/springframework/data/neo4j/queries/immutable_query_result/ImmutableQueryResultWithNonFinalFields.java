package org.springframework.data.neo4j.queries.immutable_query_result;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.annotation.QueryResult;

@QueryResult
public class ImmutableQueryResultWithNonFinalFields {
	private String name;

	private Long aNumber;

	public ImmutableQueryResultWithNonFinalFields(String name, Long aNumber) {
		this.name = "J" + name + "d";
		this.aNumber = 1 + aNumber;
	}

	public String getName() {
		return name;
	}

	public Long getaNumber() {
		return aNumber;
	}
}
