/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.queries.stored_procedures;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Michael J. Simons
 */
public interface DocumentRepository extends Neo4jRepository<DocumentEntity, Long> {
	@Query("CALL apoc.periodic.iterate('MATCH (d:Document) RETURN d', "
			+ "'SET d.thisIsAProperty = 0'"
			+ ", {batchSize:200, parallel:false, iterateList:true}) ")
	public void callApocProcedureAndIgnoreResult();
}
