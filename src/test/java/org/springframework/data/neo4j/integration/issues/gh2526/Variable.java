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
package org.springframework.data.neo4j.integration.issues.gh2526;

import java.util.Objects;

import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Second type of relationship
 */
@SuppressWarnings("HiddenField")
@RelationshipProperties
@Immutable
public final class Variable {

	@RelationshipId
	private final Long id;

	@TargetNode
	private final MeasurementMeta measurement;

	private final String variable;

	public Variable(Long id, MeasurementMeta measurement, String variable) {
		this.id = id;
		this.measurement = measurement;
		this.variable = variable;
	}

	public static Variable create(MeasurementMeta measurement, String variable) {
		return new Variable(null, measurement, variable);
	}

	public Long getId() {
		return this.id;
	}

	public MeasurementMeta getMeasurement() {
		return this.measurement;
	}

	public String getVariable() {
		return this.variable;
	}

	public Variable withId(Long id) {
		return Objects.equals(this.id, id) ? this : new Variable(id, this.measurement, this.variable);
	}

	public Variable withMeasurement(MeasurementMeta measurement) {
		return (this.measurement != measurement) ? new Variable(this.id, measurement, this.variable) : this;
	}

	public Variable withVariable(String variable) {
		return Objects.equals(this.variable, variable) ? this : new Variable(this.id, this.measurement, variable);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Variable)) {
			return false;
		}
		final Variable other = (Variable) o;
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$measurement = this.getMeasurement();
		final Object other$measurement = other.getMeasurement();
		if (!Objects.equals(this$measurement, other$measurement)) {
			return false;
		}
		final Object this$variable = this.getVariable();
		final Object other$variable = other.getVariable();
		return Objects.equals(this$variable, other$variable);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = (result * PRIME) + (($id != null) ? $id.hashCode() : 43);
		final Object $measurement = this.getMeasurement();
		result = (result * PRIME) + (($measurement != null) ? $measurement.hashCode() : 43);
		final Object $variable = this.getVariable();
		result = (result * PRIME) + (($variable != null) ? $variable.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return this.variable + ": " + this.measurement.getNodeId();
	}

}
