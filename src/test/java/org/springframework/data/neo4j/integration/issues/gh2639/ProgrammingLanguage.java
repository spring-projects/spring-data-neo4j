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
package org.springframework.data.neo4j.integration.issues.gh2639;

import java.util.StringJoiner;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Programming language to represent. Only available at the Developer entity.
 */
@Node
public class ProgrammingLanguage {

	private final String name;

	private final String version;

	@Relationship("INVENTED_BY")
	public Inventor inventor;

	@Id
	@GeneratedValue
	private Long id;

	public ProgrammingLanguage(String name, String version) {
		this.name = name;
		this.version = version;
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", ProgrammingLanguage.class.getSimpleName() + "[", "]").add("id=" + this.id)
			.add("name='" + this.name + "'")
			.add("version='" + this.version + "'")
			.toString();
	}

}
