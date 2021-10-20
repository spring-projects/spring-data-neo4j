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
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
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

			if (!optionalValue.isPresent()) {
				if (!internalId && matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
					predicate.add(mode, property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(probeNodeDescription), propertyName).isNull());
				}
				continue;
			}

			Neo4jConversionService conversionService = mappingContext.getConversionService();

			if (graphProperty.isRelationship()) {
				Neo4jQuerySupport.REPOSITORY_QUERY_LOG.error("Querying by example does not support traversing of relationships.");
			} else if (graphProperty.isIdProperty() && probeNodeDescription.isUsingInternalIds()) {
				predicate.add(mode,
						predicate.neo4jPersistentEntity.getIdExpression().isEqualTo(literalOf(optionalValue.get())));
			} else {
				Expression property = property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(probeNodeDescription), propertyName);
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
									"Unsupported StringMatcher " + matcherAccessor
											.getStringMatcherForPath(currentPath));
					}
				}
				predicate.add(mode, condition);
				predicate.parameters.put(propertyName, optionalValue.map(
						v -> {
							Neo4jPersistentProperty neo4jPersistentProperty = (Neo4jPersistentProperty) graphProperty;
							return conversionService.writeValue(v, neo4jPersistentProperty.getTypeInformation(),
											neo4jPersistentProperty.getOptionalConverter());
						})
						.get());
			}
		}

		return predicate;
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

	public NodeDescription<?> getNeo4jPersistentEntity() {
		return neo4jPersistentEntity;
	}

	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}
}
