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
package org.neo4j.springframework.data.integration.shared;

import java.time.LocalDate;
import java.util.Objects;

import org.neo4j.springframework.data.core.schema.RelationshipProperties;
import org.neo4j.springframework.data.types.CartesianPoint2d;

/**
 * @author Gerrit Meier
 */
@RelationshipProperties
public class LikesHobbyRelationship {

	private final Integer since;

	private Boolean active;

	// use some properties that require conversion
	// cypher type
	private LocalDate localDate;

	// additional type
	private MyEnum myEnum;

	// spatial type
	private CartesianPoint2d point;


	public LikesHobbyRelationship(Integer since) {
	 	this.since = since;
	 }

	public void setActive(Boolean active) {
		this.active = active;
	}

	public void setLocalDate(LocalDate localDate) {
		this.localDate = localDate;
	}

	public void setMyEnum(MyEnum myEnum) {
		this.myEnum = myEnum;
	}

	public void setPoint(CartesianPoint2d point) {
		this.point = point;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LikesHobbyRelationship that = (LikesHobbyRelationship) o;
		return since.equals(that.since) &&
			Objects.equals(active, that.active) &&
			Objects.equals(localDate, that.localDate) &&
			myEnum == that.myEnum &&
			Objects.equals(point, that.point);
	}

	@Override
	public int hashCode() {
		return Objects.hash(since, active, localDate, myEnum, point);
	}

	/**
	 * The missing javadoc
	 */
	public enum MyEnum {
		SOMETHING, SOMETHING_DIFFERENT
	}
}
