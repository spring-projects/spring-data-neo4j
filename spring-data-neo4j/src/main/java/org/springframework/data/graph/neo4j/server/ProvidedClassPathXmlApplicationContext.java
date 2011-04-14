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

package org.springframework.data.graph.neo4j.server;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Context that merges the provided graph database service with the given context locations,
 * so that spring beans that consume a graph database are populated properly.
 */
public class ProvidedClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

    private final GraphDatabaseService database;

    public ProvidedClassPathXmlApplicationContext(GraphDatabaseService database, final String... locations)
            throws org.springframework.beans.BeansException {
        super();
        setConfigLocations(locations);
        this.database = database;
        refresh();
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        super.prepareBeanFactory(beanFactory);
        beanFactory.registerResolvableDependency(GraphDatabaseService.class, database);
        beanFactory.registerSingleton("graphDatabaseService", database);
    }
}