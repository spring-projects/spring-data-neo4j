package org.springframework.data.neo4j.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.support.node.Neo4jNodeBacking;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.relationship.Neo4jRelationshipBacking;

/**
 * @author mh
 * @since 30.09.11
 */
public class Neo4jAspectConfiguration extends Neo4jConfiguration {
    @Bean
    public Neo4jRelationshipBacking neo4jRelationshipBacking() throws Exception {
        Neo4jRelationshipBacking aspect = Neo4jRelationshipBacking.aspectOf();
        aspect.setGraphDatabaseContext(graphDatabaseContext());
aspect.setRelationshipEntityStateFactory(relationshipEntityStateFactory());
        return aspect;
    }

    @Bean
	public Neo4jNodeBacking neo4jNodeBacking() throws Exception {
		Neo4jNodeBacking aspect = Neo4jNodeBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext());
        NodeEntityStateFactory entityStateFactory = nodeEntityStateFactory();
		aspect.setNodeEntityStateFactory(entityStateFactory);
		return aspect;
	}
}
