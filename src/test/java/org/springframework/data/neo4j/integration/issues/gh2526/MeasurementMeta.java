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
package org.springframework.data.neo4j.integration.issues.gh2526;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.integration.issues.gh2415.BaseNodeEntity;

/**
 * Defining relationship to measurand
 */
@SuppressWarnings("HiddenField")
@Node
public class MeasurementMeta extends BaseNodeEntity {

	@Relationship(type = "IS_MEASURED_BY", direction = Relationship.Direction.INCOMING)
	private Set<DataPoint> dataPoints;

	@Relationship(type = "USES", direction = Relationship.Direction.OUTGOING)
	private Set<Variable> variables;

	protected MeasurementMeta(Set<DataPoint> dataPoints, Set<Variable> variables) {
		this.dataPoints = dataPoints;
		this.variables = variables;
	}

	protected MeasurementMeta() {
	}

	protected MeasurementMeta(MeasurementMetaBuilder<?, ?> b) {
		super(b);
		this.dataPoints = b.dataPoints;
		this.variables = b.variables;
	}

	public static MeasurementMetaBuilder<?, ?> builder() {
		return new MeasurementMetaBuilderImpl();
	}

	public Set<DataPoint> getDataPoints() {
		return this.dataPoints;
	}

	private void setDataPoints(Set<DataPoint> dataPoints) {
		this.dataPoints = dataPoints;
	}

	public Set<Variable> getVariables() {
		return this.variables;
	}

	private void setVariables(Set<Variable> variables) {
		this.variables = variables;
	}

	@Override
	protected boolean canEqual(final Object other) {
		return other instanceof MeasurementMeta;
	}

	@Override
	public MeasurementMetaBuilder<?, ?> toBuilder() {
		return new MeasurementMetaBuilderImpl().$fillValuesFrom(this);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof MeasurementMeta)) {
			return false;
		}
		final MeasurementMeta other = (MeasurementMeta) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "MeasurementMeta(dataPoints=" + this.getDataPoints() + ", variables=" + this.getVariables() + ")";
	}

	/**
	 * the builder
	 *
	 * @param <C> needed c type
	 * @param <B> needed b type
	 */
	public abstract static class MeasurementMetaBuilder<C extends MeasurementMeta, B extends MeasurementMetaBuilder<C, B>>
			extends BaseNodeEntityBuilder<C, B> {

		private Set<DataPoint> dataPoints;

		private Set<Variable> variables;

		private static void $fillValuesFromInstanceIntoBuilder(MeasurementMeta instance,
				MeasurementMetaBuilder<?, ?> b) {
			b.dataPoints(instance.dataPoints);
			b.variables(instance.variables);
		}

		public B dataPoints(Set<DataPoint> dataPoints) {
			this.dataPoints = dataPoints;
			return self();
		}

		public B variables(Set<Variable> variables) {
			this.variables = variables;
			return self();
		}

		@Override
		protected B $fillValuesFrom(C instance) {
			super.$fillValuesFrom(instance);
			MeasurementMetaBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
			return self();
		}

		@Override
		protected abstract B self();

		@Override
		public abstract C build();

		@Override
		public String toString() {
			return "MeasurementMeta.MeasurementMetaBuilder(super=" + super.toString() + ", dataPoints="
					+ this.dataPoints + ", variables=" + this.variables + ")";
		}

	}

	private static final class MeasurementMetaBuilderImpl
			extends MeasurementMetaBuilder<MeasurementMeta, MeasurementMetaBuilderImpl> {

		private MeasurementMetaBuilderImpl() {
		}

		@Override
		protected MeasurementMetaBuilderImpl self() {
			return this;
		}

		@Override
		public MeasurementMeta build() {
			return new MeasurementMeta(this);
		}

	}

}
