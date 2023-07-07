/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2289;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael J. Simons
 */
@Node("SKU")
public class Sku {

	@Id
	@GeneratedValue
	private Long id;

	@Property("number")
	private Long number;

	@Property("name")
	private String name;

	@Relationship(type = "RANGE_RELATION_TO", direction = Relationship.Direction.OUTGOING)
	private Set<RangeRelation> rangeRelationsOut = new HashSet<>();

	@Relationship(type = "RANGE_RELATION_TO", direction = Relationship.Direction.INCOMING)
	private Set<RangeRelation> rangeRelationsIn = new HashSet<>();

	public Sku(Long number, String name) {
		this.number = number;
		this.name = name;
	}

	public RangeRelation rangeRelationTo(Sku sku, double minDelta, double maxDelta, RelationType relationType) {
		RangeRelation relationOut = new RangeRelation(sku, minDelta, maxDelta, relationType);
		RangeRelation relationIn = new RangeRelation(this, minDelta, maxDelta, relationType);
		rangeRelationsOut.add(relationOut);
		sku.rangeRelationsIn.add(relationIn);
		return relationOut;
	}

	@Override
	public String toString() {
		return "Sku{" +
				"id=" + id +
				", number=" + number +
				", name='" + name +
				'}';
	}

	public Long getId() {
		return this.id;
	}

	public Long getNumber() {
		return this.number;
	}

	public String getName() {
		return this.name;
	}

	public Set<RangeRelation> getRangeRelationsOut() {
		return this.rangeRelationsOut;
	}

	public Set<RangeRelation> getRangeRelationsIn() {
		return this.rangeRelationsIn;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRangeRelationsOut(Set<RangeRelation> rangeRelationsOut) {
		this.rangeRelationsOut = rangeRelationsOut;
	}

	public void setRangeRelationsIn(Set<RangeRelation> rangeRelationsIn) {
		this.rangeRelationsIn = rangeRelationsIn;
	}
}
