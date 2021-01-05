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
package org.springframework.data.neo4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.driver.summary.InputPosition;
import org.neo4j.driver.summary.Notification;

/**
 * @author Michael J. Simons
 * @soundtrack Fatoni & Dexter - Yo, Picasso
 */
class ResultSummariesTest {

	private static Stream<Arguments> params() {
		return Stream.of(
				Arguments.of("match (n) - [r:FOO*] -> (m) RETURN r", 1, 19, ""
						+ "\tmatch (n) - [r:FOO*] -> (m) RETURN r\n"
						+ "\t                  ^\n"),
				Arguments.of("match (n)\n- [r:FOO*] -> (m) RETURN r", 2, 1, ""
						+ "\tmatch (n)\n"
						+ "\t- [r:FOO*] -> (m) RETURN r\n"
						+ "\t^\n"),
				Arguments.of("match (x0123456789) \nwith x0123456789\nmatch(n) - [r:FOO*] -> (m) RETURN r", 3, 10, ""
						+ "\tmatch (x0123456789) \n"
						+ "\twith x0123456789\n"
						+ "\tmatch(n) - [r:FOO*] -> (m) RETURN r\n"
						+ "\t         ^\n"),
				Arguments.of("match (n)                  \n-        [r:FOO*] -> (m) \nRETURN r", 2, 1, ""
						+ "\tmatch (n)                  \n"
						+ "\t-        [r:FOO*] -> (m) \n"
						+ "\t^\n"
						+ "\tRETURN r\n")
		);
	}

	@ParameterizedTest(name = "{index}: Notifications for \"{0}\"")
	@MethodSource("params")
	void shouldFormatNotifications(String query, int line, int column, String expected) {

		InputPosition inputPosition = mock(InputPosition.class);
		when(inputPosition.line()).thenReturn(line);
		when(inputPosition.column()).thenReturn(column);

		Notification notification = mock(Notification.class);
		when(notification.severity()).thenReturn("WARNING");
		when(notification.code()).thenReturn("KGQ.Warning");
		when(notification.title()).thenReturn("Das ist keine gute Query.");
		when(notification.description()).thenReturn("Das solltest Du besser nicht mehr machen.");
		when(notification.position()).thenReturn(inputPosition);

		String formattedNotification = ResultSummaries.format(notification, query);
		assertThat(formattedNotification).isEqualTo("KGQ.Warning: Das ist keine gute Query.\n"
				+ expected
				+ "Das solltest Du besser nicht mehr machen.");
	}
}
