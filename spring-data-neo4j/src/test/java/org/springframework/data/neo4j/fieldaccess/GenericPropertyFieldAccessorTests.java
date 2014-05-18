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

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GenericPropertyFieldAccessorTests {

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        TestConfig() throws ClassNotFoundException {
            setBasePackage(EntityWithGenericProperty.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Autowired Neo4jTemplate template;

    @Test
    @Transactional
    public void testGenericIntProperty() {
        assertSaveAndRetrieveValueUsingGenericProperty(123);
    }

    @Test
    @Transactional
    public void testGenericDoubleProperty() {
        assertSaveAndRetrieveValueUsingGenericProperty(3.1415);
    }

    @Test
    @Transactional
    public void testGenericBooleanProperty() {
        assertSaveAndRetrieveValueUsingGenericProperty(true);
    }

    @Test
    @Transactional
    public void testGenericIntegerProperty() {
        assertSaveAndRetrieveValueUsingGenericProperty(Integer.valueOf(3));
    }

    @Test
    @Transactional
    public void testTransientGenericPropertyNotSaved() {
        assertSaveAndRetrieveValueUsingTransientGenericProperty(Integer.valueOf(3));
    }

    @Test
    @Transactional
    public void testCustomGenericTypePropertyWithNoExplicitConverterThrowsException() {
        try {
            assertSaveAndRetrieveValueUsingGenericProperty(new CustomGenericType("custom-stuff"));
        } catch (Exception e) {
            boolean isExpectedSpring3Exception = e instanceof ConverterNotFoundException;
            boolean isExpectedSpring4Exception = e instanceof ConversionFailedException;
            assertTrue(isExpectedSpring3Exception || isExpectedSpring4Exception);
            return;
        }

        fail("An exception should have occurred");
    }

    @Ignore("Not supported without a custom converter")
    @Test
    @Transactional
    public void neo4jGenericCollectionsPropertyTestThrowsException() {
        try {
            List<Integer> col = new ArrayList<Integer>();
            col.add(1);
            col.add(5);
            col.add(10);
            assertSaveAndRetrieveValueUsingGenericProperty(col);  // Spring 3 results in assert failure (String vs collection)
        } catch (Exception e) {
            boolean isExpectedSpring4Exception = e instanceof ConversionFailedException;
            assertTrue(isExpectedSpring4Exception);
            return;
        }

        fail("An exception should have occurred already");
    }

    private <T> void assertSaveAndRetrieveValueUsingGenericProperty(final T value) {
        EntityWithGenericProperty entity = new EntityWithGenericProperty();
        entity.setGenericProperty(value);
        template.save(entity);
        final EntityWithGenericProperty loaded = template.findOne(entity.getId(), EntityWithGenericProperty.class);
        assertEquals(value, loaded.getGenericProperty());
    }

    private <T> void assertSaveAndRetrieveValueUsingTransientGenericProperty(final T value) {
        EntityWithGenericProperty entity = new EntityWithGenericProperty();
        entity.setTransientGenericProperty(value);
        template.save(entity);
        final EntityWithGenericProperty loaded = template.findOne(entity.getId(), EntityWithGenericProperty.class);
        assertNull("Value was transient and should not have been saved", loaded.getTransientGenericProperty());
    }

}

class CustomGenericType {
    String value;

    CustomGenericType(String value) {
        this.value = value;
    }
}

@NodeEntity
class EntityWithGenericProperty {
    @GraphId
    private Long id;
    private Object genericProperty;

    private transient Object transientGenericProperty;

    Long getId() {
        return id;
    }

    Object getGenericProperty() {
        return genericProperty;
    }

    void setGenericProperty(Object genericProperty) {
        this.genericProperty = genericProperty;
    }

    Object getTransientGenericProperty() {
        return transientGenericProperty;
    }

    void setTransientGenericProperty(Object transientGenericProperty) {
        this.transientGenericProperty = transientGenericProperty;
    }
}

