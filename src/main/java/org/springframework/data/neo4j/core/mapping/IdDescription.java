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
package org.springframework.data.neo4j.core.mapping;

import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
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
	private @Nullable final Class<? extends IdGenerator<?>> idGeneratorClass;

	/**
	 * A reference to an ID generator.
	 */
	private @Nullable final String idGeneratorRef;

	/**
	 * The property that stores the id if applicable.
	 */
	private @Nullable final String graphPropertyName;

	private final Lazy<Expression> idExpression;

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
		    @Nullable Class<? extends IdGenerator<?>> idGeneratorClass,
			@Nullable String idGeneratorRef, String graphPropertyName) {

		Assert.notNull(graphPropertyName, "Graph property name is required");
		try {
			Assert.hasText(idGeneratorRef, "Reference to an ID generator has precedence");

			return new IdDescription(symbolicName, null, idGeneratorRef, graphPropertyName, false);
		} catch (IllegalArgumentException e) {
			Assert.notNull(idGeneratorClass, "Class of id generator is required");
			Assert.isTrue(idGeneratorClass != GeneratedValue.InternalIdGenerator.class,
					"Cannot use InternalIdGenerator for externally generated ids");

			return new IdDescription(symbolicName, idGeneratorClass, null, graphPropertyName, false);
		}
	}

	private IdDescription(SymbolicName symbolicName, @Nullable Class<? extends IdGenerator<?>> idGeneratorClass,
		    @Nullable String idGeneratorRef, @Nullable String graphPropertyName, boolean isDeprecated) {

		this.idGeneratorClass = idGeneratorClass;
		this.idGeneratorRef = idGeneratorRef != null && idGeneratorRef.isEmpty() ? null : idGeneratorRef;
		this.graphPropertyName = graphPropertyName;

		this.idExpression = Lazy.of(() -> {
			final Node rootNode = Cypher.anyNode(symbolicName);
			if (this.isInternallyGeneratedId()) {
				return isDeprecated ? Functions.id(rootNode) : Functions.elementId(rootNode);
			} else {
				return this.getOptionalGraphPropertyName()
						.map(propertyName -> Cypher.property(symbolicName, propertyName)).get();
			}
		});
	}

	public Expression asIdExpression() {
		return this.idExpression.get();
	}

	public Optional<Class<? extends IdGenerator<?>>> getIdGeneratorClass() {
		return Optional.ofNullable(idGeneratorClass);
	}

	public Optional<String> getIdGeneratorRef() {
		return Optional.ofNullable(idGeneratorRef);
	}

	/**
	 * @return True, if the ID is assigned to the entity before the entity hits the database, either manually or through a
	 *         generator.
	 */
	public boolean isAssignedId() {
		return this.idGeneratorClass == null && this.idGeneratorRef == null;
	}

	/**
	 * @return True, if the database generated the ID.
	 */
	public boolean isInternallyGeneratedId() {
		return this.idGeneratorClass == GeneratedValue.InternalIdGenerator.class;
	}

	/**
	 * @return True, if the ID is externally generated.
	 */
	public boolean isExternallyGeneratedId() {
		return (this.idGeneratorClass != null && this.idGeneratorClass != GeneratedValue.InternalIdGenerator.class)
				|| this.idGeneratorRef != null;
	}

	/**
	 * An ID description has only a corresponding graph property name when it's bas on an external assigment. An internal
	 * id has no corresponding graph property and therefore this method will return an empty {@link Optional} in such
	 * cases.
	 *
	 * @return The name of an optional graph property.
	 */
	public Optional<String> getOptionalGraphPropertyName() {
		return Optional.ofNullable(graphPropertyName);
	}
}
