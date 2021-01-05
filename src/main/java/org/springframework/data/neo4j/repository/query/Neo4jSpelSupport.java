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
package org.springframework.data.neo4j.repository.query;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apiguardian.api.API;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.lang.Nullable;

/**
 * This class provides a couple of extensions to the Spring Data Neo4j SpEL support and is registered by
 * the appropriate repository factories as a root bean.
 *
 * @author Michael J. Simons
 * @soundtrack Red Hot Chili Peppers - Californication
 * @since 6.0.2
 */
@API(status = API.Status.INTERNAL, since = "6.0.2")
public final class Neo4jSpelSupport {

	public static String FUNCTION_LITERAL = "literal";
	public static String FUNCTION_ORDER_BY = "orderBy";

	/**
	 * Takes {@code arg} and tries to either extract a {@link Sort sort} from it or cast it to a sort.  That sort is
	 * than past to the {@link CypherGenerator} that renders a valid order by fragment which replaces the SpEL placeholder
	 * without further validation whether it's attributes are in the query or similar literal.
	 *
	 * @param arg The {@link Sort sort object} to order the result set of the final query.
	 * @return A literal replacement for a SpEL placeholder
	 */
	public static LiteralReplacement orderBy(@Nullable Object arg) {

		Sort sort = null;
		if (arg instanceof Pageable) {
			sort = ((Pageable) arg).getSort();
		} else if (arg instanceof Sort) {
			sort = (Sort) arg;
		} else if (arg != null) {
			throw new IllegalArgumentException(arg.getClass() + " is not a valid order criteria.");
		}
		return StringBasedLiteralReplacement.withTargetAndValue(LiteralReplacement.Target.SORT,
				CypherGenerator.INSTANCE.createOrderByFragment(sort));
	}

	/**
	 * Turns the arguments of this function into a literal replacement for the SpEL placeholder (instead of creating
	 * Cypher parameters).
	 *
	 * @param arg The object that will be inserted as a literal String into the query. It's {@code toString()} method will be used.
	 * @return A literal replacement for a SpEL placeholder
	 */
	public static LiteralReplacement literal(@Nullable Object arg) {

		LiteralReplacement literalReplacement = StringBasedLiteralReplacement
				.withTargetAndValue(LiteralReplacement.Target.UNSPECIFIED, arg == null ? "" : arg.toString());
		return literalReplacement;
	}

	/**
	 * A marker interface that indicates a literal replacement in a query instead of a parameter replacement. This
	 * comes in handy in places where non-parameterizable things should be created dynamic, for example matching on
	 * set of dynamic labels, types order ordering in a dynamic way.
	 */
	interface LiteralReplacement {

		/**
		 * The target of this replacement. While a replacement can be used theoretically everywhere in the query, the target
		 * can be used to infer a dedicated meaning of this replacement.
		 */
		enum Target { SORT, UNSPECIFIED }

		String getValue();

		Target getTarget();
	}

	private static class StringBasedLiteralReplacement implements LiteralReplacement {

		/**
		 * Default number of cached instances.
		 */
		private static final int DEFAULT_CACHE_SIZE = 16;

		/**
		 * A small cache of instances of replacements. The cache key is the literal string value. Done to avoid
		 * the creation of too many small objects.
		 */
		private static final Map<String, LiteralReplacement> INSTANCES =
				new LinkedHashMap<String, LiteralReplacement>(DEFAULT_CACHE_SIZE) {
					@Override
					protected boolean removeEldestEntry(Map.Entry<String, LiteralReplacement> eldest) {
						return size() > DEFAULT_CACHE_SIZE;
					}
				};

		static LiteralReplacement withTargetAndValue(LiteralReplacement.Target target, @Nullable String value) {

			String valueUsed = value == null ? "" : value;
			StringBuilder key = new StringBuilder(target.name()).append("_").append(valueUsed);

			return INSTANCES.computeIfAbsent(key.toString(), k -> new StringBasedLiteralReplacement(target, valueUsed));
		}

		private final Target target;
		private final String value;

		private StringBasedLiteralReplacement(Target target, String value) {
			this.target = target;
			this.value = value;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public Target getTarget() {
			return target;
		}
	}

}
