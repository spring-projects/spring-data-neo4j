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

package org.springframework.data.neo4j.config;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.neo4j.config.model.TestEntity;
import org.springframework.data.neo4j.model.PersonRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.Neo4jPersistentEntityImpl;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 31.01.11
 */
public class DataGraphNamespaceHandlerTests {

    static class Config {
        @Autowired
        GraphDatabaseService graphDatabaseService;
        @Autowired
        Neo4jTemplate neo4jTemplate;
        @Autowired
        PlatformTransactionManager transactionManager;
        @Autowired(required = false)
        Neo4jMappingContext mappingContext;
        @Autowired(required = false)
        PersonRepository personRepository;
    }

    public static class TestConverter implements GenericConverter {
        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(Config.class, Integer.class));
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return source.hashCode();
        }
    }
    @Test
    public void injectionForJustStoreDir() {
        final Config config = assertInjected("");
        Assert.assertNotNull(config.personRepository);
    }
    @Test                                                                       
    public void injectionForExistingGraphDatabaseService() {
        final Config config = assertInjected("-external-embedded");
        final GraphDatabaseAPI gds = (GraphDatabaseAPI) config.graphDatabaseService;
        assertEquals(EmbeddedGraphDatabase.class, gds.getClass());
        final org.neo4j.kernel.configuration.Config neoConfig = gds.getDependencyResolver().resolveDependency(org.neo4j.kernel.configuration.Config.class);
        assertEquals("true", neoConfig.getParams().get("allow_store_upgrade"));
    }
    @Test
    @Ignore("todo setup zk-cluster")
    public void injectionForExistingHighlyAvailableGraphDatabaseService() {
        final Config config = assertInjected("-external-ha");
        final GraphDatabaseAPI gds = (GraphDatabaseAPI) config.graphDatabaseService;
        final org.neo4j.kernel.configuration.Config neoConfig = gds.getDependencyResolver().resolveDependency(org.neo4j.kernel.configuration.Config.class);
        assertEquals("HighlyAvailableGraphDatabase", gds.getClass().getSimpleName());
        assertEquals("1", neoConfig.getParams().get("ha.server_id"));
    }

    @Test
    public void injectionForCodeConfiguredExistingGraphDatabaseService() {
        assertInjected("-code");
    }
    @Test
    public void injectionForBasePackageOfEntities() {
        Config config = assertInjected("-entities");
        Collection<Neo4jPersistentEntityImpl<?>> entities = config.mappingContext.getPersistentEntities();
        assertTrue(entities.size() > 0);
        boolean found = false;
        for (Neo4jPersistentEntityImpl<?> entity : entities) {
            found |= TestEntity.class.equals(entity.getType());
        }
        assertEquals(true, found);
    }
    @Test
    public void injectionForConversionService() {
        final Config config = assertInjected("-conversion");
        final ConversionService conversionService = config.neo4jTemplate.getConversionService();
        assertEquals(true, conversionService.canConvert(Enum.class,String.class));
        assertEquals(true, conversionService.canConvert(Config.class, Integer.class));
    }

    private Config assertInjected(String testCase) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:org/springframework/data/neo4j/config/DataGraphNamespaceHandlerTests" + testCase + "-context.xml");
        try {
        Config config = ctx.getBean("config", Config.class);
        Neo4jTemplate template = config.neo4jTemplate;
        Assert.assertNotNull("template", template);
        AbstractGraphDatabase graphDatabaseService = (AbstractGraphDatabase) template.getGraphDatabaseService();
        String fileSeparator = "target" + System.getProperty("file.separator") + "config-test";
        Assert.assertTrue("store-dir", graphDatabaseService.getStoreDir().endsWith(fileSeparator));
        Assert.assertNotNull("graphDatabaseService",config.graphDatabaseService);
        Assert.assertNotNull("transactionManager",config.transactionManager);
        config.graphDatabaseService.shutdown();
        return config;
        } finally {
            ctx.close();
        }
    }

}
