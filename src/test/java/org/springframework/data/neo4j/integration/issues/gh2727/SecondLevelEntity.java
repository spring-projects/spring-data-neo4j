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
@Node("SecondLevel")
public class SecondLevelEntity {
	@Id
	@GeneratedValue
	private Long id;

	private String someValue;

	@Relationship("HasThirdLevel")
	private List<ThirdLevelEntityRelationship> thirdLevelEntityRelationshipProperties;

	public SecondLevelEntity() {
	}

	protected SecondLevelEntity(SecondLevelEntityBuilder<?, ?> b) {
		this.id = b.id;
		this.someValue = b.someValue;
		this.thirdLevelEntityRelationshipProperties = b.thirdLevelEntityRelationshipProperties;
	}

	public static SecondLevelEntityBuilder<?, ?> builder() {
		return new SecondLevelEntityBuilderImpl();
	}

	public Long getId() {
		return this.id;
	}

	public String getSomeValue() {
		return this.someValue;
	}

	public List<ThirdLevelEntityRelationship> getThirdLevelEntityRelationshipProperties() {
		return this.thirdLevelEntityRelationshipProperties;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setSomeValue(String someValue) {
		this.someValue = someValue;
	}

	public void setThirdLevelEntityRelationshipProperties(List<ThirdLevelEntityRelationship> thirdLevelEntityRelationshipProperties) {
		this.thirdLevelEntityRelationshipProperties = thirdLevelEntityRelationshipProperties;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof SecondLevelEntity)) {
			return false;
		}
		final SecondLevelEntity other = (SecondLevelEntity) o;
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
		return other instanceof SecondLevelEntity;
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
	public static abstract class SecondLevelEntityBuilder<C extends SecondLevelEntity, B extends SecondLevelEntityBuilder<C, B>> {
		private Long id;
		private String someValue;
		private List<ThirdLevelEntityRelationship> thirdLevelEntityRelationshipProperties;

		public B id(Long id) {
			this.id = id;
			return self();
		}

		public B someValue(String someValue) {
			this.someValue = someValue;
			return self();
		}

		public B thirdLevelEntityRelationshipProperties(List<ThirdLevelEntityRelationship> thirdLevelEntityRelationshipProperties) {
			this.thirdLevelEntityRelationshipProperties = thirdLevelEntityRelationshipProperties;
			return self();
		}

		protected abstract B self();

		public abstract C build();

		public String toString() {
			return "SecondLevelEntity.SecondLevelEntityBuilder(id=" + this.id + ", someValue=" + this.someValue + ", thirdLevelEntityRelationshipProperties=" + this.thirdLevelEntityRelationshipProperties + ")";
		}
	}

	private static final class SecondLevelEntityBuilderImpl extends SecondLevelEntityBuilder<SecondLevelEntity, SecondLevelEntityBuilderImpl> {
		private SecondLevelEntityBuilderImpl() {
		}

		protected SecondLevelEntityBuilderImpl self() {
			return this;
		}

		public SecondLevelEntity build() {
			return new SecondLevelEntity(this);
		}
	}
}
