/*
 * Copyright 2011-present the original author or authors.
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

import java.util.Objects;

import org.apiguardian.api.API;

/**
 * A concrete, 3-dimensional cartesian point.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class CartesianPoint3d extends AbstractPoint {

	static final int SRID = 9157;

	CartesianPoint3d(Coordinate coordinate) {
		super(coordinate, SRID);
	}

	public CartesianPoint3d(double x, double y, double z) {
		super(new Coordinate(x, y, z), SRID);
	}

	public double getX() {
		return this.coordinate.getX();
	}

	public double getY() {
		return this.coordinate.getY();
	}

	public Double getZ() {
		return Objects.requireNonNull(this.coordinate.getZ(),
				"The underlying coordinate does not have a z-value (height)");
	}

	@Override
	public String toString() {
		return "CartesianPoint3d{" + "x=" + getX() + ", y=" + getY() + ", z=" + getZ() + '}';
	}

}
