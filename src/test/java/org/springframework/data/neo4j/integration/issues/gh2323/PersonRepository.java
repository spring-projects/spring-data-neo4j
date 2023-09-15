/*
 * Copyright 2011-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.issues.gh2323;

import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Michael J. Simons
 */
@Repository
public interface PersonRepository extends Neo4jRepository<Person, String> {

	// Using separate id and then relationships on top level
	@Query("""
			UNWIND $relations As rel WITH rel
			MATCH (f:Person {id: $from})
			 MATCH (t:Language {name: rel.__target__.__id__})
			CREATE (f)- [r:KNOWS {description: rel.__properties__.description}] -> (t)
			RETURN f, collect(r), collect(t)
			"""
	)
	Person updateRel(@Param("from") String from, @Param("relations") List<Knows> relations);

	// Using the whole person object
	@Query("""
			UNWIND $person.__properties__.KNOWS As rel WITH rel
			MATCH (f:Person {id: $person.__id__})
			MATCH  (t:Language {name: rel.__target__.__id__})
			CREATE (f) - [r:KNOWS {description: rel.__properties__.description}] -> (t)
			RETURN f, collect(r), collect(t)
			"""
	)
	Person updateRel2(@Param("person") Person person);

	@Query("""
			MATCH (f:Person {id: $person.__id__})
			MATCH (mt:Language {name: $person.__properties__.HAS_MOTHER_TONGUE[0].__target__.__id__})
			MATCH (f)-[frl:HAS_MOTHER_TONGUE]->(mt) WITH f, frl, mt
			UNWIND $person.__properties__.KNOWS As rel WITH f, frl, mt, rel
			MATCH (t:Language {name: rel.__target__.__id__})
			MERGE (f)- [r:KNOWS {description: rel.__properties__.description}] -> (t)
			RETURN f, frl, mt, collect(r), collect(t)
			""")
	Person updateRelWith11(@Param("person") Person person);

	@Query("""
			UNWIND keys($relationships) as relationshipKey
			UNWIND $relationships[relationshipKey] as relationship
			MATCH (p:Person)-[:HAS_MOTHER_TONGUE]->(:Language{name: relationship.__target__.__id__})
			RETURN p
			   """)
	Person queryWithMapOfRelationship(@Param("relationships") Map<String, List<Knows>> relationshipList);
}
