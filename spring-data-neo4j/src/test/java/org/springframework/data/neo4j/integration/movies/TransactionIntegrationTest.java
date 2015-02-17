package org.springframework.data.neo4j.integration.movies;

import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.movies.context.PersistenceContext;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.integration.movies.repo.UserRepository;
import org.springframework.data.neo4j.integration.movies.service.UserService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@ContextConfiguration(classes = {PersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionIntegrationTest extends WrappingServerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Override
    protected int neoServerPort() {
        return 7879;
    }

    @Override
    protected void populateDatabase(GraphDatabaseService database) {
        super.populateDatabase(database);
        database.registerTransactionEventHandler(new TransactionEventHandler.Adapter<Object>() {
            @Override
            public Object beforeCommit(TransactionData data) throws Exception {
                System.out.println("The request to commit is denied");
                throw new TransactionInterceptException("Deliberate testing exception");
                // the exception here does not get propagated to the caller if we're.
            }
        }) ;
    }


    @Test(expected = Exception.class)
    public void whenImplicitTransactionFailsNothingShouldBeCreated() {
        try {
            userRepository.save(new User("Michal"));
            fail("should have thrown exception");
        } catch (Exception e) {
            parseExceptionMessage(e.getLocalizedMessage());
            checkDatabase();
        }

    }

    private void parseExceptionMessage(String localizedMessage) {
        String parsed = localizedMessage.replace("{", "{\n");
        parsed = parsed.replace("\\n\\tat", "\n\tat");
        parsed = parsed.replace("},{", "},\n{");
        parsed = parsed.replace("\\n", "\n");

        System.out.println(parsed);

    }

    @Test(expected = Exception.class)
    public void whenExplicitTransactionFailsNothingShouldBeCreated() {
        try {
            userService.saveWithTxAnnotationOnInterface(new User("Michal"));
            fail("should have thrown exception");
        } catch (Exception e) {
            parseExceptionMessage(e.getLocalizedMessage());
            checkDatabase();
        }

    }

    @Test(expected = Exception.class)
    public void whenExplicitTransactionFailsNothingShouldBeCreated2() {
        try {
            userService.saveWithTxAnnotationOnImpl(new User("Michal"));
            fail("should have thrown exception");
        } catch (Exception e) {
            parseExceptionMessage(e.getLocalizedMessage());
            checkDatabase();
        }
    }

    private void checkDatabase() {
        try (Transaction tx = getDatabase().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getDatabase()).getAllNodes().iterator().hasNext());
            tx.success();
        }
    }

    static class TransactionInterceptException extends Exception {
        public TransactionInterceptException(String msg) {
            super(msg);
        }
    }

}
