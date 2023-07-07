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
package org.springframework.data.neo4j.integration.issues.gh2727;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Gerrit Meier
 * @param <T> relationship properties type
 */
@RelationshipProperties
public class OrderedRelation<T> implements Comparable<OrderedRelation<T>> {
	@RelationshipId
	@GeneratedValue
	private Long id;
	@TargetNode
	private T target;
	@Property
	private Integer order;

	public OrderedRelation(Long id, T target, Integer order) {
		this.id = id;
		this.target = target;
		this.order = order;
	}

	public OrderedRelation() {
	}

	@Override
	public int compareTo(final OrderedRelation<T> o) {
		return order - o.order;
	}

	public Long getId() {
		return this.id;
	}

	public T getTarget() {
		return this.target;
	}

	public Integer getOrder() {
		return this.order;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setTarget(T target) {
		this.target = target;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}
}
