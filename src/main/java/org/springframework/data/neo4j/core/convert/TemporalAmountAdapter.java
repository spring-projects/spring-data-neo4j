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
package org.springframework.data.neo4j.core.convert;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This adapter maps a Driver or embedded based {@link TemporalAmount} to a valid Java
 * temporal amount. It tries to be as specific as possible: If the amount can be reliable
 * mapped to a {@link Period}, it returns a period. If only fields are present that are no
 * estimated time unites, then it returns a {@link Duration}. <br>
 * <br>
 * In cases a user has used Cypher and its <code>duration()</code> function, i.e. like so
 * <code>CREATE (s:SomeTime {isoPeriod: duration('P13Y370M45DT25H120M')}) RETURN s</code>
 * a duration object has been created that cannot be represented by either a
 * {@link Period} or {@link Duration}. The user has to map it to a plain
 * {@link TemporalAmount} in these cases. <br>
 * The Java Driver uses a <code>org.neo4j.driver.v1.types.IsoDuration</code>, embedded
 * uses <code>org.neo4j.values.storable.DurationValue</code> for representing a temporal
 * amount, but in the end, they can be treated the same. However be aware that the
 * temporal amount returned in that case may not be equal to the other one, only
 * represents the same amount after normalization.
 *
 * @author Michael J. Simons
 */
final class TemporalAmountAdapter implements Function<TemporalAmount, TemporalAmount> {

	private static final int PERIOD_MASK = 0b11100;

	private static final int DURATION_MASK = 0b00011;

	private static final TemporalUnit[] SUPPORTED_UNITS = { ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS,
			ChronoUnit.SECONDS, ChronoUnit.NANOS };

	private static final short FIELD_YEAR = 0;

	private static final short FIELD_MONTH = 1;

	private static final short FIELD_DAY = 2;

	private static final short FIELD_SECONDS = 3;

	private static final short FIELD_NANOS = 4;

	private static final BiFunction<TemporalAmount, TemporalUnit, Integer> TEMPORAL_UNIT_EXTRACTOR = (d, u) -> {
		if (!d.getUnits().contains(u)) {
			return 0;
		}
		return Math.toIntExact(d.get(u));
	};

	private static boolean couldBePeriod(int type) {
		return (PERIOD_MASK & type) > 0;
	}

	private static boolean couldBeDuration(int type) {
		return (DURATION_MASK & type) > 0;
	}

	@Override
	public TemporalAmount apply(TemporalAmount internalTemporalAmountRepresentation) {

		int[] values = new int[SUPPORTED_UNITS.length];
		int type = 0;
		for (int i = 0; i < SUPPORTED_UNITS.length; ++i) {
			values[i] = TEMPORAL_UNIT_EXTRACTOR.apply(internalTemporalAmountRepresentation, SUPPORTED_UNITS[i]);
			type |= (values[i] == 0) ? 0 : (0b10000 >> i);
		}

		boolean couldBePeriod = couldBePeriod(type);
		boolean couldBeDuration = couldBeDuration(type);

		if (couldBePeriod && !couldBeDuration) {
			return Period.of(values[FIELD_YEAR], values[FIELD_MONTH], values[FIELD_DAY]).normalized();
		}
		else if (couldBeDuration && !couldBePeriod) {
			return Duration.ofSeconds(values[FIELD_SECONDS]).plusNanos(values[FIELD_NANOS]);
		}
		else {
			return internalTemporalAmountRepresentation;
		}
	}

}
