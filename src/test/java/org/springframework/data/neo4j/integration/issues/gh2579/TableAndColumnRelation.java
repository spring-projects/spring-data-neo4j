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
package org.springframework.data.neo4j.integration.issues.gh2579;

import java.util.Objects;

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

	public void setId(Long id) {
		this.id = id;
	}

	public ColumnNode getColumnNode() {
		return this.columnNode;
	}

	public void setColumnNode(ColumnNode columnNode) {
		this.columnNode = columnNode;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof TableAndColumnRelation;
	}

	@Override
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
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$columnNode = this.getColumnNode();
		final Object other$columnNode = other.getColumnNode();
		return Objects.equals(this$columnNode, other$columnNode);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = (result * PRIME) + (($id != null) ? $id.hashCode() : 43);
		final Object $columnNode = this.getColumnNode();
		result = (result * PRIME) + (($columnNode != null) ? $columnNode.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "TableAndColumnRelation(id=" + this.getId() + ", columnNode=" + this.getColumnNode() + ")";
	}

}
