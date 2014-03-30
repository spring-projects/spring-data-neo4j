/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.fieldaccess;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Shape;
import org.springframework.data.neo4j.repository.GeoConverter;

import java.util.Date;

public class Neo4jConversionServiceFactoryBean implements FactoryBean<ConversionService> {

    @Override
    public ConversionService getObject() throws Exception {
        GenericConversionService conversionService = new GenericConversionService();
        addConverters(conversionService);
        DefaultConversionService.addDefaultConverters(conversionService);
        return conversionService;
    }

    public void addConverters(ConversionService service) {
        if (service instanceof ConverterRegistry) {
            ConverterRegistry registry = (ConverterRegistry) service;
            registry.addConverter(new DateToStringConverter());
            registry.addConverter(new DateToLongConverter());
            registry.addConverter(new StringToDateConverter());
            registry.addConverter(new NumberToDateConverter());
            registry.addConverter(new EnumToStringConverter());
            registry.addConverter(new ShapeToStringConverter());
            registry.addConverter(new StringToShapeConverter());
            registry.addConverter(new PointToStringConverter());
            registry.addConverter(new StringToPointConverter());
            registry.addConverterFactory(new StringToEnumConverterFactory());
        } else {
            throw new IllegalArgumentException("conversionservice is no ConverterRegistry:" + service);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return GenericConversionService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public static class DateToStringConverter implements Converter<Date, String> {

        @Override
        public String convert(Date source) {
            return String.valueOf(source.getTime());
        }
    }

    public static class DateToLongConverter implements Converter<Date, Long> {

        @Override
        public Long convert(Date source) {
            return source.getTime();
        }
    }

    public static class NumberToDateConverter implements Converter<Number, Date> {

        @Override
        public Date convert(Number source) {
            return new Date(source.longValue());
        }
    }
    public static class StringToDateConverter implements Converter<String, Date> {

        @Override
        public Date convert(String source) {
            return new Date(Long.valueOf(source));
        }
    }

    public static class EnumToStringConverter implements Converter<Enum, String> {

        @Override
        public String convert(Enum source) {
            return source.name();
        }
    }

    public static class ShapeToStringConverter implements Converter<Shape, String> {

        @Override
        public String convert(Shape source) {
            return GeoConverter.toWellKnownText(source);
        }
    }
    public static class PointToStringConverter implements Converter<Point, String> {

        @Override
        public String convert(Point source) {
            return GeoConverter.toWellKnownText(source);
        }
    }

    public static class StringToShapeConverter implements Converter<String, Shape> {

        @Override
        public Shape convert(String source) {
            return GeoConverter.fromWellKnownText(source);
        }
    }

    public static class StringToPointConverter implements Converter<String, Point> {

        @Override
        public Point convert(String source) {
            return GeoConverter.pointFromWellKnownText(source);
        }
    }

    public static class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

        @SuppressWarnings("unchecked")
        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return new StringToEnum(targetType);
        }

        private static class StringToEnum<T extends Enum> implements Converter<String, T> {

            private final Class<T> enumType;

            public StringToEnum(Class<T> enumType) {
                this.enumType = enumType;
            }

            @SuppressWarnings("RedundantCast")
            public T convert(String source) {
                if (source == null) return null;
                final String trimmed = source.trim();
                if (trimmed.isEmpty()) return null;
                return (T) Enum.valueOf(this.enumType, trimmed);
            }

        }

    }
}
