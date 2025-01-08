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
package org.springframework.data.neo4j.integration.issues.gh2906;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Mathias Kühn
 * @param <T> The crux of this thing
 */
@RelationshipProperties
public abstract class BugRelationship<T>  {

	@RelationshipId
	public Long id;

	public String comment;

	@TargetNode
	public T target;

	BugRelationship(String comment, T target) {
		this.comment = comment;
		this.target = target;
	}

	@Override
	public String toString() {
		return String.format("<%s> {id: %d, comment: %s}", this.getClass().getSimpleName(), id, comment);
	}

}
