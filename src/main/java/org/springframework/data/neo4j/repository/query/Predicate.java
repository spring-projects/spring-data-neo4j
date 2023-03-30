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
package org.springframework.data.neo4j.repository.query;

import static org.neo4j.cypherdsl.core.Cypher.literalOf;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.neo4j.cypherdsl.core.Cypher.property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.neo4j.core.Neo4jPropertyValueTransformers;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;

/**
 * Support class for "query by example" executors.
 * <p>
 * This wraps all information necessary to predicate a match: A root condition and actual parameters to fill in formal
 * parameters inside the condition.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class Predicate {

	static <S> Predicate create(Neo4jMappingContext mappingContext, Example<S> example) {

		Neo4jPersistentEntity<?> probeNodeDescription = mappingContext.getRequiredPersistentEntity(example.getProbeType());

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

			boolean internalId = graphProperty.isIdProperty() && probeNodeDescription.isUsingInternalIds();
			String propertyName = graphProperty.getPropertyName();

			ExampleMatcher.PropertyValueTransformer transformer = matcherAccessor
					.getValueTransformerForPath(currentPath);
			Optional<Object> optionalValue = transformer
					.apply(Optional.ofNullable(beanWrapper.getPropertyValue(currentPath)));

			if (optionalValue.isEmpty()) {
				if (!internalId && matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					predicate.add(mode, property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(probeNodeDescription), propertyName).isNull());
				}
				continue;
			}

			Neo4jConversionService conversionService = mappingContext.getConversionService();

			var theValue = optionalValue.map(v -> v instanceof Neo4jPropertyValueTransformers.NegatedValue negatedValue ? negatedValue.value() : v).get();
			Condition condition = null;

			if (graphProperty.isRelationship()) {
				Neo4jQuerySupport.REPOSITORY_QUERY_LOG.error("Querying by example does not support traversing of relationships.");
			} else if (graphProperty.isIdProperty() && probeNodeDescription.isUsingInternalIds()) {
				condition = predicate.neo4jPersistentEntity.getIdExpression().isEqualTo(literalOf(theValue));
			} else {
				Expression property = property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(probeNodeDescription), propertyName);
				Expression parameter = parameter(propertyName);
				condition = property.isEqualTo(parameter);

				if (String.class.equals(graphProperty.getActualType())) {

					if (matcherAccessor.isIgnoreCaseForPath(currentPath)) {
						property = Functions.toLower(property);
						parameter = Functions.toLower(parameter);
					}

					condition = switch (matcherAccessor.getStringMatcherForPath(currentPath)) {
						case DEFAULT, EXACT -> property.isEqualTo(parameter);
						case CONTAINING -> property.contains(parameter);
						case STARTING -> property.startsWith(parameter);
						case ENDING -> property.endsWith(parameter);
						case REGEX -> property.matches(parameter);
					};
				}

				Neo4jPersistentProperty neo4jPersistentProperty = (Neo4jPersistentProperty) graphProperty;
				predicate.parameters.put(propertyName, conversionService.writeValue(theValue,
						neo4jPersistentProperty.getTypeInformation(), neo4jPersistentProperty.getOptionalConverter()));
			}
			if (condition != null) {
				predicate.add(mode, postProcess(condition, optionalValue.get()));
			}
		}

		return predicate;
	}

	private static Condition postProcess(Condition condition, Object transformedValue) {
		if (transformedValue instanceof Neo4jPropertyValueTransformers.NegatedValue) {
			return condition.not();
		}
		return condition;
	}

	private final Neo4jPersistentEntity neo4jPersistentEntity;

	private Condition condition = Conditions.noCondition();

	private final Map<String, Object> parameters = new HashMap<>();

	private Predicate(Neo4jPersistentEntity neo4jPersistentEntity) {
		this.neo4jPersistentEntity = neo4jPersistentEntity;
	}

	public Condition getCondition() {
		return condition;
	}

	StatementBuilder.OrderableOngoingReadingAndWith useWithReadingFragment(
			BiFunction<NodeDescription<?>, Condition, StatementBuilder.OrderableOngoingReadingAndWith> readingFragmentSupplier) {
		return readingFragmentSupplier.apply(this.neo4jPersistentEntity, this.condition);
	}

	private void add(ExampleMatcher.MatchMode matchMode, Condition additionalCondition) {

		this.condition = switch (matchMode) {
			case ALL -> this.condition.and(additionalCondition);
			case ANY -> this.condition.or(additionalCondition);
		};
	}

	public NodeDescription<?> getNeo4jPersistentEntity() {
		return neo4jPersistentEntity;
	}

	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}
}
