/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.Set;

import org.springframework.data.annotation.ReadOnlyProperty;
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

	public SkuRO(Long number, String name) {
		this.number = number;
		this.name = name;
	}

	public RangeRelationRO rangeRelationTo(SkuRO sku, double minDelta, double maxDelta, RelationType relationType) {
		RangeRelationRO relationOut = new RangeRelationRO(sku, minDelta, maxDelta, relationType);
		rangeRelationsOut.add(relationOut);
		return relationOut;
	}
}
