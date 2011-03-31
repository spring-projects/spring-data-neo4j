package org.springframework.data.graph.core;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;

import javax.annotation.PreDestroy;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;

/**
 * @author mh
 * @since 25.01.11
 */
public class GraphDatabaseFactory implements FactoryBean<GraphDatabase> {

    private String storeLocation;
    private String userName;
    private String password;
    protected GraphDatabase graphDatabase;

    public String getStoreLocation() {
        return storeLocation;
    }

    public void setStoreLocation(String storeLocation) {
        this.storeLocation = storeLocation;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private GraphDatabase databaseFor(String url, String username, String password) throws Exception {
        if (url.startsWith( "http://" ) || url.startsWith( "https://" )) {
            return createRestGraphDatabase(url, username, password);
        }
        String path=url;
        if (url.startsWith( "file:" )) {
            path = new URI(url).getPath();
        }
        File file = new File( path );
        // if (!file.isDirectory()) file=file.getParentFile();
        return new DelegatingGraphDatabase(new EmbeddedGraphDatabase(file.getAbsolutePath()));
    }

    private GraphDatabase createRestGraphDatabase(String url, String username, String password) throws Exception {
        Class<?> restGraphDatabaseClass = Class.forName("org.neo4j.rest.graphdb.RestGraphDatabase");
        Constructor<?> constructor = restGraphDatabaseClass.getConstructor(URI.class, String.class, String.class);
        return (GraphDatabase) constructor.newInstance(new URI(url), username,password );
    }

    @Override
    public GraphDatabase getObject() throws Exception {
        if (graphDatabase==null) graphDatabase = databaseFor(storeLocation, userName, password);
        return graphDatabase;
    }

    @PreDestroy
    public void shutdown() {
        if (graphDatabase instanceof DelegatingGraphDatabase) {
           ((DelegatingGraphDatabase)graphDatabase).shutdown();
        }
    }

    @Override
    public Class<?> getObjectType() {
        return GraphDatabaseService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
