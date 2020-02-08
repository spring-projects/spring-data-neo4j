/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.mapping;

import java.lang.reflect.Field;
import java.util.Optional;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Version;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * This class implements Spring Data's PersistentProperty interface, scavenging the required data from the OGM's mapping
 * classes in order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is
 * effectively a shim for FieldInfo. We don't reload all the mapping information again. We do not yet support
 * getter/setter access to entity properties.
 * <p>
 * These attributes do not appear to be used/needed for SDN 4 to inter-operate correctly with SD-REST:
 * </p>
 * <ul>
 * <li>mapValueType</li>
 * <li>typeInformation</li>
 * <li>isVersionProperty (there is no SDN versioning at this stage)</li>
 * <li>isTransient (we never supply transient classes to the Spring mapping context)</li>
 * <li>isWritable (we don't currently support read-only fields)</li>
 * </ul>
 * Consequently their associated getter methods always return default values of null or [true|false] However, because
 * these method calls are not expected, we also log a warning message if they get invoked
 *
 * @author Vince Bickers
 * @author Adam George
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Michael J. Simons
 * @since 4.0.0
 */
public class Neo4jPersistentProperty extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jPersistentProperty.class);

	enum PropertyType {
		REGULAR_PROPERTY(false), INTERNAL_ID_PROPERTY(true), ID_PROPERTY(true);

		private final boolean idProperty;

		PropertyType(boolean idProperty) {
			this.idProperty = idProperty;
		}
	}

	private final PropertyType propertyType;

	/**
	 * Constructs a new {@link Neo4jPersistentProperty} based on the given arguments.
	 *
	 * @param owningClassInfo The {@link ClassInfo} of the object of which the property field is a member
	 * @param property The property
	 * @param owner The owning {@link PersistentEntity} that corresponds to the given {@code ClassInfo}
	 * @param simpleTypeHolder The {@link SimpleTypeHolder} that dictates whether the type of this property is considered
	 *          simple or not
	 */
	public Neo4jPersistentProperty(ClassInfo owningClassInfo, Property property,
			PersistentEntity<?, Neo4jPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);

		if (owningClassInfo == null) {
			logger.warn("Owning ClassInfo is null for property: {}", property);
		}

		if (owningClassInfo == null || owningClassIsSimple(owningClassInfo, simpleTypeHolder)
				|| owningClassDoesNotSupportIdProperties(owningClassInfo) || owningPropertyIsEnum(owner)) {
			this.propertyType = PropertyType.REGULAR_PROPERTY;
		} else if (isInternalIdentityField(owningClassInfo, property)) {
			this.propertyType = PropertyType.INTERNAL_ID_PROPERTY;
		} else if (isExplicitIdentityField(owningClassInfo, property)) {
			this.propertyType = PropertyType.ID_PROPERTY;
		} else {
			this.propertyType = PropertyType.REGULAR_PROPERTY;
		}
	}

	private static boolean owningPropertyIsEnum(PersistentEntity<?, Neo4jPersistentProperty> owner) {
		return owner.getType().isEnum();
	}

	private static boolean owningClassIsSimple(ClassInfo owningClassInfo, SimpleTypeHolder simpleTypeHolder) {

		return owningClassInfo.getUnderlyingClass() != null
				&& simpleTypeHolder.isSimpleType(owningClassInfo.getUnderlyingClass());
	}

	private static boolean owningClassDoesNotSupportIdProperties(ClassInfo owningClassInfo) {

		return owningClassInfo.isInterface() || owningClassInfo.annotationsInfo().get(QueryResult.class.getName()) != null
				|| owningClassInfo.isEnum();
	}

	private static boolean isInternalIdentityField(ClassInfo owningClassInfo, Property property) {

		Optional<Field> optionalInternalIdentityField = Optional.ofNullable(owningClassInfo.identityFieldOrNull())
				.map(FieldInfo::getField);
		return property.getField().equals(optionalInternalIdentityField);
	}

	private static boolean isExplicitIdentityField(ClassInfo owningClassInfo, Property property) {

		// Cannot use owningClassInfo.propertyField() as those are not initialized yet. They will
		// be initialized on the call, but that would change behaviour: SDN fails late when there
		// are invalid properties atm. Even if'ts better to fail early, it means at least changing
		// a dozen tests in SDN itself.
		return property.getField().map(field -> AnnotatedElementUtils.findMergedAnnotation(field, Id.class)).isPresent();
	}

	@Override
	public boolean isIdProperty() {
		return propertyType.idProperty;
	}

	/**
	 * @return True if this property describes the internal ID property.
	 */
	public boolean isInternalIdProperty() {
		return propertyType == PropertyType.INTERNAL_ID_PROPERTY;
	}

	PropertyType getPropertyType() {
		return propertyType;
	}

	@Override
	public boolean isVersionProperty() {
		return isAnnotationPresent(Version.class);
	}

	/**
	 * Overridden to force field access as opposed to getter method access for simplicity.
	 *
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#usePropertyAccess()
	 */
	@Override
	public boolean usePropertyAccess() {
		logger.debug("[property].usePropertyAccess() returns false");
		return false;
	}

	/**
	 * Determines whether or not this property should be considered an association to another entity or whether it's just
	 * a simple property that should be shown as a value.
	 * <p>
	 * This implementation works by looking for non-transient members annotated with <code>@Relationship</code>.
	 * </p>
	 *
	 * @return <code>true</code> if this property is an association to another entity, <code>false</code> if not
	 */
	@Override
	public boolean isAssociation() {
		return !isTransient() && (isAnnotationPresent(Relationship.class) || isAnnotationPresent(StartNode.class)
				|| isAnnotationPresent(EndNode.class));
	}

	@Override
	protected Association<Neo4jPersistentProperty> createAssociation() {
		return new Association<Neo4jPersistentProperty>(this, null);
	}
}
