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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import kotlin.reflect.jvm.internal.ReflectProperties;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.InternalPoint2D;
import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.internal.value.PointValue;
import org.neo4j.driver.types.Point;
import org.springframework.data.neo4j.types.CartesianPoint2d;
import org.springframework.data.neo4j.types.CartesianPoint3d;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.neo4j.types.GeographicPoint3d;

/**
 * @author Michael J. Simons
 */
class SpatialTypesTest {

	@Test
	void neo4jPointAsValueShouldWork() {

		Point point;

		point = SpatialTypes.value(new GeographicPoint2d(10, 20)).asPoint();
		assertThat(point.srid()).isEqualTo(4326);
		assertThat(point.x()).isEqualTo(20.0);
		assertThat(point.y()).isEqualTo(10.0);

		point = SpatialTypes.value(new CartesianPoint2d(10, 20)).asPoint();
		assertThat(point.srid()).isEqualTo(7203);
		assertThat(point.x()).isEqualTo(10.0);
		assertThat(point.y()).isEqualTo(20.0);

		point = SpatialTypes.value(new GeographicPoint3d(10.0, 20.0, 30)).asPoint();
		assertThat(point.srid()).isEqualTo(4979);
		assertThat(point.x()).isEqualTo(20.0);
		assertThat(point.y()).isEqualTo(10.0);
		assertThat(point.z()).isEqualTo(30.0);

		point = SpatialTypes.value(new CartesianPoint3d(10.0, 20.0, 30)).asPoint();
		assertThat(point.srid()).isEqualTo(9157);
		assertThat(point.x()).isEqualTo(10.0);
		assertThat(point.y()).isEqualTo(20.0);
		assertThat(point.z()).isEqualTo(30.0);
	}

	/**
	 * @author Shivang Patel
	 */
	@Test
	void valueReturnsArray(){
		org.springframework.data.geo.Point point1 = new org.springframework.data.geo.Point(12, 45);
		org.springframework.data.geo.Point point2 = new org.springframework.data.geo.Point(65, 23);
		org.springframework.data.geo.Point point3 = new org.springframework.data.geo.Point(76, 23);
		org.springframework.data.geo.Point point4 = new org.springframework.data.geo.Point(32, 45);

		org.springframework.data.geo.Point[] points = new org.springframework.data.geo.Point[]{point1, point2, point3, point4};

		Value value1 = new PointValue( new InternalPoint2D(4326, point1.getY(), point1.getX()));
		Value value2 = new PointValue( new InternalPoint2D(4326, point2.getY(), point2.getX()));
		Value value3 = new PointValue( new InternalPoint2D(4326, point3.getY(), point3.getX()));
		Value value4 = new PointValue( new InternalPoint2D(4326, point4.getY(), point4.getX()));

		Value[] values = new Value[]{value1, value2, value3, value4};

		ListValue listValue = new ListValue(values);

		assertEquals(SpatialTypes.value(points), listValue);
	}
}
