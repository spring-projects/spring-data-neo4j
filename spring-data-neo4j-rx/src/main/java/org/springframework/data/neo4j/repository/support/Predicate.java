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
package org.springframework.data.neo4j.repository.support;

import static lombok.AccessLevel.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;
import static org.springframework.data.neo4j.core.schema.NodeDescription.*;
import static org.springframework.data.neo4j.repository.query.CypherAdapterUtils.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.StatementBuilder;
import org.springframework.data.neo4j.core.cypher.SymbolicName;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

/**
 * Support class for "query by example" executors.
 * <p/>
 * This wraps all information necessary to predicate a match: A root condition and actual parameters to fill
 * in formal parameters inside the condition.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(access = PRIVATE) final class Predicate {

	static <S> Predicate create(Neo4jMappingContext mappingContext, Example<S> example) {

		NodeDescription<?> probeNodeDescription = mappingContext.getRequiredNodeDescription(example.getProbeType());

		SymbolicName rootNode = Cypher.symbolicName(NAME_OF_ROOT_NODE);
		Collection<GraphPropertyDescription> graphProperties = probeNodeDescription.getGraphProperties();
		DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(example.getProbe());
		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcher.MatchMode mode = matcher.getMatchMode();
		ExampleMatcherAccessor matcherAccessor = new ExampleMatcherAccessor(matcher);

		Predicate predicate = new Predicate(probeNodeDescription);
		for (GraphPropertyDescription graphProperty : graphProperties) {

			// TODO Relationships are not traversed.

			String currentPath = graphProperty.getFieldName();
			if (matcherAccessor.isIgnoredPath(currentPath)) {
				continue;
			}

			boolean internalId = graphProperty.isIdProperty() && probeNodeDescription.useInternalIds();
			String propertyName = graphProperty.getPropertyName();

			ExampleMatcher.PropertyValueTransformer transformer = matcherAccessor
				.getValueTransformerForPath(currentPath);
			Optional<Object> optionalValue = transformer
				.apply(Optional.ofNullable(beanWrapper.getPropertyValue(currentPath)));

			if (!optionalValue.isPresent()) {
				if (!internalId && matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					predicate.add(mode, property(rootNode, propertyName).isNull());
				}
				continue;
			}

			if (graphProperty.isRelationship()) {
				log.error("Querying by example does not support traversing of relationships.");
			} else if (graphProperty.isIdProperty() && probeNodeDescription.useInternalIds()) {
				predicate
					.add(mode, createIdExpression(predicate.nodeDescription).isEqualTo(literalOf(optionalValue.get())));
			} else {
				Expression property = property(rootNode, propertyName);
				Expression parameter = parameter(propertyName);
				Condition condition = property.isEqualTo(parameter);

				if (String.class.equals(graphProperty.getActualType())) {

					if (matcherAccessor.isIgnoreCaseForPath(currentPath)) {
						property = Functions.toLower(property);
						parameter = Functions.toLower(parameter);
					}

					switch (matcherAccessor.getStringMatcherForPath(currentPath)) {
						case DEFAULT:
						case EXACT:
							// This needs to be recreated as both property and parameter might have changed above
							condition = property.isEqualTo(parameter);
							break;
						case CONTAINING:
							condition = property.contains(parameter);
							break;
						case STARTING:
							condition = property.startsWith(parameter);
							break;
						case ENDING:
							condition = property.endsWith(parameter);
							break;
						case REGEX:
							condition = property.matches(parameter);
							break;
						default:
							throw new IllegalArgumentException(
								"Unsupported StringMatcher " + matcherAccessor.getStringMatcherForPath(currentPath));
					}
				}
				predicate.add(mode, condition);
				predicate.parameters.put(propertyName, optionalValue.get());
			}
		}

		return predicate;
	}

	private final NodeDescription<?> nodeDescription;

	private Condition condition = Conditions.noCondition();

	private final Map<String, Object> parameters = new HashMap<>();

	StatementBuilder.OngoingReadingAndWith f(
		BiFunction<NodeDescription<?>, Optional<Condition>, StatementBuilder.OngoingReadingAndWith> ff) {
		return ff.apply(this.nodeDescription, Optional.of(this.condition));
	}

	private void add(ExampleMatcher.MatchMode matchMode, Condition additionalCondition) {

		switch (matchMode) {
			case ALL:
				this.condition = this.condition.and(additionalCondition);
				break;
			case ANY:
				this.condition = this.condition.or(additionalCondition);
				break;
			default:
				throw new IllegalArgumentException("Unsupported match mode: " + matchMode);
		}
	}

	public NodeDescription<?> getNodeDescription() {
		return nodeDescription;
	}

	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}
}
