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
package org.springframework.data.neo4j.integration.issues.gh2526;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Defining most concrete entity
 */
@SuppressWarnings("HiddenField")
@Node
public class AccountingMeasurementMeta extends MeasurementMeta {

	private String formula;

	@Relationship(type = "WEIGHTS", direction = Relationship.Direction.OUTGOING)
	private MeasurementMeta baseMeasurement;

	protected AccountingMeasurementMeta(String formula, MeasurementMeta baseMeasurement) {
		this.formula = formula;
		this.baseMeasurement = baseMeasurement;
	}

	protected AccountingMeasurementMeta() {
	}

	protected AccountingMeasurementMeta(AccountingMeasurementMetaBuilder<?, ?> b) {
		super(b);
		this.formula = b.formula;
		this.baseMeasurement = b.baseMeasurement;
	}

	public static AccountingMeasurementMetaBuilder<?, ?> builder() {
		return new AccountingMeasurementMetaBuilderImpl();
	}

	public String getFormula() {
		return this.formula;
	}

	public MeasurementMeta getBaseMeasurement() {
		return this.baseMeasurement;
	}

	public String toString() {
		return "AccountingMeasurementMeta(formula=" + this.getFormula() + ", baseMeasurement=" + this.getBaseMeasurement() + ")";
	}

	private void setFormula(String formula) {
		this.formula = formula;
	}

	private void setBaseMeasurement(MeasurementMeta baseMeasurement) {
		this.baseMeasurement = baseMeasurement;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof AccountingMeasurementMeta)) {
			return false;
		}
		final AccountingMeasurementMeta other = (AccountingMeasurementMeta) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof AccountingMeasurementMeta;
	}

	public int hashCode() {
		int result = super.hashCode();
		return result;
	}

	public AccountingMeasurementMetaBuilder<?, ?> toBuilder() {
		return new AccountingMeasurementMetaBuilderImpl().$fillValuesFrom(this);
	}

	/**
	 * the builder
	 * @param <C> needed c type
	 * @param <B> needed b type
	 */
	public static abstract class AccountingMeasurementMetaBuilder<C extends AccountingMeasurementMeta, B extends AccountingMeasurementMetaBuilder<C, B>> extends MeasurementMetaBuilder<C, B> {
		private String formula;
		private MeasurementMeta baseMeasurement;

		private static void $fillValuesFromInstanceIntoBuilder(AccountingMeasurementMeta instance, AccountingMeasurementMetaBuilder<?, ?> b) {
			b.formula(instance.formula);
			b.baseMeasurement(instance.baseMeasurement);
		}

		public B formula(String formula) {
			this.formula = formula;
			return self();
		}

		public B baseMeasurement(MeasurementMeta baseMeasurement) {
			this.baseMeasurement = baseMeasurement;
			return self();
		}

		protected B $fillValuesFrom(C instance) {
			super.$fillValuesFrom(instance);
			AccountingMeasurementMetaBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
			return self();
		}

		protected abstract B self();

		public abstract C build();

		public String toString() {
			return "AccountingMeasurementMeta.AccountingMeasurementMetaBuilder(super=" + super.toString() + ", formula=" + this.formula + ", baseMeasurement=" + this.baseMeasurement + ")";
		}
	}

	private static final class AccountingMeasurementMetaBuilderImpl extends AccountingMeasurementMetaBuilder<AccountingMeasurementMeta, AccountingMeasurementMetaBuilderImpl> {
		private AccountingMeasurementMetaBuilderImpl() {
		}

		protected AccountingMeasurementMetaBuilderImpl self() {
			return this;
		}

		public AccountingMeasurementMeta build() {
			return new AccountingMeasurementMeta(this);
		}
	}
}
