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
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.Date;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class SerializableGraphQueryRepositoryTests {

    protected final Logger log = LoggerFactory.getLogger(getClass());

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

    @Test @Transactional
    public void shouldBeAbleToTurnQueryResultsIntoAPOJO() throws Exception {
        MemberDataPOJO nickisMemberData = personRepository.findMemberDataPojo(serialTesters.nicki);
        assertPOJOContainsExpectedData(nickisMemberData);
    }

    @Test @Transactional
    public void shouldBeAbleToSerializeAndDeserializeEntity() throws Exception {
        Person anSDNUpgrader = personRepository.findOne(serialTesters.nicki.getId());
        assertEntityDetailsForPerson(anSDNUpgrader);
        Person aDeserializedSDNUpgrader = assertObjectCanBeSerializedAndDeserialized(anSDNUpgrader);
        assertEntityDetailsForPerson(aDeserializedSDNUpgrader);
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

    @Test @Transactional
    public void shouldBeAbleToSerializedPOJOReturnedFromQueryResult() throws Exception {
        MemberDataPOJO nickisOrigMemberData = personRepository.findMemberDataPojo(serialTesters.nicki);
        assertPOJOContainsExpectedData(nickisOrigMemberData);
        MemberDataPOJO nickisDeserMemberData = assertObjectCanBeSerializedAndDeserialized(nickisOrigMemberData);
        assertPOJOContainsExpectedData(nickisDeserMemberData);
    }

    public void assertPOJOContainsExpectedData(MemberDataPOJO pojo) throws Exception {
        assertNotNull(pojo);
        assertThat(pojo.getBoss(), is(serialTesters.tareq));
        assertThat(asCollection(pojo.getTeams()), hasItem(serialTesters.serialTesterGroup));
        assertThat(pojo.getAnInt(),  is(serialTesters.tareq.getAge()));
        assertThat(pojo.getAName(), is(serialTesters.tareq.getName()));
    }

    private <T> T assertObjectCanBeSerializedAndDeserialized(T someObject)  throws Exception {
        assertThat(someObject, instanceOf(Serializable.class));
        byte[] bos = serializeIt(someObject);
        return deserializeIt(bos);
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




}
