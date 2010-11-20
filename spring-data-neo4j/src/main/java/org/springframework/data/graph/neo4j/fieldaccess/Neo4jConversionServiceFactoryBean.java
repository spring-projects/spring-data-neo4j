/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;

import java.util.Date;

public class Neo4jConversionServiceFactoryBean implements FactoryBean<ConversionService>
{

    @Override
    public ConversionService getObject() throws Exception
    {
        GenericConversionService conversionService = new GenericConversionService();
        addConverters(conversionService);
        ConversionServiceFactory.addDefaultConverters( conversionService );
        return conversionService;
    }

    private void addConverters( GenericConversionService conversionService )
    {
        conversionService.addConverter(new DateToLongConverter());
        conversionService.addConverter(new LongToDateConverter());
        conversionService.addConverter(new EnumToStringConverter());
        conversionService.addConverterFactory(new StringToEnumConverterFactory());
    }

    @Override
    public Class<?> getObjectType()
    {
        return GenericConversionService.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    public static class DateToLongConverter implements Converter<Date, String> {

        @Override
        public String convert( Date source )
        {
            return String.valueOf(source.getTime());
        }
    }

    public static class LongToDateConverter implements Converter<String, Date> {

        @Override
        public Date convert( String source )
        {
            return new Date(Long.valueOf(source));
        }
    }

    public static class EnumToStringConverter implements Converter<Enum, String> {

        @Override
        public String convert( Enum source )
        {
            return source.name();
        }
    }

    public static class StringToEnumConverterFactory implements ConverterFactory<String, Enum> {

        public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
            return new StringToEnum(targetType);
        }

        private static class StringToEnum<T extends Enum> implements Converter<String, T> {

            private final Class<T> enumType;

            public StringToEnum(Class<T> enumType) {
                this.enumType = enumType;
            }
            public T convert(String source) {
                if (source == null) return null;
                final String trimmed=source.trim();
                if (trimmed.isEmpty()) return null;
                return Enum.valueOf(this.enumType, trimmed);
            }

        }

    }
}
