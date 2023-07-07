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

import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Relationship with properties between measurement and measurand
 */
@SuppressWarnings("HiddenField")
@RelationshipProperties
@Immutable
public final class DataPoint {

	@RelationshipId
	private final
	Long id;

	private final boolean manual;

	@TargetNode
	private final
	Measurand measurand;

	public DataPoint(Long id, boolean manual, Measurand measurand) {
		this.id = id;
		this.manual = manual;
		this.measurand = measurand;
	}

	public Long getId() {
		return this.id;
	}

	public boolean isManual() {
		return this.manual;
	}

	public Measurand getMeasurand() {
		return this.measurand;
	}

	public String toString() {
		return "DataPoint(id=" + this.getId() + ", manual=" + this.isManual() + ", measurand=" + this.getMeasurand() + ")";
	}

	public DataPoint withId(Long id) {
		return this.id == id ? this : new DataPoint(id, this.manual, this.measurand);
	}

	public DataPoint withManual(boolean manual) {
		return this.manual == manual ? this : new DataPoint(this.id, manual, this.measurand);
	}

	public DataPoint withMeasurand(Measurand measurand) {
		return this.measurand == measurand ? this : new DataPoint(this.id, this.manual, measurand);
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof DataPoint)) {
			return false;
		}
		final DataPoint other = (DataPoint) o;
		final Object this$measurand = this.getMeasurand();
		final Object other$measurand = other.getMeasurand();
		if (this$measurand == null ? other$measurand != null : !this$measurand.equals(other$measurand)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $measurand = this.getMeasurand();
		result = result * PRIME + ($measurand == null ? 43 : $measurand.hashCode());
		return result;
	}
}
