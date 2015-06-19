/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.conversion;

import java.lang.reflect.ParameterizedType;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.neo4j.ogm.typeconversion.ConversionCallback;
import org.neo4j.ogm.typeconversion.ProxyAttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * Specialisation of {@link GenericConversionService} that creates Spring-compatible converters from those known by the mapping
 * {@link MetaData}, allowing the OGM type converters to be reused throughout a Spring application.
 *
 * @author Adam George
 */
public class MetaDataDrivenConversionService extends GenericConversionService implements ConversionCallback {

    private static final Logger logger = LoggerFactory.getLogger(MetaDataDrivenConversionService.class);

    /**
     * Constructs a new {@link MetaDataDrivenConversionService} based on the given {@link MetaData}.
     *
     * @param metaData The OGM {@link MetaData} from which to elicit type converters configured in the underlying object-graph
     *        mapping layer
     */
    public MetaDataDrivenConversionService(MetaData metaData) {
        metaData.registerConversionCallback(this);

        for (ClassInfo classInfo : metaData.persistentEntities()) {
            for (FieldInfo fieldInfo : classInfo.propertyFields()) {
                if (fieldInfo.hasConverter()) {
                    addWrappedConverter(fieldInfo.converter());
                }
            }
            // TODO: do I need to check the setters too or are programmers obliged to annotate both?
            for (MethodInfo methodInfo : classInfo.propertyGetters()) {
                if (methodInfo.hasConverter()) {
                    addWrappedConverter(methodInfo.converter());
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addWrappedConverter(final AttributeConverter attributeConverter) {
        if (attributeConverter instanceof ProxyAttributeConverter) {
            return;
        }

        Converter<?, ?> toGraphConverter = new Converter() {
            @Override
            public Object convert(Object source) {
                return attributeConverter.toGraphProperty(source);
            }
        };
        Converter<?, ?> toEntityConverter = new Converter() {
            @Override
            public Object convert(Object source) {
                return attributeConverter.toEntityAttribute(source);
            }
        };

        ParameterizedType pt = (ParameterizedType) attributeConverter.getClass().getGenericInterfaces()[0];
        Class<?> sourceType = (Class<?>) pt.getActualTypeArguments()[0];
        Class<?> targetType = (Class<?>) pt.getActualTypeArguments()[1];

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
    public <T> T convert(Class<?> sourceType, Class<T> targetType, Object value) {
        // FIXME: ahh, the problem is we always need the target type!!
        // - keep a list of convertible pairs and find one that matches, perhaps?
        // - or try all possible combinations of stuff you can stick in a graph? (probably not!)
        // - how about we make you specify the target type instead of the converter in the @Convert annotation?
        return this.convert(value, targetType);
    }

}
