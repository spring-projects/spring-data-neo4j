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

import java.util.List;
import java.util.StringJoiner;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * Developer holds the specific relationship we are trying to map in this test case.
 */
@Node
public class Developer extends CompanyPerson {

	private final List<LanguageRelationship> programmingLanguages;

	private final String name;

	public Developer(String name, List<LanguageRelationship> programmingLanguages) {
		this.name = name;
		this.programmingLanguages = programmingLanguages;
	}

	public List<LanguageRelationship> getProgrammingLanguages() {
		return this.programmingLanguages;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", Developer.class.getSimpleName() + "[", "]").add("name='" + this.name + "'")
			.add("programmingLanguages=" + this.programmingLanguages)
			.toString();
	}

}
