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
package org.springframework.data.neo4j.integration.issues.gh2289;

import lombok.Data;

import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Michael J. Simons
 */
@Data // lombok
@RelationshipProperties
public class RangeRelation {
	@RelationshipId
	private Long id;

	@Property private double minDelta;
	@Property private double maxDelta;
	@Property private RelationType relationType;

	@TargetNode private Sku targetSku;

	public RangeRelation(Sku targetSku, double minDelta, double maxDelta, RelationType relationType) {
		this.targetSku = targetSku;
		this.minDelta = minDelta;
		this.maxDelta = maxDelta;
		this.relationType = relationType;
	}
}
