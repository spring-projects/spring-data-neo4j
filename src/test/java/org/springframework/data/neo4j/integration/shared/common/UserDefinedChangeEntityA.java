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

import java.util.List;

import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UserDefinedChangeSupport;

/**
 * @author Gerrit Meier
 */
@Node
public class UserDefinedChangeEntityA implements UserDefinedChangeSupport {

	@Id
	@GeneratedValue
	String id;

	public String name;

	@Relationship("DIRECT")
	public List<UserDefinedChangeEntityB> bs;

	@Relationship("PROPERTY")
	public List<UserDefinedChangeRelationshipProperty> relationshipProperties;

	@Transient
	public boolean needsUpdate;

	public UserDefinedChangeEntityA(String name) {
		this.name = name;
	}

	@Override
	public boolean needsUpdate() {
		return this.needsUpdate;
	}
}
