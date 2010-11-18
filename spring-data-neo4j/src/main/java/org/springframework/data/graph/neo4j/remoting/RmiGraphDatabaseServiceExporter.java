/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.remoting;

import java.net.MalformedURLException;
import java.rmi.RemoteException;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.remote.transports.LocalGraphDatabase;
import org.neo4j.remote.transports.RmiTransport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class RmiGraphDatabaseServiceExporter implements InitializingBean {
	
	private static final String DEFAULT_RESOURCE_URI = "rmi://localhost/neo4j-graphdb";
	
	private String resourceUri = DEFAULT_RESOURCE_URI;
	
	@Autowired
	GraphDatabaseService neo;

	
    public void setResourceUri(String resourceUri) {
		this.resourceUri = resourceUri;
	}

	public void publishServer(GraphDatabaseService neo) throws MalformedURLException, RemoteException {
        System.out.println("publishing RMI service at = " + resourceUri);
    	RmiTransport.register(new LocalGraphDatabase(neo), resourceUri);
    }

	@Override
	public void afterPropertiesSet() throws Exception {
		if (neo != null) {
			publishServer(this.neo);
		}
	}
}
