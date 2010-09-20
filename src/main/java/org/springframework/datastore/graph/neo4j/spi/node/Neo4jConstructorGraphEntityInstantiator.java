package org.springframework.datastore.graph.neo4j.spi.node;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.AbstractConstructorEntityInstantiator;

/**
 * Try for a constructor taking a Neo4j Node: failing that, try a no-arg
 * constructor and then setUnderlyingState().
 * 
 * @author Rod Johnson
 */
public class Neo4jConstructorGraphEntityInstantiator extends AbstractConstructorEntityInstantiator<NodeBacked, Node>{
	
	@Override
	protected void setState(NodeBacked entity, Node s) {
		entity.setUnderlyingState(s);
	}

}
