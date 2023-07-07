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
package org.springframework.data.neo4j.integration.issues.gh2415;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

/**
 * @author Andreas Berger
 */
@SuppressWarnings("HiddenField")
@Node
public class NodeEntity extends BaseNodeEntity implements NodeWithDefinedCredentials {

	@JsonIgnore
	@Relationship(type = "CHILD_OF", direction = Relationship.Direction.INCOMING)
	private Set<BaseNodeEntity> children;

	@Relationship(type = "HAS_CREDENTIAL")
	private Set<Credential> definedCredentials;

	protected NodeEntity() {
	}

	protected NodeEntity(NodeEntityBuilder<?, ?> b) {
		super(b);
		this.children = b.children;
		this.definedCredentials = b.definedCredentials;
	}

	public static NodeEntityBuilder<?, ?> builder() {
		return new NodeEntityBuilderImpl();
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public Set<BaseNodeEntity> getChildren() {
		return this.children;
	}

	public Set<Credential> getDefinedCredentials() {
		return this.definedCredentials;
	}

	@JsonIgnore
	private void setChildren(Set<BaseNodeEntity> children) {
		this.children = children;
	}

	private void setDefinedCredentials(Set<Credential> definedCredentials) {
		this.definedCredentials = definedCredentials;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof NodeEntity)) {
			return false;
		}
		final NodeEntity other = (NodeEntity) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof NodeEntity;
	}

	public int hashCode() {
		int result = super.hashCode();
		return result;
	}

	public NodeEntityBuilder<?, ?> toBuilder() {
		return new NodeEntityBuilderImpl().$fillValuesFrom(this);
	}

	/**
	 * the builder
	 * @param <C> needed c type
	 * @param <B> needed b type
	 */
	public static abstract class NodeEntityBuilder<C extends NodeEntity, B extends NodeEntityBuilder<C, B>> extends BaseNodeEntityBuilder<C, B> {
		private Set<BaseNodeEntity> children;
		private Set<Credential> definedCredentials;

		private static void $fillValuesFromInstanceIntoBuilder(NodeEntity instance, NodeEntityBuilder<?, ?> b) {
			b.children(instance.children);
			b.definedCredentials(instance.definedCredentials);
		}

		@JsonIgnore
		public B children(Set<BaseNodeEntity> children) {
			this.children = children;
			return self();
		}

		public B definedCredentials(Set<Credential> definedCredentials) {
			this.definedCredentials = definedCredentials;
			return self();
		}

		protected B $fillValuesFrom(C instance) {
			super.$fillValuesFrom(instance);
			NodeEntityBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
			return self();
		}

		protected abstract B self();

		public abstract C build();

		public String toString() {
			return "NodeEntity.NodeEntityBuilder(super=" + super.toString() + ", children=" + this.children + ", definedCredentials=" + this.definedCredentials + ")";
		}
	}

	private static final class NodeEntityBuilderImpl extends NodeEntityBuilder<NodeEntity, NodeEntityBuilderImpl> {
		private NodeEntityBuilderImpl() {
		}

		protected NodeEntityBuilderImpl self() {
			return this;
		}

		public NodeEntity build() {
			return new NodeEntity(this);
		}
	}
}
