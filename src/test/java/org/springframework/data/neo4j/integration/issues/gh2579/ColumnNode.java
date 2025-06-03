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
package org.springframework.data.neo4j.integration.issues.gh2579;

import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@Node("Column")
public class ColumnNode {

	@Id
	@GeneratedValue
	private Long id;

	private String sourceName;

	private String schemaName;

	private String tableName;

	private String name;

	public ColumnNode() {
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

	public String getTableName() {
		return this.tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof ColumnNode;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof ColumnNode)) {
			return false;
		}
		final ColumnNode other = (ColumnNode) o;
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
		final Object this$tableName = this.getTableName();
		final Object other$tableName = other.getTableName();
		if (!Objects.equals(this$tableName, other$tableName)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		return Objects.equals(this$name, other$name);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
		final Object $sourceName = this.getSourceName();
		result = result * PRIME + (($sourceName != null) ? $sourceName.hashCode() : 43);
		final Object $schemaName = this.getSchemaName();
		result = result * PRIME + (($schemaName != null) ? $schemaName.hashCode() : 43);
		final Object $tableName = this.getTableName();
		result = result * PRIME + (($tableName != null) ? $tableName.hashCode() : 43);
		final Object $name = this.getName();
		result = result * PRIME + (($name != null) ? $name.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "ColumnNode(id=" + this.getId() + ", sourceName=" + this.getSourceName() + ", schemaName="
				+ this.getSchemaName() + ", tableName=" + this.getTableName() + ", name=" + this.getName() + ")";
	}

}
