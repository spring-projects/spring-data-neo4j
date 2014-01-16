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

package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.neo4j.core.GraphDatabase;

import javax.annotation.PreDestroy;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;

/**
 * @author mh
 * @since 25.01.11
 */
public class GraphDatabaseFactoryBean implements FactoryBean<GraphDatabase> {

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
        return new DelegatingGraphDatabase(new org.neo4j.graphdb.factory.GraphDatabaseFactory().newEmbeddedDatabase(file.getAbsolutePath()));
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
