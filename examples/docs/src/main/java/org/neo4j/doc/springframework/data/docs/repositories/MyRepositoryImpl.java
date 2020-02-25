package org.neo4j.doc.springframework.data.docs.repositories;

import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.repository.query.CypherAdapterUtils;
import org.neo4j.springframework.data.repository.support.Neo4jEntityInformation;
import org.neo4j.springframework.data.repository.support.SimpleNeo4jRepository;

public class MyRepositoryImpl <T, ID> {
	/*
	extends SimpleNeo4jRepository<T, ID> {

	private final Neo4jClient neo4jClient;

	public MyRepositoryImpl(
		Neo4jEntityInformation<T, ID> entityInformation,
		Neo4jClient neo4jClient,
		CypherAdapterUtils.SchemaBasedStatementBuilder statementBuilder,
		Neo4jEvents eventSupport,
		Neo4jMappingContext neo4jMappingContext,
		Neo4jClient neo4jClient1) {
		super(entityInformation, neo4jClient, statementBuilder, eventSupport, neo4jMappingContext);
		this.neo4jClient = neo4jClient1;
	}

	@Transactional
	public <S extends T> S save(S entity) {
		// implementation goes here
	}

	 */
}
