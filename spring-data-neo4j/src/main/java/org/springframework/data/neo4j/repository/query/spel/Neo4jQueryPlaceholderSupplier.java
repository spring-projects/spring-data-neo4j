package org.springframework.data.neo4j.repository.query.spel;

public class Neo4jQueryPlaceholderSupplier implements PlaceholderSupplier {

	private static final String PLACEHOLDER = "spel_expression";
	private int index = 0;

	@Override
	public String nextPlaceholder() {
		return PLACEHOLDER + index++;
	}

	@Override
	public String decoratedPlaceholder(String placeholder) {
		return "{" + placeholder + "}";
	}
}
