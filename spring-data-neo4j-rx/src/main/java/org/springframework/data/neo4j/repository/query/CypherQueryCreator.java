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
package org.springframework.data.neo4j.repository.query;

import static lombok.AccessLevel.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;

import lombok.RequiredArgsConstructor;

import java.util.Iterator;
import java.util.Optional;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.cypher.Property;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameter;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameters;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * A Cypher-DSL based implementation of the {@link AbstractQueryCreator} that eventually creates Cypher queries as strings
 * to be used by a Neo4j client or driver as statement template.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
final class CypherQueryCreator extends AbstractQueryCreator<String, Condition> {

	private final Neo4jMappingContext mappingContext;
	private final Class<?> domainType;
	private final NodeDescription<?> nodeDescription;

	private Iterator<Neo4jParameter> formalParameters;

	CypherQueryCreator(Neo4jMappingContext mappingContext, Class<?> domainType, PartTree tree,
		Parameters<Neo4jParameters, Neo4jParameter> formalParameters, ParameterAccessor actualParameters
	) {
		super(tree, actualParameters);

		this.mappingContext = mappingContext;
		this.domainType = domainType;
		this.nodeDescription = this.mappingContext.getRequiredNodeDescription(this.domainType);
		this.formalParameters = formalParameters.iterator();
	}

	@Override
	protected Condition create(Part part, Iterator<Object> actualParameters) {
		return createImpl(part, actualParameters);
	}

	@Override
	protected Condition and(Part part, Condition base, Iterator<Object> actualParameters) {

		if (base == null) {
			return create(part, actualParameters);
		}

		return base.and(createImpl(part, actualParameters));
	}

	@Override
	protected Condition or(Condition base, Condition condition) {
		return base.or(condition);
	}

	@Override
	protected String complete(Condition condition, Sort sort) {

		Statement statement = mappingContext
			.prepareMatchOf(nodeDescription, Optional.of(condition))
			.returning(Cypher.symbolicName("n"))
			.build();

		return CypherRenderer.create().render(statement);
	}

	private Condition createImpl(Part part, Iterator<Object> actualParameters) {

		PersistentPropertyPath<Neo4jPersistentProperty> path = mappingContext
			.getPersistentPropertyPath(part.getProperty());
		Neo4jPersistentProperty persistentProperty = path.getLeafProperty();

		// TODO case insensitive (like, notlike, simpleProperty, negatedSimpleProperty)

		switch (part.getType()) {
			case BETWEEN:
				return betweenCondition(persistentProperty, actualParameters);
			case CONTAINING:
				return property(persistentProperty)
					.contains(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case ENDING_WITH:
				return property(persistentProperty)
					.endsWith(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case FALSE:
				return property(persistentProperty).isFalse();
			case GREATER_THAN:
				return property(persistentProperty).gt(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case GREATER_THAN_EQUAL:
				return property(persistentProperty).gte(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case LESS_THAN:
				return property(persistentProperty).lt(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case LESS_THAN_EQUAL:
				return property(persistentProperty).lte(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case LIKE:
				return likeCondition(persistentProperty, nextRequiredParameter(actualParameters).nameOrIndex);
			case SIMPLE_PROPERTY:
				return property(persistentProperty)
					.isEqualTo(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case STARTING_WITH:
				return property(persistentProperty)
					.startsWith(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case REGEX:
				return property(persistentProperty)
					.matches(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case NEGATING_SIMPLE_PROPERTY:
				return property(persistentProperty)
					.isNotEqualTo(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case NOT_CONTAINING:
				return property(persistentProperty)
					.contains(parameter(nextRequiredParameter(actualParameters).nameOrIndex)).not();
			case NOT_LIKE:
				return likeCondition(persistentProperty, nextRequiredParameter(actualParameters).nameOrIndex).not();
			case TRUE:
				return property(persistentProperty).isTrue();
			default:
				throw new IllegalArgumentException("Unsupported part type: " + part.getType());
		}
	}

	private Condition likeCondition(Neo4jPersistentProperty persistentProperty, String parameterName) {
		return property(persistentProperty)
			.matches(literalOf(".*").plus(parameter(parameterName)).plus(literalOf(".*")));
	}

	private Condition betweenCondition(Neo4jPersistentProperty persistentProperty, Iterator<Object> actualParameters) {

		Parameter lowerBoundOrRange = nextRequiredParameter(actualParameters);

		Property property = property(persistentProperty);
		Condition betweenCondition = Conditions.noCondition();
		if (lowerBoundOrRange.value instanceof Range) {
			Range range = (Range) lowerBoundOrRange.value;

			if (range.getLowerBound().isBounded()) {
				Expression parameterPlaceholder = parameter(lowerBoundOrRange.nameOrIndex + ".lb");
				betweenCondition = betweenCondition.and(range.getLowerBound().isInclusive() ?
					property.gte(parameterPlaceholder) :
					property.gt(parameterPlaceholder));
			}

			if (range.getUpperBound().isBounded()) {
				Expression parameterPlaceholder = parameter(lowerBoundOrRange.nameOrIndex + ".ub");
				betweenCondition = betweenCondition.and(range.getUpperBound().isInclusive() ?
					property.lte(parameterPlaceholder) :
					property.lt(parameterPlaceholder));
			}
		} else {
			Parameter upperBound = nextRequiredParameter(actualParameters);
			betweenCondition = property.gte(parameter(lowerBoundOrRange.nameOrIndex))
				.and(property.lte(parameter(upperBound.nameOrIndex)));
		}
		return betweenCondition;
	}

	private Property property(Neo4jPersistentProperty persistentProperty) {
		return Cypher.property("n", persistentProperty.getPropertyName());
	}

	private Parameter nextRequiredParameter(Iterator<Object> actualParameters) {
		if (!formalParameters.hasNext()) {
			throw new IllegalStateException("Not enough formal, bindable parameters for parts");
		}

		//System.out.println(actualParameters());
		return new Parameter(formalParameters.next().getNameOrIndex(), actualParameters.next());
	}

	@RequiredArgsConstructor(access = PACKAGE)
	static class Parameter {
		final String nameOrIndex;

		final Object value;
	}
}
