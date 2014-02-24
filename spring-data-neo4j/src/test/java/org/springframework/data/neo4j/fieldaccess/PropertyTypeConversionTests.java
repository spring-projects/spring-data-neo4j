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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.CRUDRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

class CustomType {
    String value;

    CustomType(String value) {
        this.value = value;
    }
}

@NodeEntity
class EntityWithCustomTypeProperty {
    @GraphId
    Long id;

    CustomType customTypeConvertedToString;

    @GraphProperty(propertyType = byte[].class)
    CustomType customTypeConvertedToByteArray;

    @GraphProperty(propertyType = boolean.class)
    CustomType unConvertibleCustomType;

    EntityWithCustomTypeProperty() {
    }

    EntityWithCustomTypeProperty(CustomType customTypeConvertedToString, CustomType customTypeConvertedToByteArray, CustomType unConvertibleCustomType) {
        this.customTypeConvertedToString = customTypeConvertedToString;
        this.customTypeConvertedToByteArray = customTypeConvertedToByteArray;
        this.unConvertibleCustomType = unConvertibleCustomType;
    }
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class PropertyTypeConversionTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        TestConfig() throws ClassNotFoundException {
            setBasePackage(EntityWithCustomTypeProperty.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }

        @Override
        protected ConversionService neo4jConversionService() throws Exception {
            ConverterRegistry converterRegistry = (ConverterRegistry) super.neo4jConversionService();

            converterRegistry.addConverter(new Converter<CustomType, String>() {
                @Override
                public String convert(CustomType source) {
                    return source.value + " encoded using string";
                }
            });
            converterRegistry.addConverter(new Converter<String, CustomType>() {
                @Override
                public CustomType convert(String source) {
                    return new CustomType(source + " decoded using string");
                }
            });

            converterRegistry.addConverter(new Converter<CustomType, byte[]>() {
                @Override
                public byte[] convert(CustomType source) {
                    return (source.value + " encoded using byte array").getBytes();
                }
            });
            converterRegistry.addConverter(new Converter<byte[], CustomType>() {
                @Override
                public CustomType convert(byte[] source) {
                    return new CustomType(new String(source) + " decoded using byte array");
                }
            });

            return (ConversionService) converterRegistry;
        }
    }

    @Autowired
    Neo4jTemplate template;

    CRUDRepository<EntityWithCustomTypeProperty> fooRepository;

    @Before
    public void before() {
        fooRepository = template.repositoryFor(EntityWithCustomTypeProperty.class);
    }

    @Test
    public void shouldConvertCustomTypeUsingDefaultPropertyType() throws Exception {
        EntityWithCustomTypeProperty entityWithCustomTypeProperty = fooRepository.save(new EntityWithCustomTypeProperty(new CustomType("some value"), null, null));
        entityWithCustomTypeProperty = fooRepository.findOne(entityWithCustomTypeProperty.id);

        assertThat(entityWithCustomTypeProperty.customTypeConvertedToString.value, is("some value encoded using string decoded using string"));
    }

    @Test
    public void shouldConvertCustomTypeUsingParameterisedPropertyType() throws Exception {
        EntityWithCustomTypeProperty entityWithCustomTypeProperty = fooRepository.save(new EntityWithCustomTypeProperty(null, new CustomType("some value"), null));
        entityWithCustomTypeProperty = fooRepository.findOne(entityWithCustomTypeProperty.id);

        assertThat(entityWithCustomTypeProperty.customTypeConvertedToByteArray.value, is("some value encoded using byte array decoded using byte array"));
    }

    @Test
    public void shouldNotConvertCustomTypeWhenNoConverterExists() throws Exception {
        EntityWithCustomTypeProperty entityWithCustomTypeProperty = fooRepository.save(new EntityWithCustomTypeProperty(null, null, new CustomType("some value")));
        entityWithCustomTypeProperty = fooRepository.findOne(entityWithCustomTypeProperty.id);

        assertThat(entityWithCustomTypeProperty.unConvertibleCustomType, is(nullValue()));
    }
}
