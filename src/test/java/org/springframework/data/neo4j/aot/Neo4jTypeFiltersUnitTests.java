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

package org.springframework.data.neo4j.aot;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;

import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.util.TypeCollector;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Neo4jTypeFilters}.
 *
 * @author Mark Paluch
 */
class Neo4jTypeFiltersUnitTests {

	@Test
	void shouldExcludeNeo4jSimpleTypes() {
		assertThat(TypeCollector.inspect(MyEntity.class).list()).containsOnly(MyEntity.class, OtherEntity.class);
	}

	public static class MyEntity {

		private BigDecimal aNumber;

		private GeographicPoint2d point;

		private Value value;

		private OtherEntity other;

		public BigDecimal getaNumber() {
			return this.aNumber;
		}

		public void setaNumber(BigDecimal aNumber) {
			this.aNumber = aNumber;
		}

		public GeographicPoint2d getPoint() {
			return this.point;
		}

		public void setPoint(GeographicPoint2d point) {
			this.point = point;
		}

		public Value getValue() {
			return this.value;
		}

		public void setValue(Value value) {
			this.value = value;
		}

		public OtherEntity getOther() {
			return this.other;
		}

		public void setOther(OtherEntity other) {
			this.other = other;
		}

	}

	public static class OtherEntity {

	}

}
