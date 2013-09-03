package org.springframework.data.neo4j.entity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repository.*;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collection;
import java.util.Set;

import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;


/**
 * These tests are all focused on testing @Query annotated fields on entities.
 *
 * @author Nicki Watt
 * @since 02.09.2013
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class EntityQueryTests {

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Autowired
    private PersonRepository personRepository;
    @Autowired
    GroupRepository groupRepository;
    @Autowired
    FriendshipRepository friendshipRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TestTeam testTeam;
    private MatrixTeam matrixTeam;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }

    @Before
    public void setUp() throws Exception {
        testTeam = new TestTeam();
        matrixTeam = new MatrixTeam();
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                personRepository.deleteAll();
                groupRepository.deleteAll();
                friendshipRepository.deleteAll();
                testTeam.createSDGTeam(personRepository, groupRepository, friendshipRepository);
                matrixTeam.createMatrixTeam(personRepository, groupRepository, friendshipRepository);
            }
        });
    }


    @Test // Test for DATAGRAPH-257
    public void testPropertyQueryOfTypeSetPopulatedCorrectly() {
        Group grp = neo4jTemplate.findOne(matrixTeam.matrixGroup.getId(), Group.class);
        Set<Person> members = grp.getTeamMembersAsSetViaQuery();
        assertThat( members, hasSize(3));
        assertThat( members, hasItems( matrixTeam.neo, matrixTeam.trinity, matrixTeam.cypher));

        // assertExpectionsOfMatrixTeamSetResult(members);
        // We only get the id's when its a field - is this correct?
        // Added @Fetch brings the data back - however causes other problems
        // (breaks Neo4jEntityConverterTests - investigate)
        assertNull(members.iterator().next().getName());
    }

    @Test  // Test for DATAGRAPH-257
    public void testPropertyQueryOfTypeIterablePopulatedCorrectly() {
        Group grp = neo4jTemplate.findOne(matrixTeam.matrixGroup.getId(), Group.class);
        Iterable<Person> itMembers = grp.getTeamMembersAsIterableViaQuery();
        assertNotNull(itMembers);
        Collection<Person> members = asCollection(itMembers);
        assertThat( members, hasSize(3));
        assertThat( members, hasItems( matrixTeam.neo, matrixTeam.trinity, matrixTeam.cypher));

        // assertExpectionsOfMatrixTeamSetResult(members);
        // Is this correct?
        // We only get the id's when its a field - is this correct?
        // Added @Fetch brings the data back - however causes other problems
        // (breaks Neo4jEntityConverterTests - investigate)
        assertNull(members.iterator().next().getName());
    }

    @Test // Test for DATAGRAPH-257
    public void testRepositoryQueryOfTypeSetPopulatedCorrectly() {

        // This is really for comparison purposes - comparing query via
        // a repository vs via an entity property (theoretically they should
        // return the same result)
        Set<Person> members = groupRepository.getTeamMembersAsSetViaQuery(matrixTeam.matrixGroup.getName());
        assertExpectionsOfMatrixTeamSetResult(members);
    }

    private void assertExpectionsOfMatrixTeamSetResult(Set<Person> members) {
        assertThat( members, hasSize(3));
        assertThat( members, hasItems( matrixTeam.neo, matrixTeam.trinity, matrixTeam.cypher));
        // Queries should return the fully populated object (at least at the first level)
        assertNotNull(members.iterator().next().getName());
    }

}
