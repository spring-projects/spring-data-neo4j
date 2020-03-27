/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.cypher.renderer;

import static java.util.stream.Collectors.*;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.springframework.data.core.cypher.*;
import org.neo4j.springframework.data.core.cypher.support.ReflectiveVisitor;
import org.neo4j.springframework.data.core.cypher.support.TypedSubtree;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.springframework.lang.Nullable;

/**
 * This is a simple (some would call it naive) implementation of a visitor to the Cypher AST created by the Cypher builder
 * based on the {@link ReflectiveVisitor reflective visitor}.
 * <p>
 * It takes care of separating elements of sub trees containing the element type with a separator and provides pairs of
 * {@code enter} / {@code leave} for the structuring elements of the Cypher AST as needed.
 * <p>
 * This rendering visitor is not meant to be used outside framework code and we don't give any guarantees on the format
 * being output apart from that it works within the constraints of SDN-RX.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
class RenderingVisitor extends ReflectiveVisitor {

	private static final Pattern LABEL_AND_TYPE_QUOTATION = Pattern.compile("`");

	/**
	 * Target of all rendering.
	 */
	private final StringBuilder builder = new StringBuilder();

	/**
	 * Optional separator between elements.
	 */
	private String separator = null;

	/**
	 * This keeps track on which level of the tree a separator is needed.
	 */
	private final java.util.Set<Integer> separatorOnLevel = new HashSet<>();

	/**
	 * Keeps track of named objects that have been already visited.
	 */
	private final java.util.Set<Named> visitedNamed = new HashSet<>();

	/**
	 * The current level in the tree of cypher elements.
	 */
	private int currentLevel = 0;

	/**
	 * Will be set to true when entering an already visited node.
	 */
	private boolean skipNodeContent = false;

	private void enableSeparator(int level, boolean on) {
		if (on) {
			separatorOnLevel.add(level);
		} else {
			separatorOnLevel.remove(level);
		}
		this.separator = null;
	}

	private boolean needsSeparator() {
		return separatorOnLevel.contains(currentLevel);
	}

	@Override
	protected boolean preEnter(Visitable visitable) {

		if (skipNodeContent) {
			return false;
		}

		int nextLevel = ++currentLevel + 1;
		if (visitable instanceof TypedSubtree) {
			enableSeparator(nextLevel, true);
		}

		if (needsSeparator() && separator != null) {
			builder.append(separator);
			separator = null;
		}

		return !skipNodeContent;
	}

	@Override
	protected void postLeave(Visitable visitable) {

		if (needsSeparator()) {
			separator = ", ";
		}

		if (visitable instanceof TypedSubtree) {
			enableSeparator(currentLevel + 1, false);
		}

		--currentLevel;
	}

	void enter(Match match) {
		if (match.isOptional()) {
			builder.append("OPTIONAL ");
		}
		builder.append("MATCH ");
	}

	void leave(Match match) {
		builder.append(" ");
	}

	void enter(Where where) {
		builder.append(" WHERE ");
	}

	void enter(Create create) {
		builder.append("CREATE ");
	}

	void leave(Create create) {
		builder.append(" ");
	}

	void enter(Merge merge) {
		builder.append("MERGE ");
	}

	void leave(Merge merge) {
		builder.append(" ");
	}

	void enter(Return returning) {
		builder.append("RETURN ");
		if (returning.isDistinct()) {
			builder.append("DISTINCT ");
		}
	}

	void enter(With with) {
		builder.append("WITH ");
		if (with.isDistinct()) {
			builder.append("DISTINCT ");
		}
	}

	void leave(With with) {
		builder.append(" ");
	}

	void enter(Delete delete) {

		if (delete.isDetach()) {
			builder.append("DETACH ");
		}

		builder.append("DELETE ");
	}

	void leave(Delete match) {
		builder.append(" ");
	}

	void leave(AliasedExpression aliased) {
		builder.append(" AS ").append(aliased.getAlias());
	}

	void enter(NestedExpression nested) {
		builder.append("(");
	}

	void leave(NestedExpression nested) {
		builder.append(")");
	}

	void enter(Order order) {
		builder.append(" ORDER BY ");
	}

	void enter(Skip skip) {
		builder.append(" SKIP ");
	}

	void enter(Limit limit) {
		builder.append(" LIMIT ");
	}

	void enter(SortItem.Direction direction) {
		builder
			.append(" ")
			.append(direction.getSymbol());
	}

	void enter(PropertyLookup propertyLookup) {
		builder
			.append(".")
			.append(propertyLookup.getPropertyKeyName());
	}

	void enter(FunctionInvocation functionInvocation) {
		builder
			.append(functionInvocation.getFunctionName())
			.append("(");
	}

	void leave(FunctionInvocation functionInvocation) {
		builder
			.append(")");
	}

	void enter(Operation operation) {

		if (operation.needsGrouping()) {
			builder.append("(");
		}
	}

	void enter(Operator operator) {

		Operator.Type type = operator.getType();
		if (type == Operator.Type.LABEL) {
			return;
		}
		if (type != Operator.Type.PREFIX) {
			builder.append(" ");
		}
		builder.append(operator.getRepresentation());
		if (type != Operator.Type.POSTFIX) {
			builder.append(" ");
		}
	}

	void leave(Operation operation) {

		if (operation.needsGrouping()) {
			builder.append(")");
		}
	}

	void enter(CompoundCondition compoundCondition) {
		builder.append("(");
	}

	void leave(CompoundCondition compoundCondition) {
		builder.append(")");
	}

	void enter(Literal<?> expression) {
		builder.append(expression.asString());
	}

	void enter(Node node) {

		builder.append("(");

		// This is only relevant for nodes in relationships.
		// Otherwise all the labels would be rendered again.
		node.getSymbolicName().map(SymbolicName::getName).ifPresent(symbolicName -> {
			skipNodeContent = visitedNamed.contains(node);
			visitedNamed.add(node);

			if (skipNodeContent) {
				builder.append(symbolicName);
			}
		});
	}

	void leave(Node node) {

		builder.append(")");

		skipNodeContent = false;
	}

	void enter(NodeLabel nodeLabel) {

		escapeName(nodeLabel.getValue()).ifPresent(label -> builder.append(":").append(label));
	}

	void enter(Properties properties) {

		builder.append(" ");
	}

	void enter(SymbolicName symbolicName) {
		builder.append(symbolicName.getName());
	}

	void enter(RelationshipDetail details) {

		Relationship.Direction direction = details.getDirection();
		builder
			.append(direction.getSymbolLeft())
			.append("[");
	}

	void enter(RelationshipTypes types) {

		builder
			.append(types.getValues().stream()
				.map(RenderingVisitor::escapeName)
				.map(Optional::get).collect(joining("|", ":", "")));
	}

	void enter(RelationshipLength length) {

		Integer minimum = length.getMinimum();
		Integer maximum = length.getMaximum();

		if (minimum == null && maximum == null) {
			return;
		}

		builder.append("*");
		if (minimum != null) {
			builder.append(minimum);
		}
		builder.append("..");
		if (maximum != null) {
			builder.append(maximum);
		}
	}

	void leave(RelationshipDetail details) {

		Relationship.Direction direction = details.getDirection();
		builder
			.append("]")
			.append(direction.getSymbolRight());
	}

	void enter(Parameter parameter) {

		builder.append("$").append(parameter.getName());
	}

	void enter(MapExpression map) {

		builder.append("{");
	}

	void enter(KeyValueMapEntry map) {

		builder.append(map.getKey()).append(": ");
	}

	void leave(MapExpression map) {

		builder.append("}");
	}

	void enter(ListExpression list) {

		builder.append("[");
	}

	void leave(ListExpression list) {

		builder.append("]");
	}

	void enter(Unwind unwind) {

		builder.append("UNWIND ");
	}

	void leave(Unwind unwind) {

		builder.append(" AS ")
			.append(unwind.getVariable())
			.append(" ");
	}

	void enter(UnionPart unionPart) {

		builder.append(" UNION ");
		if (unionPart.isAll()) {
			builder.append("ALL ");
		}
	}
	void enter(Set set) {

		builder.append("SET ");
	}

	void leave(Set set) {
		builder.append(" ");
	}

	void enter(Remove remove) {

		builder.append("REMOVE ");
	}

	void leave(Remove remove) {
		builder.append(" ");
	}

	void enter(PatternComprehension patternComprehension) {
		builder.append("[");
	}

	void leave(PatternComprehension patternComprehension) {
		builder.append("]");
	}

	public String getRenderedContent() {
		return this.builder.toString();
	}

	/**
	 * Escapes a symbolic name. Such a symbolic name is either used for a nodes label, the type of a relationship or a
	 * variable.
	 *
	 * @param unescapedName The name to escape.
	 * @return An empty optional when the unescaped name is {@literal null}, otherwise the correctly escaped name, safe to be used in statements.
	 */
	static Optional<String> escapeName(@Nullable CharSequence unescapedName) {

		if (unescapedName == null) {
			return Optional.empty();
		}

		Matcher matcher = LABEL_AND_TYPE_QUOTATION.matcher(unescapedName);
		return Optional.of(String.format(Locale.ENGLISH, "`%s`", matcher.replaceAll("``")));
	}
}
