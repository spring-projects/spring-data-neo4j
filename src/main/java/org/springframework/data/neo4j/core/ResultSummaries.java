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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.NotificationClassification;
import org.neo4j.driver.NotificationSeverity;
import org.neo4j.driver.summary.GqlNotification;
import org.neo4j.driver.summary.Plan;
import org.neo4j.driver.summary.ResultSummary;

import org.springframework.core.log.LogAccessor;

/**
 * Utility class for dealing with result summaries.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class ResultSummaries {

	private static final String LINE_SEPARATOR = System.lineSeparator();

	private static final LogAccessor cypherPerformanceNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.performance"));

	private static final LogAccessor cypherHintNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.hint"));

	private static final LogAccessor cypherUnrecognizedNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.unrecognized"));

	private static final LogAccessor cypherUnsupportedNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.unsupported"));

	private static final LogAccessor cypherDeprecationNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.deprecation"));

	private static final LogAccessor cypherGenericNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.generic"));

	private static final LogAccessor cypherSecurityNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.security"));

	private static final LogAccessor cypherTopologyNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.topology"));

	private static final LogAccessor cypherSchemaNotificationLog = new LogAccessor(
			LogFactory.getLog("org.springframework.data.neo4j.cypher.schema"));

	private static final List<Pattern> STUFF_THAT_MIGHT_INFORM_THAT_THE_ID_FUNCTION_IS_PROBLEMATIC = Stream.of(
			"(?im)The query used a deprecated function[.:] \\(?[`']id.+",
			"(?im).*id is deprecated and will be removed without a replacement\\.",
			"(?im).*feature deprecated with replacement\\. id is deprecated\\. It is replaced by elementId or consider using an application-generated id\\.")
		.map(Pattern::compile)
		.toList();

	private ResultSummaries() {
	}

	/**
	 * Does some post-processing on the giving result summary, especially logging all
	 * notifications and potentially query plans.
	 * @param resultSummary the result summary to process
	 * @return the same, unmodified result summary.
	 */
	static ResultSummary process(ResultSummary resultSummary) {
		logNotifications(resultSummary);
		logPlan(resultSummary);
		return resultSummary;
	}

	private static void logNotifications(ResultSummary resultSummary) {

		if (resultSummary.gqlStatusObjects().isEmpty() || !Neo4jClient.cypherLog.isWarnEnabled()) {
			return;
		}

		boolean supressIdDeprecations = Neo4jClient.SUPPRESS_ID_DEPRECATIONS.getAcquire();
		Predicate<GqlNotification> isDeprecationWarningForId;
		try {
			isDeprecationWarningForId = notification -> supressIdDeprecations
					&& notification.classification()
						.filter(cat -> cat == NotificationClassification.UNRECOGNIZED
								|| cat == NotificationClassification.DEPRECATION)
						.isPresent()
					&& STUFF_THAT_MIGHT_INFORM_THAT_THE_ID_FUNCTION_IS_PROBLEMATIC.stream()
						.anyMatch(p -> p.matcher(notification.statusDescription()).matches());
		}
		finally {
			Neo4jClient.SUPPRESS_ID_DEPRECATIONS.setRelease(supressIdDeprecations);
		}

		String query = resultSummary.query().text();
		resultSummary.gqlStatusObjects()
			.stream()
			.filter(GqlNotification.class::isInstance)
			.map(GqlNotification.class::cast)
			.filter(Predicate.not(isDeprecationWarningForId))
			.forEach(notification -> notification.severity().ifPresent(severityLevel -> {
				var category = notification.classification().orElse(null);

				var logger = getLogAccessor(category);
				Consumer<String> logFunction;
				if (severityLevel == NotificationSeverity.WARNING) {
					logFunction = logger::warn;
				}
				else if (severityLevel == NotificationSeverity.INFORMATION) {
					logFunction = logger::info;
				}
				else if (severityLevel == NotificationSeverity.OFF) {
					logFunction = (String message) -> {
					};
				}
				else {
					logFunction = logger::debug;
				}

				logFunction.accept(ResultSummaries.format(notification, query));
			}));
	}

	private static LogAccessor getLogAccessor(@Nullable NotificationClassification classification) {
		if (classification == null) {
			return Neo4jClient.cypherLog;
		}

		return switch (classification) {
			case HINT -> cypherHintNotificationLog;
			case UNRECOGNIZED -> cypherUnrecognizedNotificationLog;
			case UNSUPPORTED -> cypherUnsupportedNotificationLog;
			case PERFORMANCE -> cypherPerformanceNotificationLog;
			case DEPRECATION -> cypherDeprecationNotificationLog;
			case SECURITY -> cypherSecurityNotificationLog;
			case TOPOLOGY -> cypherTopologyNotificationLog;
			case GENERIC -> cypherGenericNotificationLog;
			case SCHEMA -> cypherSchemaNotificationLog;
		};
	}

	/**
	 * Creates a formatted string for a notification issued for a given query.
	 * @param notification the notification to format
	 * @param forQuery the query that caused the notification
	 * @return a formatted string
	 */
	static String format(GqlNotification notification, String forQuery) {

		var position = notification.position().orElse(null);

		StringBuilder queryHint = new StringBuilder();
		String[] lines = forQuery.split("(\r\n|\n)");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			queryHint.append("\t").append(line).append(LINE_SEPARATOR);
			if (position != null && i + 1 == position.line()) {
				queryHint.append("\t")
					.append(Stream.generate(() -> " ").limit(position.column() - 1).collect(Collectors.joining()))
					.append("^")
					.append(System.lineSeparator());
			}
		}
		return String.format("%s (%s):%n%s", notification.statusDescription(), notification.gqlStatus(), queryHint);
	}

	/**
	 * Logs the plan of the result summary if available and log level is at least debug.
	 * @param resultSummary the result summary that might contain a plan
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

}
