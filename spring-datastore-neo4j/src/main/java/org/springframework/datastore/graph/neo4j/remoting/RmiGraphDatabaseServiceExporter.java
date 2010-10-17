package org.springframework.datastore.graph.neo4j.remoting;

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
