/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.neo4j.springframework.data.integration.shared;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.types.CartesianPoint2d;
import org.neo4j.springframework.data.types.CartesianPoint3d;
import org.neo4j.springframework.data.types.GeographicPoint2d;
import org.neo4j.springframework.data.types.GeographicPoint3d;
import org.springframework.data.geo.Point;

/**
 * Contains properties of all spatial types.
 *
 * @author Michael J. Simons
 */
@Node("SpatialTypes")
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ThingWithAllSpatialTypes {

	@Id @GeneratedValue
	@With
	public final Long id;

	private Point sdnPoint;

	private GeographicPoint2d geo2d;

	private GeographicPoint3d geo3d;

	private CartesianPoint2d car2d;

	private CartesianPoint3d car3d;
}
