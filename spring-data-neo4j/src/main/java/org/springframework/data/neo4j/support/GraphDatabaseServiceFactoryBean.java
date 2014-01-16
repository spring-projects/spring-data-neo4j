package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.beans.factory.FactoryBean;

import java.util.Map;

/**
 * @author mh
 * @since 04.01.14
 */
public class GraphDatabaseServiceFactoryBean implements FactoryBean<GraphDatabaseService> {
    private String path;
    private Map<String,String> config;
    private GraphDatabaseService database;

    public GraphDatabaseServiceFactoryBean(String path, Map<String,String> config) {
        this.path = path;
        this.config = config;
    }

    public GraphDatabaseServiceFactoryBean(String path) {
        this.path = path;
    }

    public GraphDatabaseServiceFactoryBean() {
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setConfig(Map<String,String> config) {
        this.config = config;
    }

    @Override
    public GraphDatabaseService getObject() throws Exception {
        if (database != null) return database;
        return database = createDatabase();
    }

    private GraphDatabaseService createDatabase() {
        GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(path);
        if (config != null) {
            builder.setConfig(config);
        }
        return builder.newGraphDatabase();
    }

    @Override
    public Class<?> getObjectType() {
        return GraphDatabaseService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void shutdown() {
        if (database != null) {
            database.shutdown();
        }
    }
}
