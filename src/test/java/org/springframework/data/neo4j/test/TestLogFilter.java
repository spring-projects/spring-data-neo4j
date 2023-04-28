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
package org.springframework.data.neo4j.test;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.List;

/**
 * @author Gerrit Meier
 */
public class TestLogFilter extends Filter<ILoggingEvent> {

	private static final List<String> FILTER_MESSAGES = List.of(
			"ClientNotification.Statement.UnknownRelationshipTypeWarning",
			"ClientNotification.Statement.UnknownPropertyKeyWarning"
	);

	@Override
	public FilterReply decide(ILoggingEvent event) {

		String message = event.getMessage();

		for (String filterMessage : FILTER_MESSAGES) {
			if (message.contains(filterMessage)) {
				return FilterReply.DENY;
			}
		}

		return FilterReply.ACCEPT;
	}
}
