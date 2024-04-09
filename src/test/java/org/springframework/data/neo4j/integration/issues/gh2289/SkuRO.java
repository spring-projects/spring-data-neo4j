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
package org.springframework.data.neo4j.integration.issues.gh2289;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node("SKU_RO")
@Getter // lombok
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SkuRO {

	@Id @GeneratedValue
	@EqualsAndHashCode.Include
	private Long id;

	@Property("number")
	@EqualsAndHashCode.Include
	private Long number;

	@Property(value = "name", readOnly = true)
	@EqualsAndHashCode.Include
	private String name;

	@Relationship(type = "RANGE_RELATION_TO", direction = Relationship.Direction.OUTGOING)
	private Set<RangeRelationRO> rangeRelationsOut = new HashSet<>();

	@ReadOnlyProperty
	@Relationship(type = "RANGE_RELATION_TO", direction = Relationship.Direction.INCOMING)
	private Set<RangeRelationRO> rangeRelationsIn = new HashSet<>();

	@CompositeProperty
	private Map<String, Integer> composite;

	public SkuRO(Long number, String name) {
		this.number = number;
		this.name = name;
	}

	public RangeRelationRO rangeRelationTo(SkuRO sku, double minDelta, double maxDelta, RelationType relationType) {
		RangeRelationRO relationOut = new RangeRelationRO(sku, minDelta, maxDelta, relationType);
		rangeRelationsOut.add(relationOut);
		return relationOut;
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

	public Set<RangeRelationRO> getRangeRelationsOut() {
		return this.rangeRelationsOut;
	}

	public Set<RangeRelationRO> getRangeRelationsIn() {
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

	public void setRangeRelationsOut(Set<RangeRelationRO> rangeRelationsOut) {
		this.rangeRelationsOut = rangeRelationsOut;
	}

	public void setRangeRelationsIn(Set<RangeRelationRO> rangeRelationsIn) {
		this.rangeRelationsIn = rangeRelationsIn;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof SkuRO)) {
			return false;
		}
		final SkuRO other = (SkuRO) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$number = this.getNumber();
		final Object other$number = other.getNumber();
		if (this$number == null ? other$number != null : !this$number.equals(other$number)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof SkuRO;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $number = this.getNumber();
		result = result * PRIME + ($number == null ? 43 : $number.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		return result;
	}
}
