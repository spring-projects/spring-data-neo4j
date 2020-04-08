/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

/**
 * @author Michael J. Simons
 */
class BoundingBoxTest {

	@ParameterizedTest
	@MethodSource("polygonsToTest")
	void builderShouldWorkForPolygons(Polygon p, Point ll, Point ur) {

		BoundingBox boundingBox = BoundingBox.of(p);
		assertThat(boundingBox.getLowerLeft()).isEqualTo(ll);
		assertThat(boundingBox.getUpperRight()).isEqualTo(ur);
	}

	@ParameterizedTest
	@MethodSource("boxesToTest")
	void builderShouldWorkForBoxes(Box b, Point ll, Point ur) {

		BoundingBox boundingBox = BoundingBox.of(b);
		assertThat(boundingBox.getLowerLeft()).isEqualTo(ll);
		assertThat(boundingBox.getUpperRight()).isEqualTo(ur);
	}

	private static Stream<Arguments> polygonsToTest() {
		return Stream.of(
			Arguments.of(
				new Polygon(new Point(1, 1), new Point(5, 1), new Point(5, 5), new Point(5, 1)),
				new Point(1, 1), new Point(5, 5)
			),
			Arguments.of(
				new Polygon(new Point(3, 6), new Point(6, 2), new Point(8, 3), new Point(8, 6), new Point(2, 9)),
				new Point(2, 2), new Point(8, 9)
			),
			Arguments.of(
				new Polygon(new Point(3, 4), new Point(7, 1), new Point(9, 4), new Point(10, 8), new Point(8, 10)),
				new Point(3, 1), new Point(10, 10)
			)
		);
	}

	private static Stream<Arguments> boxesToTest() {
		return Stream.of(
			Arguments.of(
				new Box(new Point(1, 1), new Point(5, 5)),
				new Point(1, 1), new Point(5, 5)
			),
			Arguments.of(
				new Box(new Point(8, 3), new Point(2, 9)),
				new Point(2, 3), new Point(8, 9)
			),
			Arguments.of(
				new Box(new Point(3, 4), new Point(10, 8)),
				new Point(3, 4), new Point(10, 8)
			)
		);
	}
}
