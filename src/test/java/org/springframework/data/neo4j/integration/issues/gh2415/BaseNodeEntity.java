/*
 * Copyright 2011-2024 the original author or authors.
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

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Andreas Berger
 */
@SuppressWarnings("HiddenField")
@Node
public class BaseNodeEntity {

	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	private String nodeId;

	private String name;

	protected BaseNodeEntity() {
	}

	protected BaseNodeEntity(BaseNodeEntityBuilder<?, ?> b) {
		this.nodeId = b.nodeId;
		this.name = b.name;
	}

	public static BaseNodeEntityBuilder<?, ?> builder() {
		return new BaseNodeEntityBuilderImpl();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " - " + getName() + " (" + getNodeId() + ")";
	}

	public String getNodeId() {
		return this.nodeId;
	}

	public String getName() {
		return this.name;
	}

	private void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	private void setName(String name) {
		this.name = name;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof BaseNodeEntity)) {
			return false;
		}
		final BaseNodeEntity other = (BaseNodeEntity) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$nodeId = this.getNodeId();
		final Object other$nodeId = other.getNodeId();
		if (this$nodeId == null ? other$nodeId != null : !this$nodeId.equals(other$nodeId)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof BaseNodeEntity;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $nodeId = this.getNodeId();
		result = result * PRIME + ($nodeId == null ? 43 : $nodeId.hashCode());
		return result;
	}

	public BaseNodeEntityBuilder<?, ?> toBuilder() {
		return new BaseNodeEntityBuilderImpl().$fillValuesFrom(this);
	}

	/**
	 * the builder
	 * @param <C> needed c type
	 * @param <B> needed b type
	 */
	public static abstract class BaseNodeEntityBuilder<C extends BaseNodeEntity, B extends BaseNodeEntityBuilder<C, B>> {
		private String nodeId;
		private String name;

		private static void $fillValuesFromInstanceIntoBuilder(BaseNodeEntity instance, BaseNodeEntityBuilder<?, ?> b) {
			b.nodeId(instance.nodeId);
			b.name(instance.name);
		}

		public B nodeId(String nodeId) {
			this.nodeId = nodeId;
			return self();
		}

		public B name(String name) {
			this.name = name;
			return self();
		}

		protected B $fillValuesFrom(C instance) {
			BaseNodeEntityBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
			return self();
		}

		protected abstract B self();

		public abstract C build();

		public String toString() {
			return "BaseNodeEntity.BaseNodeEntityBuilder(nodeId=" + this.nodeId + ", name=" + this.name + ")";
		}
	}

	private static final class BaseNodeEntityBuilderImpl extends BaseNodeEntityBuilder<BaseNodeEntity, BaseNodeEntityBuilderImpl> {
		private BaseNodeEntityBuilderImpl() {
		}

		protected BaseNodeEntityBuilderImpl self() {
			return this;
		}

		public BaseNodeEntity build() {
			return new BaseNodeEntity(this);
		}
	}
}
