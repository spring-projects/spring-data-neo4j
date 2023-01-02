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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Zurück zum Glück
 */
@Node
public class ExtendedParentNode extends ParentNode {

	private String someOtherAttribute;

	@Relationship("CONNECTED_TO")
	private List<PersonWithAllConstructor> people;

	public String getSomeOtherAttribute() {
		return someOtherAttribute;
	}

	public void setSomeOtherAttribute(String someOtherAttribute) {
		this.someOtherAttribute = someOtherAttribute;
	}

	public List<PersonWithAllConstructor> getPeople() {
		return people;
	}

	public void setPeople(List<PersonWithAllConstructor> people) {
		this.people = people;
	}
}
