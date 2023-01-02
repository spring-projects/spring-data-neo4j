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
package org.springframework.data.neo4j.integration.issues.gh2526;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;

import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Second type of relationship
 */
@RelationshipProperties
@Value
@With
@AllArgsConstructor
@EqualsAndHashCode
@Immutable
public class Variable {
	@RelationshipId
	Long id;

	@TargetNode
	MeasurementMeta measurement;

	String variable;

	public static Variable create(MeasurementMeta measurement, String variable) {
		return new Variable(null, measurement, variable);
	}

	@Override
	public String toString() {
		return variable + ": " + measurement.getNodeId();
	}
}
