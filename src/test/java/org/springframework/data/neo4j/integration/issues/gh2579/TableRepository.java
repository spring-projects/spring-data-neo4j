/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2579;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Michael J. Simons
 */
public interface TableRepository extends Neo4jRepository<TableNode, Long> {

	@Query("""
			UNWIND :#{#froms} AS col
			WITH col.__properties__ AS col, :#{#to}.__properties__ AS to
			MERGE (c:Column {
				sourceName: col.sourceName,
				schemaName: col.schemaName,
				tableName: col.tableName,
				name: col.name
			})
			MERGE (t:Table {
				sourceName: to.sourceName,
				schemaName: to.schemaName,
				name: to.name
			})
			MERGE (c) -[r:BELONG]-> (t)""")
	void mergeTableAndColumnRelations(@Param("froms") List<ColumnNode> froms, @Param("to") TableNode to);

}
