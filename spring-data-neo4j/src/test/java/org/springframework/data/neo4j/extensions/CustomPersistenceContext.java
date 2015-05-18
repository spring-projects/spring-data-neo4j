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
package org.springframework.data.neo4j.extensions;

import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 *
 * Note how the repository base class for all our repositories is overridden
 * using the 'repositoryBaseClass' attribute.
 * This annotation change allows all our repositories to easily extend one or more
 * additional interfaces.
 *
 * @author: Vince Bickers
 */
@Configuration
@EnableNeo4jRepositories(repositoryBaseClass = CustomGraphRepositoryImpl.class)
@EnableTransactionManagement
public class CustomPersistenceContext extends Neo4jConfiguration {

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.extensions.domain");
    }

    @Bean
    @Override
    public Neo4jServer neo4jServer() {
        return new RemoteServer("http://localhost:7879");
    }

    @Bean
    public ConversionService conversionService() {
        final DefaultConversionService conversionService = new DefaultConversionService();

        // perhaps this should all be wrapped in a new meta-data-driven implementation of ConversionService?
        Collection<ClassInfo> persistentEntities = getSessionFactory().metaData().persistentEntities();
        for (ClassInfo classInfo : persistentEntities) {
            //TODO: consider method-level converters too
            for (final FieldInfo fieldInfo : classInfo.fieldsInfo().fields()) {
                if (fieldInfo.hasConverter()) {
                    @SuppressWarnings("rawtypes")
                    Converter<?, ?> converter = new Converter() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public Object convert(Object source) {
                            return fieldInfo.converter().toGraphProperty(source);
                        }
                    };

                    ParameterizedType pt = (ParameterizedType) fieldInfo.converter().getClass().getGenericInterfaces()[0];
                    Type[] converterTypeParameters = pt.getActualTypeArguments();
                    //TODO: what about duplicates?
                    conversionService.addConverter(
                            (Class<?>) converterTypeParameters[0], (Class<?>) converterTypeParameters[1], converter);
                }
            }
        }

        return conversionService;
    }

}
