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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class BidirectionalStart {

	@Id @GeneratedValue private Long id;

	private String name;

	@Relationship("CONNECTED") private Set<BidirectionalEnd> ends;

	public BidirectionalStart(String name, Set<BidirectionalEnd> ends) {
		this.name = name;
		this.ends = ends;
	}

	public String getName() {
		return name;
	}

	public Set<BidirectionalEnd> getEnds() {
		return ends;
	}
}
