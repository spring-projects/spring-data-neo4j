/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.cypher;

import java.util.ArrayList;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.Statement.SingleQuery;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.ExposesSet;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OrderableOngoingReadingAndWith;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingWithoutWhere;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingUnwind;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingUpdate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * The main entry point into the Cypher DSL.
 *
 * The Cypher Builder API is intended for framework usage to produce Cypher statements required for database operations.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Cypher {

	/**
	 * Create a new Node representation with at least one label, the "primary" label. This is required. All other labels
	 * are optional.
	 *
	 * @param primaryLabel     The primary label this node is identified by.
	 * @param additionalLabels Additional labels
	 * @return A new node representation
	 */
	public static Node node(String primaryLabel, String... additionalLabels) {

		return Node.create(primaryLabel, additionalLabels);
	}

	public static Node node(String primaryLabel, MapExpression properties, String... additionalLabels) {

		return Node.create(primaryLabel, properties, additionalLabels);
	}

	/**
	 * @return A node matching any node.
	 */
	public static Node anyNode() {
		return Node.create();
	}

	/**
	 * @return The {@code *} wildcard literal.
	 */
	public static Asterisk asterisk() {
		return Asterisk.INSTANCE;
	}

	/**
	 * @return A node matching any node with the symbolic the given {@code symbolicName}.
	 */
	public static Node anyNode(String symbolicName) {
		return Node.create().named(symbolicName);
	}

	/**
	 * Dereferences a property for a symbolic name, most likely pointing to a property container like a node or a relationship.
	 *
	 * @param containerName The symbolic name of a property container
	 * @param name          The name of the property to dereference
	 * @return A new property
	 */
	public static Property property(String containerName, String name) {
		return property(name(containerName), name);
	}

	/**
	 * Dereferences a property on a arbitrary expression.
	 *
	 * @param expression The expression that describes some sort of accessible map
	 * @param name       The name of the property to dereference
	 * @return A new property.
	 */
	public static Property property(Expression expression, String name) {
		return Property.create(expression, name);
	}

	/**
	 * Creates a new symbolic name.
	 *
	 * @param value The value of the symbolic name
	 * @return A new symbolic name
	 */
	public static SymbolicName name(String value) {

		return SymbolicName.create(value);
	}

	/**
	 * Creates a new parameter placeholder. Existing $-signs will be removed.
	 *
	 * @param name The name of the parameter, must not be null
	 * @return The new parameter
	 */
	public static Parameter parameter(String name) {
		return Parameter.create(name);
	}

	/**
	 * Prepares an optional match statement.
	 *
	 * @param pattern The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 */
	public static OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern) {

		return Statement.builder().optionalMatch(pattern);
	}

	/**
	 * Starts building a statement based on a match clause. Use {@link Cypher#node(String, String...)} and related to
	 * retrieve a node or a relationship, which both are pattern elements.
	 *
	 * @param pattern The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 */
	public static OngoingReadingWithoutWhere match(PatternElement... pattern) {

		return Statement.builder().match(pattern);
	}

	/**
	 * Starts building a statement based on a {@code CREATE} clause.
	 *
	 * @param pattern The patterns to create
	 * @return An ongoing {@code CREATE} that can be used to specify {@code WITH} and {@code RETURNING} etc.
	 */
	public static <T extends OngoingUpdate & ExposesSet> T create(PatternElement... pattern) {

		return Statement.builder().create(pattern);
	}

	/**
	 * Starts a statement with a leading {@code WITH}. Those are useful for passing on lists of various type that
	 * can be unwound later on etc. A leading {@code WITH} cannot be used with patterns obviously and needs its
	 * arguments to have an alias.
	 *
	 * @param expressions One ore more aliased expressions.
	 * @return An ongoing with clause.
	 */
	public static OrderableOngoingReadingAndWith with(AliasedExpression... expressions) {

		return Statement.builder().with(expressions);
	}

	/**
	 * Starts building a statement based on a {@code MERGE} clause.
	 *
	 * @param pattern The patterns to merge
	 * @return An ongoing {@code MERGE} that can be used to specify {@code WITH} and {@code RETURNING} etc.
	 */
	public static <T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern) {

		return Statement.builder().merge(pattern);
	}

	/**
	 * Starts building a statement starting with an {@code UNWIND} clause. The expression needs to be an expression
	 * evaluating to a list, otherwise the query will fail.
	 *
	 * @param expression The expression to unwind
	 * @return An ongoing {@code UNWIND}.
	 */
	public static OngoingUnwind unwind(Expression expression) {

		return Statement.builder().unwind(expression);
	}

	/**
	 * Starts building a statement starting with an {@code UNWIND} clause. The expressions passed will be turned into a
	 * list expression
	 * @param expressions expressions to unwind
	 * @return a new instance of {@link OngoingUnwind}
	 */
	public static OngoingUnwind unwind(Expression... expressions) {

		return Statement.builder().unwind(Cypher.listOf(expressions));
	}

	/**
	 * Creates a new {@link SortItem} to be used as part of an {@link Order}.
	 *
	 * @param expression The expression by which things should be sorted
	 * @return A sort item, providing means to specify ascending or descending order
	 */
	public static SortItem sort(Expression expression) {

		return SortItem.create(expression, null);
	}

	/**
	 * Creates a map of expression from a list of key/value pairs.
	 *
	 * @param keysAndValues A list of key and values. Must be an even number, with alternating {@link String} and {@link Expression}
	 * @return A new map expression.
	 */
	public static MapExpression mapOf(Object... keysAndValues) {

		return MapExpression.create(keysAndValues);
	}

	/**
	 * Creates a {@link ListExpression list-expression} from several expressions.
	 *
	 * @param expressions expressions to get combined into a list
	 * @return a new instance of {@link ListExpression}
	 */
	public static ListExpression listOf(Expression... expressions) {

		return ListExpression.create(expressions);
	}

	/**
	 * Creates a new {@link NullLiteral} from the given {@code object}.
	 *
	 * @param object the object to represent.
	 * @return a new {@link NullLiteral}.
	 * @throws IllegalArgumentException when the object cannot be represented as a literal
	 */
	public static Literal<?> literalOf(@Nullable Object object) {

		if (object == null) {
			return NullLiteral.INSTANCE;
		}
		if (object instanceof CharSequence) {
			return new StringLiteral((CharSequence) object);
		}
		if (object instanceof Number) {
			return new NumberLiteral((Number) object);
		}
		if (object instanceof Iterable) {
			return new ListLiteral((Iterable<Literal<?>>) object);
		}
		throw new IllegalArgumentException("Unsupported literal type: " + object.getClass());
	}

	/**
	 * @return The {@literal true} literal.
	 */
	public static Literal literalTrue() {
		return BooleanLiteral.TRUE;
	}

	/**
	 * @return The {@literal false} literal.
	 */
	public static Literal literalFalse() {
		return BooleanLiteral.FALSE;
	}

	public static Statement union(Statement... statement) {
		return unionImpl(false, statement);
	}

	public static Statement unionAll(Statement... statement) {
		return unionImpl(true, statement);
	}

	public static PatternComprehension.OngoingDefinition listBasedOn(Relationship pattern) {
		return PatternComprehension.basedOn(pattern);
	}

	public static PatternComprehension.OngoingDefinition listBasedOn(RelationshipChain pattern) {
		return PatternComprehension.basedOn(pattern);
	}

	/**
	 * Escapes and quotes the {@code unquotedString} for safe usage in Neo4j-Browser and Shell.
	 *
	 * @param unquotedString An unquoted string
	 * @return A quoted string with special chars escaped.
	 */
	public static String quote(String unquotedString) {
		return literalOf(unquotedString).asString();
	}

	private static Statement unionImpl(boolean unionAll, Statement... statements) {

		Assert.isTrue(statements != null && statements.length >= 2, "At least two statements are required!");

		int i = 0;
		UnionQuery existingUnionQuery = null;
		if (statements[0] instanceof UnionQuery) {
			existingUnionQuery = (UnionQuery) statements[0];
			Assert.isTrue(existingUnionQuery.isAll() == unionAll, "Cannot mix union and union all!");
			i = 1;
		}

		List<SingleQuery> listOfQueries = new ArrayList<>();
		do {
			Assert.isInstanceOf(SingleQuery.class, statements[i], "Can only union single queries!");
			listOfQueries.add((SingleQuery) statements[i]);
		} while (++i < statements.length);

		if (existingUnionQuery == null) {
			return UnionQuery.create(unionAll, listOfQueries);
		} else {
			return existingUnionQuery.addAdditionalQueries(listOfQueries);
		}
	}

	/**
	 * This is a literal copy of {@link javax.lang.model.SourceVersion#isIdentifier(CharSequence)} included here to
	 * be not dependent on the compiler module.
	 *
	 * @param name A possible Java identifier
	 * @return True, if {@code name} represents an identifier.
	 */
	static boolean isIdentifier(CharSequence name) {
		String id = name.toString();

		if (id.length() == 0) {
			return false;
		}
		int cp = id.codePointAt(0);
		if (!Character.isJavaIdentifierStart(cp)) {
			return false;
		}
		for (int i = Character.charCount(cp);
			 i < id.length();
			 i += Character.charCount(cp)) {
			cp = id.codePointAt(i);
			if (!Character.isJavaIdentifierPart(cp)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Not to be instantiated.
	 */
	private Cypher() {
	}
}
