/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2576;

import java.util.List;
import java.util.Map;

import org.neo4j.driver.Value;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
public interface CollegeRepository extends Neo4jRepository<College, String> {

	@Query("""
			UNWIND $0 AS row
			MATCH (student:Student{guid:row.stuGuid})
			MATCH (college:College{guid:row.collegeGuid})
			CREATE (student)<-[:STUDENT_OF]-(college) RETURN student.guid"""
	)
	List<String> addStudentToCollege(List<Map<String, String>> list);

	@Query("""
			UNWIND $0 AS row
			MATCH (student:Student{guid:row.stuGuid})
			MATCH (college:College{guid:row.collegeGuid})
			CREATE (student)<-[:STUDENT_OF]-(college) RETURN student.guid"""
	)
	List<String> addStudentToCollegeWorkaround(List<Value> list);
}
