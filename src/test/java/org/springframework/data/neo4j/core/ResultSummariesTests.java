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
package org.springframework.data.neo4j.core;

import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.NotificationClassification;
import org.neo4j.driver.NotificationSeverity;
import org.neo4j.driver.internal.summary.InternalGqlNotification;
import org.neo4j.driver.summary.GqlNotification;
import org.neo4j.driver.summary.InputPosition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Michael J. Simons
 * @author Kevin Wittek
 */
class ResultSummariesTests {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	private static Stream<Arguments> params() {
		return Stream.of(
				Arguments.of("match (n) - [r:FOO*] -> (m) RETURN r", 1, 19,
						"\tmatch (n) - [r:FOO*] -> (m) RETURN r" + LINE_SEPARATOR + "\t                  ^"
								+ LINE_SEPARATOR),
				Arguments.of("match (n)\n- [r:FOO*] -> (m) RETURN r", 2, 1,
						"\tmatch (n)" + LINE_SEPARATOR + "\t- [r:FOO*] -> (m) RETURN r" + LINE_SEPARATOR + "\t^"
								+ LINE_SEPARATOR),
				Arguments.of("match (x0123456789) \nwith x0123456789\nmatch(n) - [r:FOO*] -> (m) RETURN r", 3, 10,
						"\tmatch (x0123456789) " + LINE_SEPARATOR + "\twith x0123456789" + LINE_SEPARATOR
								+ "\tmatch(n) - [r:FOO*] -> (m) RETURN r" + LINE_SEPARATOR + "\t         ^"
								+ LINE_SEPARATOR),
				Arguments.of("match (n)                  \n-        [r:FOO*] -> (m) \nRETURN r", 2, 1,
						"\tmatch (n)                  " + LINE_SEPARATOR + "\t-        [r:FOO*] -> (m) "
								+ LINE_SEPARATOR + "\t^" + LINE_SEPARATOR + "\tRETURN r" + LINE_SEPARATOR),
				Arguments.of("match (n) - [r] -> (m) RETURN r", null, null,
						"\tmatch (n) - [r] -> (m) RETURN r" + LINE_SEPARATOR));
	}

	@ParameterizedTest(name = "{index}: Notifications for \"{0}\"")
	@MethodSource("params")
	void shouldFormatNotifications(String query, @Nullable Integer line, @Nullable Integer column, String expected) {

		InputPosition inputPosition;
		if (line == null || column == null) {
			inputPosition = null;
		}
		else {
			inputPosition = mock(InputPosition.class);
			given(inputPosition.line()).willReturn(line);
			given(inputPosition.column()).willReturn(column);
		}

		// Mockito cannot mock this class: interface
		// org.neo4j.driver.summary.GqlNotification.
		// Sealed interfaces or abstract classes can't be mocked. Interfaces cannot be
		// instantiated and cannot be subclassed for mocking purposes.
		// Instead of mocking a sealed interface or an abstract class, a non-abstract
		// class can be mocked and used to represent the interface.
		GqlNotification notification = mock(InternalGqlNotification.class);
		given(notification.severity()).willReturn(Optional.of(NotificationSeverity.WARNING));
		given(notification.gqlStatus()).willReturn("KGQ.Warning");
		given(notification.classification()).willReturn(Optional.of(NotificationClassification.UNRECOGNIZED));
		given(notification.statusDescription()).willReturn("Das solltest Du besser nicht mehr machen.");
		given(notification.position()).willReturn(Optional.ofNullable(inputPosition));

		String formattedNotification = ResultSummaries.format(notification, query);
		assertThat(formattedNotification)
			.isEqualTo("Das solltest Du besser nicht mehr machen. (KGQ.Warning):" + LINE_SEPARATOR + expected);
	}

}
