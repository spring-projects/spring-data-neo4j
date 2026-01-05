/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2622;

import java.util.List;
import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class MePointingTowardsMe {

	@Relationship
	public final List<MePointingTowardsMe> others;

	final String name;

	@Id
	@GeneratedValue
	Long id;

	public MePointingTowardsMe(String name, List<MePointingTowardsMe> others) {
		this.name = name;
		this.others = others;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MePointingTowardsMe that = (MePointingTowardsMe) o;
		return this.name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

}
