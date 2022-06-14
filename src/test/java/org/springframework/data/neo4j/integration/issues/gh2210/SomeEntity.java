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
package org.springframework.data.neo4j.integration.issues.gh2210;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 * @soundtrack Sodom - Sodom
 */
// tag::custom-query.paths.dm[]
@Node
public class SomeEntity {

	@Id
	private final Long number;

	private String name;

	@Relationship(type = "SOME_RELATION_TO", direction = Relationship.Direction.OUTGOING)
	private Set<SomeRelation> someRelationsOut = new HashSet<>();
	// end::custom-query.paths.dm[]

	public Long getNumber() {
		return number;
	}

	public String getName() {
		return name;
	}

	public Set<SomeRelation> getSomeRelationsOut() {
		return someRelationsOut;
	}

	SomeEntity(Long number) {
		this.number = number;
	}
	// tag::custom-query.paths.dm[]
}
// end::custom-query.paths.dm[]
