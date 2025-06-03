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
package org.springframework.data.neo4j.types;

import org.apiguardian.api.API;

/**
 * A builder for points, so that coordinates and optionally a coordinate system can be
 * configured. Dependening on the coordinate system, {@link GeographicPoint2d} or
 * {@link GeographicPoint3d} will be created, cartesian points otherwise.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class PointBuilder {

	private final int srid;

	private PointBuilder(int srid) {
		this.srid = srid;
	}

	public static PointBuilder withSrid(int srid) {
		return new PointBuilder(srid);
	}

	public Neo4jPoint build(Coordinate coordinate) {

		boolean is3d = coordinate.getZ() != null;

		if (this.srid == CartesianPoint2d.SRID || this.srid == CartesianPoint3d.SRID) {
			return is3d ? new CartesianPoint3d(coordinate) : new CartesianPoint2d(coordinate);
		}
		else {
			return is3d ? new GeographicPoint3d(coordinate, this.srid) : new GeographicPoint2d(coordinate, this.srid);
		}
	}

}
