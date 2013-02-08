package backwardscompatibility;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(locations = "classpath:/backwardscompatibility.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class BackwardsCompatibilityTests
{
    @Autowired
    private Neo4jTemplate template;

    @Test
    public void shouldBeBackwardsCompatible() throws Exception
    {
        Transaction transaction = template.getGraphDatabase().beginTx();

        try
        {
            Node node = template.createNode();
            node.setProperty( "foo", "bar" );
            transaction.success();
        }
        catch ( Exception e )
        {
            transaction.failure();
        }
        finally
        {
            transaction.finish();
        }
    }
}
