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
package org.springframework.data.neo4j.core.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
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
}
