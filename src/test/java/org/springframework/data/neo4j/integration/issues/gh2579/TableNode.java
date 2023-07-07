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
package org.springframework.data.neo4j.integration.issues.gh2579;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

/**
 * @author Michael J. Simons
 */
@Node("Table")
public class TableNode {

	@Id
	@GeneratedValue
	private Long id;

	private String sourceName;

	private String schemaName;

	private String name;

	private String tableComment;

	@Relationship(type = "BELONG", direction = Relationship.Direction.INCOMING)
	private List<TableAndColumnRelation> tableAndColumnRelation;

	public TableNode() {
	}

	public Long getId() {
		return this.id;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public String getSchemaName() {
		return this.schemaName;
	}

	public String getName() {
		return this.name;
	}

	public String getTableComment() {
		return this.tableComment;
	}

	public List<TableAndColumnRelation> getTableAndColumnRelation() {
		return this.tableAndColumnRelation;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTableComment(String tableComment) {
		this.tableComment = tableComment;
	}

	public void setTableAndColumnRelation(List<TableAndColumnRelation> tableAndColumnRelation) {
		this.tableAndColumnRelation = tableAndColumnRelation;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof TableNode)) {
			return false;
		}
		final TableNode other = (TableNode) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$sourceName = this.getSourceName();
		final Object other$sourceName = other.getSourceName();
		if (this$sourceName == null ? other$sourceName != null : !this$sourceName.equals(other$sourceName)) {
			return false;
		}
		final Object this$schemaName = this.getSchemaName();
		final Object other$schemaName = other.getSchemaName();
		if (this$schemaName == null ? other$schemaName != null : !this$schemaName.equals(other$schemaName)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		final Object this$tableComment = this.getTableComment();
		final Object other$tableComment = other.getTableComment();
		if (this$tableComment == null ? other$tableComment != null : !this$tableComment.equals(other$tableComment)) {
			return false;
		}
		final Object this$tableAndColumnRelation = this.getTableAndColumnRelation();
		final Object other$tableAndColumnRelation = other.getTableAndColumnRelation();
		if (this$tableAndColumnRelation == null ? other$tableAndColumnRelation != null : !this$tableAndColumnRelation.equals(other$tableAndColumnRelation)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof TableNode;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $sourceName = this.getSourceName();
		result = result * PRIME + ($sourceName == null ? 43 : $sourceName.hashCode());
		final Object $schemaName = this.getSchemaName();
		result = result * PRIME + ($schemaName == null ? 43 : $schemaName.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $tableComment = this.getTableComment();
		result = result * PRIME + ($tableComment == null ? 43 : $tableComment.hashCode());
		final Object $tableAndColumnRelation = this.getTableAndColumnRelation();
		result = result * PRIME + ($tableAndColumnRelation == null ? 43 : $tableAndColumnRelation.hashCode());
		return result;
	}

	public String toString() {
		return "TableNode(id=" + this.getId() + ", sourceName=" + this.getSourceName() + ", schemaName=" + this.getSchemaName() + ", name=" + this.getName() + ", tableComment=" + this.getTableComment() + ", tableAndColumnRelation=" + this.getTableAndColumnRelation() + ")";
	}
}
