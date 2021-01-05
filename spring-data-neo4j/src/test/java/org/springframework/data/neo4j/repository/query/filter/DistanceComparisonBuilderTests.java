/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.repository.query.filter;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Percentage.*;
import static org.springframework.data.neo4j.repository.query.filter.DistanceComparisonBuilder.*;

import org.junit.Test;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;

public class DistanceComparisonBuilderTests {
	@Test
	public void calculateDistanceInMeterShouldWork() {

		Distance distance;

		distance = new Distance(1, Metrics.KILOMETERS);
		assertThat(calculateDistanceInMeter(distance)).isCloseTo(1000.0, withPercentage(0.001));

		distance = new Distance(1, Metrics.MILES);
		assertThat(calculateDistanceInMeter(distance)).isCloseTo(1609.34, withPercentage(0.001));

		distance = new Distance(1, Metrics.NEUTRAL);
		assertThat(calculateDistanceInMeter(distance)).isEqualTo(1.0);
	}
}
