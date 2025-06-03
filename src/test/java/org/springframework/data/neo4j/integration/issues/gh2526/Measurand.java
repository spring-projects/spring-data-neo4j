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
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Target node
 */
@Node
@Immutable
public final class Measurand {

	@Id
	private final String measurandId;

	public Measurand(String measurandId) {
		this.measurandId = measurandId;
	}

	public String getMeasurandId() {
		return this.measurandId;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Measurand)) {
			return false;
		}
		final Measurand other = (Measurand) o;
		final Object this$measurandId = this.getMeasurandId();
		final Object other$measurandId = other.getMeasurandId();
		return Objects.equals(this$measurandId, other$measurandId);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $measurandId = this.getMeasurandId();
		result = result * PRIME + (($measurandId != null) ? $measurandId.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "Measurand(measurandId=" + this.getMeasurandId() + ")";
	}

}
