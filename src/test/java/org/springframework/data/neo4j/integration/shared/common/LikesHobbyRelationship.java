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
package org.springframework.data.neo4j.integration.shared.common;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.types.CartesianPoint2d;

/**
 * @author Gerrit Meier
 */
@RelationshipProperties
public class LikesHobbyRelationship {

	@RelationshipId
	private Long id;

	private final Integer since;

	private Boolean active;

	// use some properties that require conversion
	// cypher type
	private LocalDate localDate;

	// additional type
	private MyEnum myEnum;

	// spatial type
	private CartesianPoint2d point;

	@TargetNode
	private Hobby hobby;

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
		return since.equals(that.since) && Objects.equals(active, that.active) && Objects.equals(localDate, that.localDate)
				&& myEnum == that.myEnum && Objects.equals(point, that.point);
	}

	@Override
	public int hashCode() {
		return Objects.hash(since, active, localDate, myEnum, point);
	}

	public Hobby getHobby() {
		return hobby;
	}

	public void setHobby(Hobby hobby) {
		this.hobby = hobby;
	}

	public Integer getSince() {
		return since;
	}

	/**
	 * The missing javadoc
	 */
	public enum MyEnum {
		SOMETHING, SOMETHING_DIFFERENT
	}
}
