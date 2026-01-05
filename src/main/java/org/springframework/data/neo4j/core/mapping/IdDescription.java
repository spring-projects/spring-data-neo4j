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
package org.springframework.data.neo4j.core.mapping;

import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.SymbolicName;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.util.Lazy;
import org.springframework.util.Assert;

/**
 * Description how to generate Ids for entities.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class IdDescription {

	/**
	 * The class representing a generator for new ids or null for assigned ids.
	 */
	@Nullable
	private final Class<? extends IdGenerator<?>> idGeneratorClass;

	/**
	 * A reference to an ID generator.
	 */
	@Nullable
	private final String idGeneratorRef;

	/**
	 * The property that stores the id if applicable.
	 */
	@Nullable
	private final String graphPropertyName;

	private final boolean isDeprecated;

	private final Lazy<Expression> idExpression;

	@SuppressWarnings("deprecation")
	private IdDescription(SymbolicName symbolicName, @Nullable Class<? extends IdGenerator<?>> idGeneratorClass,
			@Nullable String idGeneratorRef, @Nullable String graphPropertyName, boolean isDeprecated) {

		this.idGeneratorClass = idGeneratorClass;
		this.idGeneratorRef = (idGeneratorRef != null && idGeneratorRef.isEmpty()) ? null : idGeneratorRef;
		this.graphPropertyName = graphPropertyName;
		this.isDeprecated = isDeprecated;

		this.idExpression = Lazy.of(() -> {
			final Node rootNode = Cypher.anyNode(symbolicName);
			if (this.isInternallyGeneratedId()) {
				return isDeprecated ? rootNode.internalId() : rootNode.elementId();
			}
			else {
				return this.getOptionalGraphPropertyName()
					.map(propertyName -> Cypher.property(symbolicName, propertyName))
					.get();
			}
		});
	}

	public static IdDescription forAssignedIds(SymbolicName symbolicName, String graphPropertyName) {

		Assert.notNull(graphPropertyName, "Graph property name is required");
		return new IdDescription(symbolicName, null, null, graphPropertyName, false);
	}

	public static IdDescription forInternallyGeneratedIds(SymbolicName symbolicName) {
		return forInternallyGeneratedIds(symbolicName, false);
	}

	public static IdDescription forInternallyGeneratedIds(SymbolicName symbolicName, boolean isDeprecated) {
		return new IdDescription(symbolicName, GeneratedValue.InternalIdGenerator.class, null, null, isDeprecated);
	}

	public static IdDescription forExternallyGeneratedIds(SymbolicName symbolicName,
			Class<? extends IdGenerator<?>> idGeneratorClass, String idGeneratorRef, String graphPropertyName) {

		Assert.notNull(graphPropertyName, "Graph property name is required");
		try {
			Assert.hasText(idGeneratorRef, "Reference to an ID generator has precedence");

			return new IdDescription(symbolicName, null, idGeneratorRef, graphPropertyName, false);
		}
		catch (IllegalArgumentException ex) {
			Assert.notNull(idGeneratorClass, "Class of id generator is required");
			Assert.isTrue(idGeneratorClass != GeneratedValue.InternalIdGenerator.class,
					"Cannot use InternalIdGenerator for externally generated ids");

			return new IdDescription(symbolicName, idGeneratorClass, null, graphPropertyName, false);
		}
	}

	public Expression asIdExpression() {
		return this.idExpression.get();
	}

	/**
	 * Creates the right identifier expression for this node entity. Note: This enforces a
	 * recalculation of the name on invoke.
	 * @param nodeName use this name as the symbolic name of the node in the query
	 * @return an expression that represents the right identifier type
	 */
	@SuppressWarnings("deprecation")
	public Expression asIdExpression(String nodeName) {
		final Node rootNode = Cypher.anyNode(nodeName);
		if (this.isInternallyGeneratedId()) {
			return this.isDeprecated ? rootNode.internalId() : rootNode.elementId();
		}
		else {
			return this.getOptionalGraphPropertyName()
				.map(propertyName -> Cypher.property(nodeName, propertyName))
				.orElseThrow();
		}
	}

	public Optional<Class<? extends IdGenerator<?>>> getIdGeneratorClass() {
		return Optional.ofNullable(this.idGeneratorClass);
	}

	public Optional<String> getIdGeneratorRef() {
		return Optional.ofNullable(this.idGeneratorRef);
	}

	/**
	 * Flag, if this is an assigned id.
	 * @return true, if the ID is assigned to the entity before the entity hits the
	 * database, either manually or through a generator.
	 */
	public boolean isAssignedId() {
		return this.idGeneratorClass == null && this.idGeneratorRef == null;
	}

	/**
	 * Flag, if this is a database generated id.
	 * @return true, if the database generated the ID.
	 */
	public boolean isInternallyGeneratedId() {
		return this.idGeneratorClass == GeneratedValue.InternalIdGenerator.class;
	}

	/**
	 * Flag, if this is an externally generated id.
	 * @return true, if the ID is externally generated
	 */
	public boolean isExternallyGeneratedId() {
		return (this.idGeneratorClass != null && this.idGeneratorClass != GeneratedValue.InternalIdGenerator.class)
				|| this.idGeneratorRef != null;
	}

	/**
	 * An ID description has only a corresponding graph property name when it's bas on an
	 * external assigment. An internal id has no corresponding graph property and
	 * therefore this method will return an empty {@link Optional} in such cases.
	 * @return the name of an optional graph property
	 */
	public Optional<String> getOptionalGraphPropertyName() {
		return Optional.ofNullable(this.graphPropertyName);
	}

}
