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
package org.springframework.data.neo4j.types;

import org.apiguardian.api.API;

/**
 * A dedicated Neo4j point, that is aware of its nature, either being geographic or cartesian. While you can use this
 * interface as an attribute type in your domain class, you should not mix different type of points on the same
 * attribute of the same label. Queries will lead to inconsistent results. Use one of the concrete implementations. See
 * <a href="https://neo4j.com/docs/cypher-manual/current/syntax/spatial/#cypher-spatial">Spatial values</a>.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPoint {

	/**
	 * @return The Srid identifying the Coordinate Reference Systems (CRS) used by this point.
	 */
	Integer getSrid();
}
