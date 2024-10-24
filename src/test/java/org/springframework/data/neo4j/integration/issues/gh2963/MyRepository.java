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
package org.springframework.data.neo4j.integration.issues.gh2963;

import java.util.Optional;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * @author Andreas Rümpel
 * @author Michael J. Simons
 */
public interface MyRepository extends CrudRepository<MyModel, String> {

    @Query("""
            MATCH (root:MyModel {uuid: $uuid})
            RETURN root {
                     .*, MyModel_REL_TO_MY_NESTED_MODEL_MyModel: [
                       (root)-[:REL_TO_MY_NESTED_MODEL]->(nested:MyModel) | nested {. *}
                     ]
                   }
            """)
    Optional<MyModel> getByUuidCustomQuery(String uuid);

	@Query("""
            MATCH (root:MyModel {uuid: $uuid})
            RETURN root {
                     .*, MyModel_REL_TO_MY_NESTED_MODEL_MyModel_true: [
                       (root)-[:REL_TO_MY_NESTED_MODEL]->(nested:MyModel) | nested {. *}
                     ]
                   }
            """)
	Optional<MyModel> getByUuidCustomQueryV2(String uuid);
}
