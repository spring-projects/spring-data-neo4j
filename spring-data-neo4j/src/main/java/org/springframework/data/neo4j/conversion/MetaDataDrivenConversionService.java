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
package org.springframework.data.neo4j.conversion;

import java.util.Optional;

import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.support.ClassUtils;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.neo4j.ogm.typeconversion.EnumStringConverter;
import org.neo4j.ogm.typeconversion.ProxyAttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * Specialisation of {@link GenericConversionService} that creates Spring-compatible converters from those known by the
 * mapping {@link MetaData}, allowing the OGM type converters to be reused throughout a Spring application.
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Michael J. Simons
 */
public class MetaDataDrivenConversionService extends GenericConversionService {

	private static final Logger logger = LoggerFactory.getLogger(MetaDataDrivenConversionService.class);

	/**
	 * Constructs a new {@link MetaDataDrivenConversionService} based on the given {@link MetaData}.
	 *
	 * @param metaData The OGM {@link MetaData} from which to elicit type converters configured in the underlying
	 *          object-graph mapping layer
	 */
	public MetaDataDrivenConversionService(MetaData metaData) {

		for (ClassInfo classInfo : metaData.persistentEntities()) {
			for (FieldInfo fieldInfo : classInfo.propertyFields()) {
				if (fieldInfo.hasPropertyConverter()) {
					addWrappedConverter(fieldInfo.getField().getType(), fieldInfo.getPropertyConverter());
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addWrappedConverter(Class<?> type, final AttributeConverter attributeConverter) {

		if (attributeConverter instanceof ProxyAttributeConverter) {
			return;
		}

		EntityToGraphTypeMapping entityToGraphTypeMapping = getEntityToGraphTypeMapping(type, attributeConverter);

		if (canConvert(entityToGraphTypeMapping.entityType, entityToGraphTypeMapping.graphType)
				&& canConvert(entityToGraphTypeMapping.graphType, entityToGraphTypeMapping.entityType)) {
			logger.debug("Not adding Spring-compatible converter for " + attributeConverter.getClass()
					+ " because one that does the same job has already been registered with the ConversionService.");
		} else {
			Converter toGraphConverter = attributeConverter::toGraphProperty;
			Converter toEntityConverter = attributeConverter::toEntityAttribute;

			// It could be argued that this is wrong as it potentially overrides a registered converted that doesn't handle
			// both directions, but I've decided that it's better to ensure the same converter is used for load and save.
			addConverter(entityToGraphTypeMapping.entityType, entityToGraphTypeMapping.graphType, toGraphConverter);
			addConverter(entityToGraphTypeMapping.graphType, entityToGraphTypeMapping.entityType, toEntityConverter);
		}
	}

	static class EntityToGraphTypeMapping {
		Class<?> entityType;
		Class<?> graphType;

		private EntityToGraphTypeMapping(Class<?> entityType, Class<?> target) {
			this.entityType = entityType;
			this.graphType = target;
		}
	}

	static EntityToGraphTypeMapping getEntityToGraphTypeMapping(Class<?> type, AttributeConverter attributeConverter) {

		ResolvableType resolvableType = ResolvableType.forClass(AttributeConverter.class,
				attributeConverter.getClass());

		if (!resolvableType.hasGenerics()) {
			throw new IllegalStateException(
					"Cannot resolve source and target types for the given attribute converter of class "
							+ attributeConverter.getClass());
		}

		Class<?> sourceType = ClassUtils.isEnum(type) ? type : nestedTypeOrType(resolvableType.getGeneric(0));
		Class<?> targetType = nestedTypeOrType(resolvableType.getGeneric(1));

		return new EntityToGraphTypeMapping(sourceType, targetType);
	}

	/**
	 * If the type can be resolved to a collection that has generics, we extract the collection type, otherwise we return
	 * the resolved type.
	 *
	 * @param type Type to resolve
	 * @return The types resolved class or in case of a generic collections, the collections elements class.
	 */
	private static Class<?> nestedTypeOrType(ResolvableType type) {
		return Optional.ofNullable(type.asCollection()).filter(ResolvableType::hasGenerics).map(r -> r.getGeneric(0))
				.orElse(type).resolve(Object.class);
	}
}
