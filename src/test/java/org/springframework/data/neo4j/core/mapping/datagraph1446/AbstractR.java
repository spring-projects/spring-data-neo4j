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
package org.springframework.data.neo4j.core.mapping.datagraph1446;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @param <T> The type of the target entity
 * @author Michael J. Simons
 */
@RelationshipProperties
public abstract class AbstractR<T> {

	@TargetNode
	T target;

	@RelationshipId
	private Long id;

	private String p1;

	private String p2;

	public AbstractR(T target) {
		this.target = target;
	}

	public String getP1() {
		return this.p1;
	}

	public void setP1(String p1) {
		this.p1 = p1;
	}

	public String getP2() {
		return this.p2;
	}

	public void setP2(String p2) {
		this.p2 = p2;
	}

	@Override
	public String toString() {
		return "AbstractR{" + "target=" + this.target + ", p1='" + this.p1 + '\'' + ", p2='" + this.p2 + '\'' + '}';
	}

}
