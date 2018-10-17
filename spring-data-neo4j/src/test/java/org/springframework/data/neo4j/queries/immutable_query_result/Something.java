package org.springframework.data.neo4j.queries.immutable_query_result;

public class Something {
	private final String name;

	public Something(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
