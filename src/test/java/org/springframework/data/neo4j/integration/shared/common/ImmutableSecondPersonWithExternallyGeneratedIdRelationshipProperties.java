/*
 * Copyright 2011-2025 the original author or authors.
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

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Gerrit Meier
 */
@RelationshipProperties
public class ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties {

	@RelationshipId
	public final Long id;

	public final String name;

	@TargetNode
	public final ImmutableSecondPersonWithExternallyGeneratedId target;

	public ImmutableSecondPersonWithExternallyGeneratedIdRelationshipProperties(Long id, String name, ImmutableSecondPersonWithExternallyGeneratedId target) {
		this.id = id;
		this.name = name;
		this.target = target;
	}
}
