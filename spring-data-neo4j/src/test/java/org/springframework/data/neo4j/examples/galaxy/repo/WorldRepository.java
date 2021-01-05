/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.examples.galaxy.repo;

import org.neo4j.ogm.model.Result;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
@Repository
public interface WorldRepository extends Neo4jRepository<World, Long> {

	@Query("MATCH (n:World) SET n.updated=timestamp()")
	void touchAllWorlds();

	@Query("MATCH (n:World) SET n.updated=timestamp()")
	Result touchAllWorldsWithStatistics();

	World findByName(String name);

}
