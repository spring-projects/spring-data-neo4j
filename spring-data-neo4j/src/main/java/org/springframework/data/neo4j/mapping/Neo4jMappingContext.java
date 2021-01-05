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
package org.springframework.data.neo4j.mapping;

import static java.util.Collections.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.metadata.AnnotationsInfo;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.neo4j.ogm.typeconversion.ConverterBasedCollectionConverter;
import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ReflectionUtils;

/**
 * This class implements Spring Data's MappingContext interface, scavenging the required data from the OGM's metadata in
 * order to for SDN to play nicely with Spring Data REST. The main thing to note is that this class is effectively a
 * container shim for {@code ClassInfo} objects. We don't reload all the mapping information again.
 *
 * @author Vince Bickers
 * @author Adam George
 * @author Mark Paluch
 * @author Michael J. Simons
 * @since 4.0.0
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jMappingContext.class);

	// Instantiation of the EntityInstantiators must happen only once, otherwise
	// the org.springframework.data.convert.ClassGeneratingEntityInstantiator will
	// be created every time an instantiator is requested and thus the whole thing
	// will dynamically create classes like there is no tomorrow.
	private final EntityInstantiators instantiators = new EntityInstantiators();
	private final MetaData metaData;

	/**
	 * Constructs a new {@link Neo4jMappingContext} based on the persistent entities in the given {@link MetaData}.
	 *
	 * @param metaData The OGM {@link MetaData} from which to extract the persistent entities
	 */
	public Neo4jMappingContext(MetaData metaData) {
		this.metaData = metaData;

		Predicate<ClassInfo> underlyingClassIsPresent = classInfo -> classInfo.getUnderlyingClass() != null;
		Predicate<ClassInfo> isAnnotatedEntity = classInfo -> {
			final AnnotationsInfo annotationsInfo = classInfo.annotationsInfo();
			return annotationsInfo.get(NodeEntity.class) != null || annotationsInfo.get(RelationshipEntity.class) != null;
		};

		metaData.persistentEntities().stream()
				.filter(underlyingClassIsPresent.and(isAnnotatedEntity))
				.forEach(k -> addPersistentEntity(k.getUnderlyingClass()));

		installDefaultConverter();

		logger.info("Neo4jMappingContext initialisation completed");
	}

	private void installDefaultConverter() {

		Method setPropertyConverter = ReflectionUtils.findMethod(FieldInfo.class, "setPropertyConverter",
				AttributeConverter.class);

		if (setPropertyConverter == null) {
			return;
		}

		setPropertyConverter.setAccessible(true);

		NativePointConverter nativePointConverter = new NativePointConverter();
		NativePointArrayConverter nativePointArrayConverter = new NativePointArrayConverter();

		metaData.persistentEntities().stream().flatMap(classInfo -> Stream
				.concat(classInfo.findFields(Point.class).stream(), classInfo.findIterableFields(Point.class).stream()))
				.distinct().forEach(fieldInfo -> {

					AttributeConverter<?, ?> propertyConverter;
					if (fieldInfo.isArray()) {
						propertyConverter = nativePointArrayConverter;
					} else if (fieldInfo.isIterable()) {
						propertyConverter = new ConverterBasedCollectionConverter(fieldInfo.getField().getType(),
								nativePointConverter);
					} else {
						propertyConverter = nativePointConverter;
					}

					ReflectionUtils.invokeMethod(setPropertyConverter, fieldInfo, propertyConverter);
				});
	}

	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		logger.debug("Creating Neo4jPersistentEntity from type information: {}", typeInformation);
		return new Neo4jPersistentEntity<>(typeInformation, this.metaData);
	}

	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property, Neo4jPersistentEntity<?> owner,
			SimpleTypeHolder simpleTypeHolder) {

		ClassInfo owningClassInfo = this.metaData.classInfo(owner.getType().getName());

		Field propertyField = property.getField().orElse(null);

		if (!property.isFieldBacked() && owningClassInfo != null) {
			FieldInfo fieldInfo = owningClassInfo.propertyFieldByName(property.getName());
			if (fieldInfo == null) {
				fieldInfo = owningClassInfo.relationshipFieldByName(property.getName());
			}
			if (fieldInfo != null) {
				propertyField = owningClassInfo.getField(fieldInfo);
			} else {
				// there is no field, probably because descriptor gave us a field name derived from a getter
				logger.debug("Couldn't resolve a concrete field corresponding to property {} on {} ", property.getName(),
						owningClassInfo.name());
			}
		}

		return new Neo4jPersistentProperty(owningClassInfo, property, owner,
				updateSimpleTypeHolder(simpleTypeHolder, propertyField));
	}

	/**
	 * This is not public API and should not be overwritten.
	 * @param entity The persistent entity for which to retrieve an {@link EntityInstantiator}.
	 * @return An instantiator for a given entity.
	 */
	public final EntityInstantiator getInstantiatorFor(PersistentEntity<?, ?> entity) {
		return instantiators.getInstantiatorFor(entity);
	}

	private SimpleTypeHolder updateSimpleTypeHolder(SimpleTypeHolder currentSimpleTypeHolder, Field field) {
		if (field == null) {
			return currentSimpleTypeHolder;
		}

		final Class<?> fieldType = field.getType().isArray() ? field.getType().getComponentType() : field.getType();

		if (shouldUpdateSimpleTypes(currentSimpleTypeHolder, field, fieldType)) {
			SimpleTypeHolder updatedSimpleTypeHolder = new SimpleTypeHolder(singleton(fieldType), currentSimpleTypeHolder);
			setSimpleTypeHolder(updatedSimpleTypeHolder);
			return updatedSimpleTypeHolder;
		}
		return currentSimpleTypeHolder;
	}

	private boolean shouldUpdateSimpleTypes(SimpleTypeHolder currentSimpleTypeHolder, Field field,
			Class<?> rawFieldType) {
		if (field.isAnnotationPresent(Convert.class)) {
			return true;
		}

		if (currentSimpleTypeHolder.isSimpleType(rawFieldType) || rawFieldType.isInterface()) {
			return false;
		}
		if (this.metaData.classInfo(rawFieldType.getName()) == null) {
			logger.info("No class information found in OGM meta-data for {} so treating as simple type for SD Commons",
					rawFieldType);
			return true;
		}
		return false;
	}

	/**
	 * This is used internally for automatic conversion of Spring Data Point to a fitting Neo4j point. While OGM native
	 * types treat longitude as x and latitude as y, we apply the same behaviour as SDN: x/y -> lat/long. We need however
	 * a geographic type, as all calculations expected from Spring Data are usually based on WGS-84 If you want to be
	 * explicit which coordinate system you use, please use {@link org.neo4j.ogm.types.spatial.GeographicPoint2d} or
	 * {@link CartesianPoint2d} in your domain model.
	 */
	private static class NativePointConverter implements AttributeConverter<Point, GeographicPoint2d> {

		@Override
		public GeographicPoint2d toGraphProperty(Point value) {

			return Optional.ofNullable(value).map(p -> new GeographicPoint2d(value.getX(), value.getY())).orElse(null);
		}

		@Override
		public Point toEntityAttribute(GeographicPoint2d value) {

			return Optional.ofNullable(value).map(p -> new Point(value.getLatitude(), value.getLongitude())).orElse(null);
		}
	}

	/**
	 * Wrapper delegating to {@link NativePointConverter} for arrays of points.
	 */
	private static class NativePointArrayConverter implements AttributeConverter<Point[], GeographicPoint2d[]> {

		private final NativePointConverter delegate = new NativePointConverter();

		@Override
		public GeographicPoint2d[] toGraphProperty(Point[] value) {

			if (value == null) {
				return null;
			}

			return Arrays.stream(value).map(delegate::toGraphProperty).toArray(GeographicPoint2d[]::new);
		}

		@Override
		public Point[] toEntityAttribute(GeographicPoint2d[] value) {

			if (value == null) {
				return null;
			}

			return Arrays.stream(value).map(delegate::toEntityAttribute).toArray(Point[]::new);
		}
	}

}
