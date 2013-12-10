package org.springframework.data.neo4j.support.mapping;

import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.data.neo4j.support.Infrastructure;
import org.springframework.data.neo4j.support.MappingInfrastructureFactoryBean;

/**
 * @author mh
 * @since 11.11.13
 */
public class EntityRemoverTest {
    @Test
    public void testRemoveNodeEntityWithAutoIndex() throws Exception {
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        Transaction tx = db.beginTx();
        try {
            AutoIndexer<Node> nodeAutoIndexer = db.index().getNodeAutoIndexer();
            nodeAutoIndexer.setEnabled(true);
            nodeAutoIndexer.startAutoIndexingProperty("foo");
            Infrastructure infrastructure = MappingInfrastructureFactoryBean.createDirect(db, null);
            Node node = db.createNode();
            node.setProperty("foo", "bar");
            infrastructure.getEntityRemover().remove(node);
            node = db.createNode();
            node.setProperty("foo", "bar");
            infrastructure.getGraphDatabase().remove(node);
            tx.success();
        } finally {
            tx.finish();
        }
    }
    @Test
    public void testRemoveRelationshipEntityWithAutoIndex() throws Exception {
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        Transaction tx = db.beginTx();
        try {
            AutoIndexer<Relationship> autoIndexer = db.index().getRelationshipAutoIndexer();
            autoIndexer.setEnabled(true);
            autoIndexer.startAutoIndexingProperty("foo");
            Infrastructure infrastructure = MappingInfrastructureFactoryBean.createDirect(db, null);
            final Node node = db.createNode();

            Relationship relationship = node.createRelationshipTo(node, DynamicRelationshipType.withName("KNOWS"));
            relationship.setProperty("foo", "bar");
            infrastructure.getEntityRemover().remove(relationship);

            relationship = node.createRelationshipTo(node, DynamicRelationshipType.withName("KNOWS"));
            relationship.setProperty("foo", "bar");
            infrastructure.getGraphDatabase().remove(relationship);
            tx.success();
        } finally {
            tx.finish();
        }
    }
}
