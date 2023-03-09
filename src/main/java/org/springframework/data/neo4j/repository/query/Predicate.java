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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.GraphPropertyDescription;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.support.ExampleMatcherAccessor;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.lang.Nullable;

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

		Neo4jPersistentEntity<?> nodeDescription = mappingContext.getRequiredPersistentEntity(example.getProbeType());

		Collection<GraphPropertyDescription> graphProperties = nodeDescription.getGraphProperties();
		DirectFieldAccessFallbackBeanWrapper beanWrapper = new DirectFieldAccessFallbackBeanWrapper(example.getProbe());
		ExampleMatcher matcher = example.getMatcher();
		ExampleMatcher.MatchMode mode = matcher.getMatchMode();
		ExampleMatcherAccessor matcherAccessor = new ExampleMatcherAccessor(matcher);
		AtomicInteger relationshipPatternCount = new AtomicInteger();

		Predicate predicate = new Predicate(nodeDescription);
		for (GraphPropertyDescription graphProperty : graphProperties) {
			PropertyPath propertyPath = PropertyPath.from(graphProperty.getFieldName(), nodeDescription.getTypeInformation());
			// create condition for every defined property
			PropertyPathWrapper propertyPathWrapper = new PropertyPathWrapper(relationshipPatternCount.incrementAndGet(), mappingContext.getPersistentPropertyPath(propertyPath), true);
			addConditionAndParameters(mappingContext, nodeDescription, beanWrapper, mode, matcherAccessor, predicate, graphProperty, propertyPathWrapper);
		}

		processRelationships(mappingContext, example, nodeDescription, beanWrapper, mode, relationshipPatternCount, null, predicate);

		return predicate;
	}

	private static <S> void processRelationships(Neo4jMappingContext mappingContext, Example<S> example, NodeDescription<?> currentNodeDescription,
												 DirectFieldAccessFallbackBeanWrapper beanWrapper, ExampleMatcher.MatchMode mode, AtomicInteger relationshipPatternCount,
												 @Nullable PropertyPath propertyPath, Predicate predicate) {

		for (RelationshipDescription relationship : currentNodeDescription.getRelationships()) {
			String relationshipFieldName = relationship.getFieldName();
			Object relationshipObject = beanWrapper.getPropertyValue(relationshipFieldName);

			if (relationshipObject == null) {
				continue;
			}

			// Right now we are only accepting the first element of a collection as a filter entry.
			// Maybe combining multiple entities with AND might make sense.
			if (relationshipObject instanceof Collection collection) {
				int collectionSize = collection.size();
				if (collectionSize > 1) {
					throw new IllegalArgumentException("Cannot have more than one related node per collection.");
				}
				if (collectionSize == 0) {
					continue;
				}
				relationshipObject = collection.iterator().next();

			}
			NodeDescription<?> relatedNodeDescription = mappingContext.getNodeDescription(relationshipObject.getClass());

			// if we come from the root object, the path is probably _null_,
			// and it needs to get initialized with the property name of the relationship
			PropertyPath nestedPropertyPath = propertyPath == null
					? PropertyPath.from(relationshipFieldName, currentNodeDescription.getUnderlyingClass())
					: propertyPath.nested(relationshipFieldName);

			PropertyPathWrapper nestedPropertyPathWrapper = new PropertyPathWrapper(relationshipPatternCount.incrementAndGet(), mappingContext.getPersistentPropertyPath(nestedPropertyPath), false);
			predicate.addRelationship(nestedPropertyPathWrapper);

			for (GraphPropertyDescription graphProperty : relatedNodeDescription.getGraphProperties()) {
				addConditionAndParameters(mappingContext, (Neo4jPersistentEntity<?>) relatedNodeDescription, new DirectFieldAccessFallbackBeanWrapper(relationshipObject), mode,
						new ExampleMatcherAccessor(example.getMatcher()), predicate,
						graphProperty, nestedPropertyPathWrapper);
			}

			processRelationships(mappingContext, example, relatedNodeDescription, new DirectFieldAccessFallbackBeanWrapper(relationshipObject), mode, relationshipPatternCount,
					nestedPropertyPath, predicate);

		}
	}

	private static void addConditionAndParameters(Neo4jMappingContext mappingContext, Neo4jPersistentEntity<?> nodeDescription, DirectFieldAccessFallbackBeanWrapper beanWrapper,
												  ExampleMatcher.MatchMode mode, ExampleMatcherAccessor matcherAccessor, Predicate predicate, GraphPropertyDescription graphProperty,
												  PropertyPathWrapper wrapper) {

		String currentPath = graphProperty.getFieldName();
		if (matcherAccessor.isIgnoredPath(currentPath)) {
			return;
		}

		boolean internalId = graphProperty.isIdProperty() && nodeDescription.isUsingInternalIds();
		String propertyName = graphProperty.getPropertyName();

		ExampleMatcher.PropertyValueTransformer transformer = matcherAccessor
				.getValueTransformerForPath(currentPath);
		Optional<Object> optionalValue = transformer
				.apply(Optional.ofNullable(beanWrapper.getPropertyValue(currentPath)));

		if (optionalValue.isEmpty()) {
			if (!internalId && matcherAccessor.getNullHandler().equals(ExampleMatcher.NullHandler.INCLUDE)) {
				predicate.add(mode, property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription), propertyName).isNull());
			}
			return;
		}

		Neo4jConversionService conversionService = mappingContext.getConversionService();
		boolean isRootNode = predicate.neo4jPersistentEntity.equals(nodeDescription);

		if (graphProperty.isIdProperty() && nodeDescription.isUsingInternalIds()) {
			if (isRootNode) {
				predicate.add(mode,
						predicate.neo4jPersistentEntity.getIdExpression().isEqualTo(literalOf(optionalValue.get())));
			} else {
				predicate.add(mode,
						nodeDescription.getIdExpression().isEqualTo(literalOf(optionalValue.get())));
			}
		} else {
			Expression property =  !isRootNode ? property(wrapper.getNodeName(), propertyName) : property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription), propertyName);
			Expression parameter = parameter(wrapper.getNodeName() + propertyName);
			Condition condition = property.isEqualTo(parameter);

			if (String.class.equals(graphProperty.getActualType())) {

				if (matcherAccessor.isIgnoreCaseForPath(currentPath)) {
					property = Functions.toLower(property);
					parameter = Functions.toLower(parameter);
				}

				condition = switch (matcherAccessor.getStringMatcherForPath(currentPath)) {
					case DEFAULT, EXACT ->
							// This needs to be recreated as both property and parameter might have changed above
							property.isEqualTo(parameter);
					case CONTAINING -> property.contains(parameter);
					case STARTING -> property.startsWith(parameter);
					case ENDING -> property.endsWith(parameter);
					case REGEX -> property.matches(parameter);
				};
			}
			predicate.add(mode, condition);
			predicate.parameters.put(wrapper.getNodeName() + propertyName, optionalValue.map(
					v -> {
						Neo4jPersistentProperty neo4jPersistentProperty = (Neo4jPersistentProperty) graphProperty;
						return conversionService.writeValue(v, neo4jPersistentProperty.getTypeInformation(),
										neo4jPersistentProperty.getOptionalConverter());
					})
					.get());
		}
	}

	private final Neo4jPersistentEntity neo4jPersistentEntity;

	private Condition condition = Conditions.noCondition();

	private final Map<String, Object> parameters = new HashMap<>();

	private final Set<PropertyPathWrapper> relationshipFields = new HashSet<>();

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

	private void addRelationship(PropertyPathWrapper propertyPathWrapper) {
		this.relationshipFields.add(propertyPathWrapper);
	}

	public NodeDescription<?> getNeo4jPersistentEntity() {
		return neo4jPersistentEntity;
	}

	public Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	public Set<PropertyPathWrapper> getPropertyPathWrappers() {
		return relationshipFields;
	}
}
