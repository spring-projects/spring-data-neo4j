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
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class GeographicPoint2d extends AbstractPoint {

	GeographicPoint2d(Coordinate coordinate, Integer srid) {
		super(coordinate, srid);
	}

	public GeographicPoint2d(double latitude, double longitude) {
		super(new Coordinate(longitude, latitude), 4326);
	}

	public double getLongitude() {
		return coordinate.getX();
	}

	public double getLatitude() {
		return coordinate.getY();
	}

	@Override
	public String toString() {
		return "GeographicPoint2d{" + "longitude=" + getLongitude() + ", latitude=" + getLatitude() + ", srid=" + getSrid()
				+ '}';
	}
}
