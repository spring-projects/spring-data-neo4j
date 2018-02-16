package org.springframework.data.neo4j.repository.query.spel;

interface PlaceholderSupplier {
	String nextPlaceholder();

	String decoratedPlaceholder(String placeholder);
}
