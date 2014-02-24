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

package org.springframework.data.neo4j.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.fieldaccess.ManagedFieldAccessorSet;
import org.springframework.data.neo4j.fieldaccess.ManagedPrefixedDynamicProperties;
import org.springframework.data.neo4j.fieldaccess.PrefixedDynamicProperties;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.FriendshipRepository;
import org.springframework.data.neo4j.repositories.GroupRepository;
import org.springframework.data.neo4j.repositories.PersonRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.template.GraphCallback;
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

import java.io.*;
import java.util.Date;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class SerializableEntityRepositoryTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    FriendshipRepository friendshipRepository;

    private SerialTesters serialTesters;
    private Date expectedBirthDate;


    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }

    @Before
    public void setUp() throws Exception {
        serialTesters = new SerialTesters();
        serialTesters.createUpgraderTeam(personRepository, groupRepository, friendshipRepository);
        expectedBirthDate = serialTesters.bdayFormatter.parse("01 JAN 2013 00:00:00");

    }

    @Test
    public void shouldBeAbleToSerializeAndDeserializeBasicEntityGraph() throws Exception {
        Person person = personRepository.findOne(serialTesters.nicki.getId());
        assertEntityDetailsForPerson(person);
        assertThat(person, instanceOf(Serializable.class));

        // Do it
        byte[] bos = serializeIt(person);
        Person aDeserializedPerson = deserializeIt(bos);

        // Verify its the same
        assertEntityDetailsForPerson(aDeserializedPerson);
    }

    @Test
    public void primitiveFieldShouldBeSerializedInOriginalForm() throws Exception {
        Person deserializedPerson = assertPreSerializationSetupThenGetDeserializedPerson();
        assertEquals(String.class, deserializedPerson.getName().getClass());
        assertEquals("Nicki", deserializedPerson.getName());
    }

    @Test
    public void primitiveFieldUpdatedOnDeserializedEntityShouldBeAbleToBeSavedBackToRepo() throws Exception {
        final Person deserializedPerson = assertPreSerializationSetupThenGetDeserializedPerson();
        assertEquals(String.class, deserializedPerson.getName().getClass());
        assertEquals("Nicki", deserializedPerson.getName());

        deserializedPerson.setName("New Name");
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                personRepository.save(deserializedPerson);
            }
        });

        Person personFromDB = personRepository.findOne(deserializedPerson.getId());
        assertEquals("New Name", personFromDB.getName());
    }

    @Test
    public void managedFieldAkaRelationshipsShouldBeSerializedAsAHashSet() throws Exception {
        Person deserializedPerson = assertPreSerializationSetupThenGetDeserializedPerson();
        assertEquals(HashSet.class, deserializedPerson.getSerialFriends().getClass());
        assertEquals(1, deserializedPerson.getSerialFriends().size());
        assertThat(deserializedPerson.getSerialFriends(), hasItem(serialTesters.michael));
    }

    @Test
    public void managedFieldAkaRelationshipUpdatedOnDeserializedEntityShouldBeAbleToBeSavedBackToRepo() throws Exception {
        final Person deserializedPerson = assertPreSerializationSetupThenGetDeserializedPerson();
        assertEquals(HashSet.class, deserializedPerson.getSerialFriends().getClass());
        assertEquals(1, deserializedPerson.getSerialFriends().size());
        assertThat(deserializedPerson.getSerialFriends(), hasItem(serialTesters.michael));

        deserializedPerson.addSerialFriend(serialTesters.david);
        assertEquals(2, deserializedPerson.getSerialFriends().size());
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                personRepository.save(deserializedPerson);
            }
        });

        Person personFromDB = personRepository.findOne(deserializedPerson.getId());
        assertEquals(2, personFromDB.getSerialFriends().size());
        assertThat(personFromDB.getSerialFriends(), hasItem(serialTesters.michael));
        assertThat(personFromDB.getSerialFriends(), hasItem(serialTesters.david));
    }


    @Test
    public void dynamicPropertiesFieldShouldBeSerializedAsAPrefixedDynamicProperties() throws Exception {
        final Person deserializedPerson = assertPreSerializationSetupThenGetDeserializedPerson();
        assertEquals(PrefixedDynamicProperties.class, deserializedPerson.getPersonalProperties().getClass());
        assertEquals(2, deserializedPerson.getPersonalProperties().asMap().size());
        assertThat(asCollection( deserializedPerson.getPersonalProperties().getPropertyKeys()) , hasItems("addressLine1","addressLine2"));
    }

    @Test
    public void dynamicPropertiesFieldUpdatedOnDeserializedEntityShouldBeAbleToBeSavedBackToRepo() throws Exception {
        final Person deserializedPerson = assertPreSerializationSetupThenGetDeserializedPerson();
        assertEquals(PrefixedDynamicProperties.class, deserializedPerson.getPersonalProperties().getClass());
        assertEquals(2, deserializedPerson.getPersonalProperties().asMap().size());
        assertThat(asCollection(deserializedPerson.getPersonalProperties().getPropertyKeys()) , hasItems("addressLine1", "addressLine2"));

        deserializedPerson.setProperty("newDynoProp", "newDynoValue");
        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                personRepository.save(deserializedPerson);
            }
        });

        Person personFromDB = personRepository.findOne(deserializedPerson.getId());
        assertEquals(3, personFromDB.getPersonalProperties().asMap().size());
        assertThat(asCollection(personFromDB.getPersonalProperties().getPropertyKeys()) , hasItems("addressLine1", "addressLine2", "newDynoProp"));

    }

    private Person assertPreSerializationSetupThenGetDeserializedPerson() throws Exception {
        return neo4jTemplate.exec(new GraphCallback<Person>() {
            @Override
            public Person doWithGraph(GraphDatabase graph) throws Exception {

                addSerialFriend(serialTesters.nicki.getId(), serialTesters.michael);

                // 1A. Make sure that before we deal with any serialization, we are still operating
                //    with the expected ManagedFieldAccessorSet class
                final Person person = personRepository.findOne(serialTesters.nicki.getId());
                assertEquals(ManagedFieldAccessorSet.class, person.getSerialFriends().getClass());
                assertEquals(1, person.getSerialFriends().size());

                // 1B. Make sure that before we deal with any serialization, we are still operating
                //    with the expected ManagedPrefixedDynamicProperties class
                assertEquals(ManagedPrefixedDynamicProperties.class, person.getPersonalProperties().getClass());
                assertEquals(2, person.getPersonalProperties().asMap().size());
                assertThat(asCollection(person.getPersonalProperties().getPropertyKeys()), hasItems("addressLine1", "addressLine2"));

                // 2. Do Serialization and return serialized object
                byte[] bos = serializeIt(person);
                return deserializeIt(bos);
            }

        });
    }


    private void addSerialFriend(final Long sourcePersonId, final Person target) {
        neo4jTemplate.exec(new GraphCallback.WithoutResult(){
            @Override
            public void doWithGraphWithoutResult(GraphDatabase graph) throws Exception {
                final Person person1 = personRepository.findOne(sourcePersonId);
                person1.addSerialFriend(target);
                personRepository.save(person1);
            }
        });
    }

    public void assertPOJOContainsExpectedData(MemberDataPOJO pojo) throws Exception {
        assertNotNull(pojo);
        assertThat(pojo.getBoss(), is(serialTesters.tareq));
        assertThat(asCollection(pojo.getTeams()), hasItem(serialTesters.serialTesterGroup));
        assertThat(pojo.getAnInt(),  is(serialTesters.tareq.getAge()));
        assertThat(pojo.getAName(), is(serialTesters.tareq.getName()));
    }

    private <T> byte[] serializeIt(T someObject) throws Exception {
        ObjectOutputStream out = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(someObject);
            return bos.toByteArray();
        } finally {
            if (out != null) out.close();
        }
    }

    private <T> T deserializeIt(byte[] serializedBytes) throws Exception {
        ObjectInputStream in = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serializedBytes);
            in = new ObjectInputStream(bis);
            Object theNewObj = in.readObject();
            return (T)theNewObj;
        } finally {
            if (in != null) in.close();
        }
    }

    private void assertEntityDetailsForPerson(Person aPerson) {
        assertThat(aPerson.getAge(), is(equalTo(36)));
        assertThat(aPerson.getBoss(), is(serialTesters.tareq));
        assertThat(aPerson.getBirthdate(), is(equalTo(expectedBirthDate)));
        assertThat(aPerson.getName(), is(equalTo("Nicki")));
        assertThat(aPerson.getDynamicProperty(), is(equalTo((Object)"What is this???")));
        assertThat(aPerson.getFriendships(), hasItems(serialTesters.friendShip2, serialTesters.friendShip3)) ;
        assertThat(aPerson.getHeight(), is(equalTo((short)100)));
        assertThat(aPerson.getProperty("addressLine1"), is(equalTo((Object)"Somewhere")));
        assertThat(aPerson.getProperty("addressLine2"), is(equalTo((Object)"Over the rainbow")));
    }

}
