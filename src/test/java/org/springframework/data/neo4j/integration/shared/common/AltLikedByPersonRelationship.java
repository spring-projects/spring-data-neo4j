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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.Objects;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Michael J. Simons
 */
@RelationshipProperties
public class AltLikedByPersonRelationship {

	@RelationshipId
	private Long id;

	private Integer rating;

	@TargetNode
	private AltPerson altPerson;

	public Integer getRating() {
		return this.rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public AltPerson getAltPerson() {
		return this.altPerson;
	}

	public void setAltPerson(AltPerson altPerson) {
		this.altPerson = altPerson;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AltLikedByPersonRelationship that = (AltLikedByPersonRelationship) o;
		return this.rating.equals(that.rating) && this.altPerson.equals(that.altPerson);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.rating, this.altPerson);
	}

	@Override
	public String toString() {
		return "AltLikedByPersonRelationship{" + "rating=" + this.rating + ", altPerson=" + this.altPerson.getName()
				+ '}';
	}

}
