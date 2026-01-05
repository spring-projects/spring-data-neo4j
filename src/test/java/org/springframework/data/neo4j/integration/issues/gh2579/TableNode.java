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

import java.util.List;
import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

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

	public void setId(Long id) {
		this.id = id;
	}

	public String getSourceName() {
		return this.sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public String getSchemaName() {
		return this.schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTableComment() {
		return this.tableComment;
	}

	public void setTableComment(String tableComment) {
		this.tableComment = tableComment;
	}

	public List<TableAndColumnRelation> getTableAndColumnRelation() {
		return this.tableAndColumnRelation;
	}

	public void setTableAndColumnRelation(List<TableAndColumnRelation> tableAndColumnRelation) {
		this.tableAndColumnRelation = tableAndColumnRelation;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof TableNode;
	}

	@Override
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
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$sourceName = this.getSourceName();
		final Object other$sourceName = other.getSourceName();
		if (!Objects.equals(this$sourceName, other$sourceName)) {
			return false;
		}
		final Object this$schemaName = this.getSchemaName();
		final Object other$schemaName = other.getSchemaName();
		if (!Objects.equals(this$schemaName, other$schemaName)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (!Objects.equals(this$name, other$name)) {
			return false;
		}
		final Object this$tableComment = this.getTableComment();
		final Object other$tableComment = other.getTableComment();
		if (!Objects.equals(this$tableComment, other$tableComment)) {
			return false;
		}
		final Object this$tableAndColumnRelation = this.getTableAndColumnRelation();
		final Object other$tableAndColumnRelation = other.getTableAndColumnRelation();
		return Objects.equals(this$tableAndColumnRelation, other$tableAndColumnRelation);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = (result * PRIME) + (($id != null) ? $id.hashCode() : 43);
		final Object $sourceName = this.getSourceName();
		result = (result * PRIME) + (($sourceName != null) ? $sourceName.hashCode() : 43);
		final Object $schemaName = this.getSchemaName();
		result = (result * PRIME) + (($schemaName != null) ? $schemaName.hashCode() : 43);
		final Object $name = this.getName();
		result = (result * PRIME) + (($name != null) ? $name.hashCode() : 43);
		final Object $tableComment = this.getTableComment();
		result = (result * PRIME) + (($tableComment != null) ? $tableComment.hashCode() : 43);
		final Object $tableAndColumnRelation = this.getTableAndColumnRelation();
		result = (result * PRIME) + (($tableAndColumnRelation != null) ? $tableAndColumnRelation.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "TableNode(id=" + this.getId() + ", sourceName=" + this.getSourceName() + ", schemaName="
				+ this.getSchemaName() + ", name=" + this.getName() + ", tableComment=" + this.getTableComment()
				+ ", tableAndColumnRelation=" + this.getTableAndColumnRelation() + ")";
	}

}
