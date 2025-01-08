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
package org.springframework.data.neo4j.integration.issues.gh2727;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

/**
 * @author Gerrit Meier
 */
@SuppressWarnings("HiddenField")
@Node("FirstLevel")
public class FirstLevelEntity {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Relationship("HasSecondLevel")
	private List<SecondLevelEntityRelationship> secondLevelEntityRelationshipProperties;

	public FirstLevelEntity() {
	}

	protected FirstLevelEntity(FirstLevelEntityBuilder<?, ?> b) {
		this.id = b.id;
		this.name = b.name;
		this.secondLevelEntityRelationshipProperties = b.secondLevelEntityRelationshipProperties;
	}

	public static FirstLevelEntityBuilder<?, ?> builder() {
		return new FirstLevelEntityBuilderImpl();
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public List<SecondLevelEntityRelationship> getSecondLevelEntityRelationshipProperties() {
		return this.secondLevelEntityRelationshipProperties;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSecondLevelEntityRelationshipProperties(List<SecondLevelEntityRelationship> secondLevelEntityRelationshipProperties) {
		this.secondLevelEntityRelationshipProperties = secondLevelEntityRelationshipProperties;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof FirstLevelEntity)) {
			return false;
		}
		final FirstLevelEntity other = (FirstLevelEntity) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof FirstLevelEntity;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		return result;
	}

	/**
	 * the builder
	 * @param <C> needed c type
	 * @param <B> needed b type
	 */
	public static abstract class FirstLevelEntityBuilder<C extends FirstLevelEntity, B extends FirstLevelEntityBuilder<C, B>> {
		private Long id;
		private String name;
		private List<SecondLevelEntityRelationship> secondLevelEntityRelationshipProperties;

		public B id(Long id) {
			this.id = id;
			return self();
		}

		public B name(String name) {
			this.name = name;
			return self();
		}

		public B secondLevelEntityRelationshipProperties(List<SecondLevelEntityRelationship> secondLevelEntityRelationshipProperties) {
			this.secondLevelEntityRelationshipProperties = secondLevelEntityRelationshipProperties;
			return self();
		}

		protected abstract B self();

		public abstract C build();

		public String toString() {
			return "FirstLevelEntity.FirstLevelEntityBuilder(id=" + this.id + ", name=" + this.name + ", secondLevelEntityRelationshipProperties=" + this.secondLevelEntityRelationshipProperties + ")";
		}
	}

	private static final class FirstLevelEntityBuilderImpl extends FirstLevelEntityBuilder<FirstLevelEntity, FirstLevelEntityBuilderImpl> {
		private FirstLevelEntityBuilderImpl() {
		}

		protected FirstLevelEntityBuilderImpl self() {
			return this;
		}

		public FirstLevelEntity build() {
			return new FirstLevelEntity(this);
		}
	}
}
