/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.conversion;

import java.lang.reflect.ParameterizedType;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MethodInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.neo4j.ogm.typeconversion.ConversionCallback;
import org.neo4j.ogm.typeconversion.ProxyAttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * Specialisation of {@link GenericConversionService} that creates Spring-compatible converters from those known by the
 * mapping {@link MetaData}, allowing the OGM type converters to be reused throughout a Spring application.
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
public class MetaDataDrivenConversionService extends GenericConversionService implements ConversionCallback {

	private static final Logger logger = LoggerFactory.getLogger(MetaDataDrivenConversionService.class);

	/**
	 * Constructs a new {@link MetaDataDrivenConversionService} based on the given {@link MetaData}.
	 *
	 * @param metaData The OGM {@link MetaData} from which to elicit type converters configured in the underlying
	 *          object-graph mapping layer
	 */
	public MetaDataDrivenConversionService(MetaData metaData) {
		metaData.registerConversionCallback(this);

		for (ClassInfo classInfo : metaData.persistentEntities()) {
			for (FieldInfo fieldInfo : classInfo.propertyFields()) {
				if (fieldInfo.hasPropertyConverter()) {
					addWrappedConverter(fieldInfo.getPropertyConverter());
				}
			}
			// TODO: do we need to check the setters too or are programmers obliged to annotate both?
			for (MethodInfo methodInfo : classInfo.propertyGetters()) {
				if (methodInfo.hasPropertyConverter()) {
					addWrappedConverter(methodInfo.getPropertyConverter());
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addWrappedConverter(final AttributeConverter attributeConverter) {
		if (attributeConverter instanceof ProxyAttributeConverter) {
			return;
		}

		Converter toGraphConverter = new Converter() {
			@Override
			public Object convert(Object source) {
				return attributeConverter.toGraphProperty(source);
			}
		};
		Converter toEntityConverter = new Converter() {
			@Override
			public Object convert(Object source) {
				return attributeConverter.toEntityAttribute(source);
			}
		};

		ParameterizedType pt = (ParameterizedType) attributeConverter.getClass().getGenericInterfaces()[0];
		Class<?> sourceType, targetType;
		if (pt.getActualTypeArguments()[0] instanceof Class) {
			sourceType = (Class<?>) pt.getActualTypeArguments()[0];
		} else { // the argument may be a Collection for example
			sourceType = (Class<?>) ((ParameterizedType) pt.getActualTypeArguments()[0]).getActualTypeArguments()[0];
		}

		if (pt.getActualTypeArguments()[1] instanceof Class) {
			targetType = (Class<?>) pt.getActualTypeArguments()[1];
		} else {
			targetType = (Class<?>) ((ParameterizedType) pt.getActualTypeArguments()[1]).getActualTypeArguments()[1];

		}

		if (canConvert(sourceType, targetType) && canConvert(targetType, sourceType)) {
			logger.info("Not adding Spring-compatible converter for " + attributeConverter.getClass()
					+ " because one that does the same job has already been registered with the ConversionService.");
		} else {
			// It could be argued that this is wrong as it potentially overrides a registered converted that doesn't handle
			// both directions, but I've decided that it's better to ensure the same converter is used for load and save.
			addConverter(sourceType, targetType, toGraphConverter);
			addConverter(targetType, sourceType, toEntityConverter);
		}
	}

	@Override
	public <T> T convert(Class<T> targetType, Object value) {
		if (value == null) {
			return null;
		}
		return convert(value, targetType);
	}

}
