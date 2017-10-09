package org.springframework.data.neo4j.namedquery.repo;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.namedquery.domain.SampleEntityForNamedQuery;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;

public interface SampleEntityForNamedQueryRepository extends Neo4jRepository<SampleEntityForNamedQuery, Long> {

	@Query("MATCH (e) WHERE e.name='test' RETURN e")
	SampleEntityForNamedQuery getTitleEntity();

	SampleEntityForNamedQuery findByName(String name);

	SampleEntityForNamedQuery findByQueryWithoutParameter();

	SampleEntityForNamedQuery findByQueryWithParameter(@Param("name") String name);

}
