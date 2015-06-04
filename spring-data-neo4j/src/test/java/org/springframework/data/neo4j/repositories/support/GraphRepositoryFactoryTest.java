package org.springframework.data.neo4j.repositories.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.neo4j.ogm.session.Session;
import org.springframework.aop.framework.Advised;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.repository.GraphRepositoryImpl;
import org.springframework.data.neo4j.repository.support.GraphRepositoryFactory;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

/**
 * Unit tests for {@code GraphRepositoryFactory}.
 *
 * @author: Vince Bickers
 */
@RunWith(MockitoJUnitRunner.class)
public class GraphRepositoryFactoryTest {

    GraphRepositoryFactory factory;

    @Mock org.neo4j.ogm.session.Session session;
    @Mock org.springframework.data.neo4j.mapping.Neo4jMappingContext mappingContext;

    @Before
    public void setUp() {

        factory = new GraphRepositoryFactory(session, mappingContext) {

        };
    }

    /**
     * Assert that the instance created for the standard configuration is a valid {@code UserRepository}.
     *
     * @throws Exception
     */
    @Test
    public void setsUpBasicInstanceCorrectly() throws Exception {
        assertNotNull(factory.getRepository(ObjectRepository.class));
    }

    @Test
    public void allowsCallingOfObjectMethods() {

        ObjectRepository repository = factory.getRepository(ObjectRepository.class);

        repository.hashCode();
        repository.toString();
        repository.equals(repository);
    }

    @Test
    public void usesConfiguredRepositoryBaseClass() {
        factory.setRepositoryBaseClass(CustomGraphRepository.class);
        ObjectRepository repository = factory.getRepository(ObjectRepository.class);
        assertEquals(CustomGraphRepository.class, ((Advised) repository).getTargetClass());
    }

    private interface ObjectRepository extends GraphRepository<Object> {
        @Transactional
        Object findOne(Long id);
    }

    static class CustomGraphRepository<T> extends GraphRepositoryImpl<T> {
        public CustomGraphRepository(Class<T> clazz, Session session) {
            super(clazz, session);
        }
    }
}
