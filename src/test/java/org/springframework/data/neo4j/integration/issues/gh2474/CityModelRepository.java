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
package org.springframework.data.neo4j.integration.issues.gh2474;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Stephen Jackson
 * @author Michael J. Simons
 */
public interface CityModelRepository extends Neo4jRepository<CityModel, UUID> {

	Optional<CityModelDTO> findByCityId(UUID cityId);

	@Query("" + "MATCH (n:CityModel)" + "RETURN n :#{orderBy(#sort)}")
	List<CityModel> customQuery(Sort sort);

	long deleteAllByExoticProperty(String property);

}
