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
package org.springframework.data.neo4j.integration.issues.gh2474;

import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Stephen Jackson
 */
@RelationshipProperties
public class JobRelationship {

	@Id
	@GeneratedValue
	private Long id;

	@TargetNode
	private PersonModel person;

	private String jobTitle;

	public JobRelationship() {
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PersonModel getPerson() {
		return this.person;
	}

	public void setPerson(PersonModel person) {
		this.person = person;
	}

	public String getJobTitle() {
		return this.jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof JobRelationship;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof JobRelationship)) {
			return false;
		}
		final JobRelationship other = (JobRelationship) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$person = this.getPerson();
		final Object other$person = other.getPerson();
		if (!Objects.equals(this$person, other$person)) {
			return false;
		}
		final Object this$jobTitle = this.getJobTitle();
		final Object other$jobTitle = other.getJobTitle();
		return Objects.equals(this$jobTitle, other$jobTitle);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
		final Object $person = this.getPerson();
		result = result * PRIME + (($person != null) ? $person.hashCode() : 43);
		final Object $jobTitle = this.getJobTitle();
		result = result * PRIME + (($jobTitle != null) ? $jobTitle.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "JobRelationship(id=" + this.getId() + ", person=" + this.getPerson() + ", jobTitle="
				+ this.getJobTitle() + ")";
	}

}
