/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.neo4j.repository.cdi;


import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.GraphDatabaseFactory;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

/**
 * Simple component exposing {@link Neo4jTemplate} and {@link Neo4jMappingContext}
 * instances as CDI bean.
 * 
 * @author Nicki Watt
 */
class Neo4jCDIProducer {

	@Produces
	@ApplicationScoped
	public Neo4jTemplate createNeo4jTemplate()  {
        return new Neo4jTemplate(createGraphDatabase());
	}

    //@Produces
    //@ApplicationScoped
    private GraphDatabase createGraphDatabase()  {

        GraphDatabaseFactory factory = new GraphDatabaseFactory();
        GraphDatabase graphDatabase = null;
        try {
            factory.setStoreLocation("target/cdi-test-db");
            graphDatabase = factory.getObject();
            registerShutdownHook(graphDatabase);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start up factory");
        }

        return graphDatabase;

    }

    private static void registerShutdownHook( final GraphDatabase graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                ((DelegatingGraphDatabase)graphDb).getGraphDatabaseService().shutdown();
            }
        } );
    }

    @Produces
    @ApplicationScoped
    public Neo4jMappingContext createNeo4jMappingContext()  {
        return new Neo4jMappingContext();
    }

}
