/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.mapping;

import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.graph.core.Direction;
import org.springframework.util.Assert;

/**
 * {@link RelationshipInfo} implementation gathering data from a {@link RelatedTo} annotation.
 * 
 * @author Oliver Gierke
 */
class AnnotationBasedRelationshipInfo implements RelationshipInfo {

	private final RelatedTo annotation;

	/**
	 * Creates a new {@link AnnotationBasedRelationshipInfo} from the given {@link RelatedTo} annotation.
	 * 
	 * @param annotation must not be {@literal null}.
	 */
	public AnnotationBasedRelationshipInfo(RelatedTo annotation) {
		Assert.notNull(annotation);
		this.annotation = annotation;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.graph.neo4j.mapping.RelationshipInfo#getDirection()
	 */
	@Override
	public Direction getDirection() {
		return annotation.direction();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.graph.neo4j.mapping.RelationshipInfo#getType()
	 */
	@Override
	public String getType() {
		return annotation.type();
	}
}
