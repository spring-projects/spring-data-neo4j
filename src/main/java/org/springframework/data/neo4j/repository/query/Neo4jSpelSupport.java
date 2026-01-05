/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import java.io.Serial;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.support.schema_name.SchemaNames;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueEvaluationContext;
import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

/**
 * This class provides a couple of extensions to the Spring Data Neo4j SpEL support. Its
 * static functions are registered inside an
 * {@link org.springframework.data.spel.spi.EvaluationContextExtension} that in turn will
 * be provided as a root bean.
 *
 * @author Michael J. Simons
 * @since 6.0.2
 */
@API(status = API.Status.INTERNAL, since = "6.0.2")
public final class Neo4jSpelSupport {

	private static final String EXPRESSION_PARAMETER = "$1#{";

	private static final String QUOTED_EXPRESSION_PARAMETER = "$1__HASH__{";

	private static final String ENTITY_NAME = "staticLabels";

	private static final String ENTITY_NAME_VARIABLE = "#" + ENTITY_NAME;

	private static final String ENTITY_NAME_VARIABLE_EXPRESSION = "#{" + ENTITY_NAME_VARIABLE + "}";

	private static final Pattern EXPRESSION_PARAMETER_QUOTING = Pattern
		.compile("([:?])#\\{(?!" + ENTITY_NAME_VARIABLE + ")");

	private static final Pattern EXPRESSION_PARAMETER_UNQUOTING = Pattern.compile("([:?])__HASH__\\{");

	/**
	 * Constant under which literal functions are registered.
	 */
	public static String FUNCTION_LITERAL = "literal";

	/**
	 * Constant for the {@code anyOf} expression.
	 */
	public static String FUNCTION_ANY_OF = "anyOf";

	/**
	 * Constant for the {@code allOf} expression.
	 */
	public static String FUNCTION_ALL_OF = "allOf";

	/**
	 * Constant for the {@code orderBy} expression.
	 */
	public static String FUNCTION_ORDER_BY = "orderBy";

	private Neo4jSpelSupport() {
	}

	/**
	 * Takes {@code arg} and tries to either extract a {@link Sort sort} from it or cast
	 * it to a sort. That sort is than past to the {@link CypherGenerator} that renders a
	 * valid order by fragment which replaces the SpEL placeholder without further
	 * validation whether it's attributes are in the query or similar literal.
	 * @param arg the {@link Sort sort object} to order the result set of the final query.
	 * @return a literal replacement for a SpEL placeholder
	 */
	public static LiteralReplacement orderBy(Object arg) {

		Sort sort = null;
		if (arg instanceof Pageable v) {
			sort = v.getSort();
		}
		else if (arg instanceof Sort v) {
			sort = v;
		}
		else if (arg != null) {
			throw new IllegalArgumentException(arg.getClass() + " is not a valid order criteria");
		}
		return StringBasedLiteralReplacement.withTargetAndValue(LiteralReplacement.Target.SORT,
				CypherGenerator.INSTANCE.createOrderByFragment(sort));
	}

	/**
	 * Turns the arguments of this function into a literal replacement for the SpEL
	 * placeholder (instead of creating Cypher parameters).
	 * @param arg the object that will be inserted as a literal String into the query.
	 * It's {@code toString()} method will be used.
	 * @return a literal replacement for a SpEL placeholder
	 */
	public static LiteralReplacement literal(Object arg) {

		return StringBasedLiteralReplacement.withTargetAndValue(LiteralReplacement.Target.UNSPECIFIED,
				(arg != null) ? arg.toString() : "");
	}

	public static LiteralReplacement anyOf(Object arg) {
		return labels(arg, "|");
	}

	public static LiteralReplacement allOf(Object arg) {
		return labels(arg, "&");
	}

	private static LiteralReplacement labels(Object arg, String joinOn) {
		return StringBasedLiteralReplacement.withTargetAndValue(LiteralReplacement.Target.UNSPECIFIED,
				(arg != null) ? joinStrings(arg, joinOn) : "");
	}

	private static String joinStrings(Object arg, String joinOn) {
		if (arg instanceof Collection) {
			return ((Collection<?>) arg).stream()
				.map(o -> SchemaNames.sanitize(o.toString()).orElseThrow())
				.collect(Collectors.joining(joinOn));
		}

		// we are so kind and also accept plain strings instead of collection<string>
		if (arg instanceof String) {
			return (String) arg;
		}

		throw new IllegalArgumentException(String.format(
				"Cannot process argument %s. Please note that only Collection<String> and String are supported types.",
				arg));
	}

	/**
	 * Renders a query that may contains SpEL expressions.
	 * @param query the query expression potentially containing a SpEL expression. Must
	 * not be {@literal null}.
	 * @param mappingContext the mapping context in which the query is rendered
	 * @param metadata the {@link Neo4jPersistentEntity} for the given entity. Must not be
	 * {@literal null}.
	 * @param parser must not be {@literal null}.
	 * @return a query in which some SpEL expression have been replaced with the result of
	 * evaluating the expression
	 */
	public static String renderQueryIfExpressionOrReturnQuery(String query, Neo4jMappingContext mappingContext,
			EntityMetadata<?> metadata, ValueExpressionParser parser) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(metadata, "metadata must not be null");
		Assert.notNull(parser, "parser must not be null");

		if (!containsExpression(query)) {
			return query;
		}

		ValueEvaluationContext evalContext = ValueEvaluationContext.of(new StandardEnvironment(),
				new StandardEvaluationContext());
		Neo4jPersistentEntity<?> requiredPersistentEntity = mappingContext
			.getRequiredPersistentEntity(metadata.getJavaType());
		evalContext.getEvaluationContext()
			.setVariable(ENTITY_NAME,
					requiredPersistentEntity.getStaticLabels()
						.stream()
						.map(l -> SchemaNames.sanitize(l, true).orElseThrow())
						.collect(Collectors.joining(":")));

		query = potentiallyQuoteExpressionsParameter(query);

		ValueExpression expr = parser.parse(query);

		String result = (String) expr.evaluate(evalContext);

		if (result == null) {
			return query;
		}

		return potentiallyUnquoteParameterExpressions(result);
	}

	static String potentiallyUnquoteParameterExpressions(String result) {
		return EXPRESSION_PARAMETER_UNQUOTING.matcher(result).replaceAll(EXPRESSION_PARAMETER);
	}

	static String potentiallyQuoteExpressionsParameter(String query) {
		return EXPRESSION_PARAMETER_QUOTING.matcher(query).replaceAll(QUOTED_EXPRESSION_PARAMETER);
	}

	private static boolean containsExpression(String query) {
		return query.contains(ENTITY_NAME_VARIABLE_EXPRESSION);
	}

	/**
	 * A marker interface that indicates a literal replacement in a query instead of a
	 * parameter replacement. This comes in handy in places where non-parameterizable
	 * things should be created dynamic, for example matching on set of dynamic labels,
	 * types order ordering in a dynamic way.
	 */
	public interface LiteralReplacement {

		String getValue();

		Target getTarget();

		/**
		 * The target of this replacement. While a replacement can be used theoretically
		 * everywhere in the query, the target can be used to infer a dedicated meaning of
		 * this replacement.
		 */
		enum Target {

			/**
			 * Replaces the sort fragment.
			 */
			SORT,
			/**
			 * Unspecified target.
			 */
			UNSPECIFIED

		}

	}

	private static final class StringBasedLiteralReplacement implements LiteralReplacement {

		/**
		 * Default number of cached instances.
		 */
		private static final int DEFAULT_CACHE_SIZE = 16;

		/**
		 * A small cache of instances of replacements. The cache key is the literal string
		 * value. Done to avoid the creation of too many small objects.
		 */
		private static final Map<String, LiteralReplacement> INSTANCES = new LinkedHashMap<>(DEFAULT_CACHE_SIZE) {
			@Serial
			private static final long serialVersionUID = 195460174410223375L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, LiteralReplacement> eldest) {
				return size() > DEFAULT_CACHE_SIZE;
			}
		};

		private static final StampedLock LOCK = new StampedLock();

		private final Target target;

		private final String value;

		private StringBasedLiteralReplacement(Target target, String value) {
			this.target = target;
			this.value = value;
		}

		static LiteralReplacement withTargetAndValue(LiteralReplacement.Target target, @Nullable String value) {

			String valueUsed = (value != null) ? value : "";
			String key = target.name() + "_" + valueUsed;

			long stamp = LOCK.tryOptimisticRead();
			if (LOCK.validate(stamp) && INSTANCES.containsKey(key)) {
				return INSTANCES.get(key);
			}
			try {
				stamp = LOCK.readLock();
				LiteralReplacement replacement = null;
				while (replacement == null) {
					if (INSTANCES.containsKey(key)) {
						replacement = INSTANCES.get(key);
					}
					else {
						long writeStamp = LOCK.tryConvertToWriteLock(stamp);
						if (LOCK.validate(writeStamp)) {
							replacement = new StringBasedLiteralReplacement(target, valueUsed);
							stamp = writeStamp;
							INSTANCES.put(key, replacement);
						}
						else {
							LOCK.unlockRead(stamp);
							stamp = LOCK.writeLock();
						}
					}
				}
				return replacement;
			}
			finally {
				LOCK.unlock(stamp);
			}
		}

		@Override
		public String getValue() {
			return this.value;
		}

		@Override
		public Target getTarget() {
			return this.target;
		}

	}

}
