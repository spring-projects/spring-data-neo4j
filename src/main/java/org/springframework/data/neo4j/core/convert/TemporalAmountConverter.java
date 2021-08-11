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
package org.springframework.data.neo4j.core.convert;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.IsoDuration;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;

/**
 * This generic converter has been introduce to augment the {@link TemporalAmountAdapter} with the type information passed
 * to a generic converter to make some educated guesses whether an {@link org.neo4j.driver.types.IsoDuration} of {@literal 0}
 * should be possibly treated as {@link java.time.Period} or {@link java.time.Duration}.
 *
 * @author Michael J. Simons
 * @soundtrack Motörhead - Bomber
 */
final class TemporalAmountConverter implements GenericConverter {

	private final TemporalAmountAdapter adapter = new TemporalAmountAdapter();
	private final Set<ConvertiblePair> convertibleTypes = Collections.unmodifiableSet(
			new HashSet<>(Arrays.asList(
					new ConvertiblePair(Value.class, TemporalAmount.class),
					new ConvertiblePair(TemporalAmount.class, Value.class)
			)));

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return convertibleTypes;
	}

	@Override
	public Object convert(@Nullable Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (TemporalAmount.class.isAssignableFrom(sourceType.getType())) {
			return Values.value(value);
		}

		boolean valueIsLiteralNullOrNullValue = value == null || value == Values.NULL;
		Object convertedValue = valueIsLiteralNullOrNullValue ? null : adapter.apply(((Value) value).asIsoDuration());

		if (convertedValue instanceof IsoDuration && isZero((IsoDuration) convertedValue)) {
			if (Period.class.isAssignableFrom(targetType.getType())) {
				return Period.of(0, 0, 0);
			} else if (Duration.class.isAssignableFrom(targetType.getType())) {
				return Duration.ZERO;
			}
		}
		return convertedValue;
	}

	/**
	 * @param isoDuration The duration to check whether it's {@literal 0} or not.
	 * @return True if there are only temporal units in that duration with a value of {@literal 0}.
	 */
	private static boolean isZero(IsoDuration isoDuration) {

		return isoDuration.months() == 0L && isoDuration.days() == 0L &&
			   isoDuration.seconds() == 0L && isoDuration.nanoseconds() == 0L;
	}
}
