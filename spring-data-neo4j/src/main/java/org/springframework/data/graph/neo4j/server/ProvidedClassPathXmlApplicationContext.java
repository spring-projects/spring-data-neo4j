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

    public ProvidedClassPathXmlApplicationContext(GraphDatabaseService database, final String[] locations)
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