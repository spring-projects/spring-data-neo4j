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
package org.springframework.data.neo4j.core.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.convert.ConverterBuilder;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.types.CartesianPoint2d;
import org.springframework.data.neo4j.types.CartesianPoint3d;
import org.springframework.data.neo4j.types.Coordinate;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.neo4j.types.GeographicPoint3d;
import org.springframework.data.neo4j.types.Neo4jPoint;
import org.springframework.data.neo4j.types.PointBuilder;
import org.springframework.util.Assert;

/**
 * Mapping of spatial types.
 * <p>
 * This replicates the behaviour of SDN+OGM. Spring Data Commons geographic points are x/y based and usually treat x/y
 * as lat/long.
 * <p>
 * Neo4j however stores x/y as long/lat when used with an Srid of 4326 or 4979 (those are geographic points). We take
 * this into account with our dedicated spatial types which can be used alternatively.
 * <p>
 * However, when converting an Spring Data Commons point to the internal value, you'll notice that we store y as x and
 * vice versa. This is intentionally. We use a hardcoded WGS-84 Srid during storage, thus you'll get back your x as
 * latitude, y as longitude, as described above.
 * <p>
 * The biggest degree of freedom will come from using an attribute of type {@link org.neo4j.driver.types.Point}
 * directly. This will be passed on as is.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class SpatialTypes {

	static final List<?> CONVERTERS;

	static {

		List<ConverterBuilder.ConverterAware> hlp = new ArrayList<>();
		hlp.add(ConverterBuilder.reading(Value.class, Point.class, SpatialTypes::asSpringDataPoint).andWriting(SpatialTypes::value));
		hlp.add(ConverterBuilder.reading(Value.class, Point[].class, SpatialTypes::asPointArray).andWriting(SpatialTypes::value));

		hlp.add(ConverterBuilder.reading(Value.class, Neo4jPoint.class, SpatialTypes::asNeo4jPoint).andWriting(SpatialTypes::value));

		CONVERTERS = Collections.unmodifiableList(hlp);
	}

	static Neo4jPoint asNeo4jPoint(Value value) {

		org.neo4j.driver.types.Point point = value.asPoint();

		Coordinate coordinate = new Coordinate(point.x(), point.y(), Double.isNaN(point.z()) ? null : point.z());
		return PointBuilder.withSrid(point.srid()).build(coordinate);
	}

	static Value value(Neo4jPoint object) {

		if (object instanceof CartesianPoint2d) {
			CartesianPoint2d point = (CartesianPoint2d) object;
			return Values.point(point.getSrid(), point.getX(), point.getY());
		} else if (object instanceof CartesianPoint3d) {
			CartesianPoint3d point = (CartesianPoint3d) object;
			return Values.point(point.getSrid(), point.getX(), point.getY(), point.getZ());
		} else if (object instanceof GeographicPoint2d) {
			GeographicPoint2d point = (GeographicPoint2d) object;
			return Values.point(point.getSrid(), point.getLongitude(), point.getLatitude());
		} else if (object instanceof GeographicPoint3d) {
			GeographicPoint3d point = (GeographicPoint3d) object;
			return Values.point(point.getSrid(), point.getLongitude(), point.getLatitude(), point.getHeight());
		} else {
			throw new IllegalArgumentException("Unsupported point implementation: " + object.getClass());
		}
	}

	static Point asSpringDataPoint(Value value) {

		org.neo4j.driver.types.Point point = value.asPoint();
		Assert.isTrue(point.srid() == 4326, "Srid must be 4326");

		return new Point(point.y(), point.x());
	}

	static Value value(Point point) {
		return Values.point(4326, point.getY(), point.getX());
	}

	static Point[] asPointArray(Value value) {
		Point[] array = new Point[value.size()];
		int i = 0;
		for (Point v : value.values(SpatialTypes::asSpringDataPoint)) {
			array[i++] = v;
		}
		return array;
	}

	static Value value(Point[] aPointArray) {
		if (aPointArray == null) {
			return Values.NULL;
		}

		Value[] values = new Value[aPointArray.length];
		int i = 0;
		for (Point v : aPointArray) {
			values[i++] = value(v);
		}

		return Values.value(values);
	}

	private SpatialTypes() {}
}
