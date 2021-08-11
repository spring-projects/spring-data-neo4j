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

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.driver.summary.InputPosition;
import org.neo4j.driver.summary.Notification;
import org.neo4j.driver.summary.Plan;
import org.neo4j.driver.summary.ResultSummary;

/**
 * Utility class for dealing with result summaries.
 *
 * @author Michael J. Simons
 * @soundtrack Fatoni & Dexter - Yo, Picasso
 * @since 6.0
 */
final class ResultSummaries {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	/**
	 * Does some post processing on the giving result summary, especially logging all notifications
	 * and potentially query plans.
	 *
	 * @param resultSummary The result summary to process
	 * @return The same, unmodified result summary.
	 */
	static ResultSummary process(ResultSummary resultSummary) {
		logNotifications(resultSummary);
		logPlan(resultSummary);
		return resultSummary;
	}

	private static void logNotifications(ResultSummary resultSummary) {

		if (resultSummary.notifications().isEmpty() || !Neo4jClient.cypherLog.isWarnEnabled()) {
			return;
		}

		String query = resultSummary.query().text();
		resultSummary.notifications()
				.forEach(notification -> {
					Consumer<String> log;
					switch (notification.severity()) {
						case "WARNING":
							log = Neo4jClient.cypherLog::warn;
							break;
						case "INFORMATION":
							log = Neo4jClient.cypherLog::info;
							break;
						default:
							log = Neo4jClient.cypherLog::debug;
					}
					log.accept(ResultSummaries.format(notification, query));
				});
	}

	/**
	 * Creates a formatted string for a notification issued for a given query.
	 *
	 * @param notification The notification to format
	 * @param forQuery     The query that caused the notification
	 * @return A formatted string
	 */
	static String format(Notification notification, String forQuery) {

		InputPosition position = notification.position();

		StringBuilder queryHint = new StringBuilder();
		String[] lines = forQuery.split("(\r\n|\n)");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			queryHint.append("\t").append(line).append(LINE_SEPARATOR);
			if (i + 1 == position.line()) {
				queryHint.append("\t").append(Stream.generate(() -> " ").limit(position.column() - 1)
						.collect(Collectors.joining())).append("^").append(System.lineSeparator());
			}
		}
		return String.format("%s: %s%n%s%s", notification.code(), notification.title(), queryHint,
				notification.description());
	}

	/**
	 * Logs the plan of the result summary if available and log level is at least debug.
	 *
	 * @param resultSummary The result summary that might contain a plan
	 */
	private static void logPlan(ResultSummary resultSummary) {

		if (!(resultSummary.hasPlan() && Neo4jClient.cypherLog.isDebugEnabled())) {
			return;
		}

		Consumer<String> log = Neo4jClient.cypherLog::debug;

		log.accept("Plan:");
		printPlan(log, resultSummary.plan(), 0);
	}

	private static void printPlan(Consumer<String> log, Plan plan, int level) {

		String tabs = Stream.generate(() -> "\t").limit(level).collect(Collectors.joining());
		log.accept(tabs + "operatorType: " + plan.operatorType());
		log.accept(tabs + "identifiers: " + String.join(",", plan.identifiers()));
		log.accept(tabs + "arguments: ");
		plan.arguments().forEach((k, v) -> log.accept(tabs + "\t" + k + "=" + v));
		if (!plan.children().isEmpty()) {
			log.accept(tabs + "children: ");
			for (Plan childPlan : plan.children()) {
				printPlan(log, childPlan, level + 1);
			}
		}
	}

	private ResultSummaries() {
	}
}
