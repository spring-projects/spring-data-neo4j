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

import java.util.Objects;

import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 */
public final class Coordinate {
	private final double x;

	private final double y;

	private final Double z;

	public Coordinate(double x, double y) {
		this(x, y, null);
	}

	public Coordinate(double x, double y, @Nullable Double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	double getX() {
		return x;
	}

	double getY() {
		return y;
	}

	@Nullable Double getZ() {
		return z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Coordinate)) {
			return false;
		}
		Coordinate that = (Coordinate) o;
		return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Objects.equals(z, that.z);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}
}
