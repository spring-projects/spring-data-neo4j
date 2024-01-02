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
package org.springframework.data.neo4j.integration.issues.gh2579;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Michael J. Simons
 */
@RelationshipProperties
public class TableAndColumnRelation {

	@RelationshipId
	private Long id;

	@TargetNode
	private ColumnNode columnNode;

	public TableAndColumnRelation() {
	}

	public Long getId() {
		return this.id;
	}

	public ColumnNode getColumnNode() {
		return this.columnNode;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setColumnNode(ColumnNode columnNode) {
		this.columnNode = columnNode;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof TableAndColumnRelation)) {
			return false;
		}
		final TableAndColumnRelation other = (TableAndColumnRelation) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$columnNode = this.getColumnNode();
		final Object other$columnNode = other.getColumnNode();
		if (this$columnNode == null ? other$columnNode != null : !this$columnNode.equals(other$columnNode)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof TableAndColumnRelation;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $columnNode = this.getColumnNode();
		result = result * PRIME + ($columnNode == null ? 43 : $columnNode.hashCode());
		return result;
	}

	public String toString() {
		return "TableAndColumnRelation(id=" + this.getId() + ", columnNode=" + this.getColumnNode() + ")";
	}
}
