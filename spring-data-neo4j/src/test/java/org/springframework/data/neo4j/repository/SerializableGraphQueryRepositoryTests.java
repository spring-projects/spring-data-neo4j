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
import org.springframework.data.neo4j.repositories.FriendshipRepository;
import org.springframework.data.neo4j.repositories.GroupRepository;
import org.springframework.data.neo4j.repositories.PersonRepository;
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

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }

    @Before
    public void setUp() throws Exception {
        serialTesters = new SerialTesters();
        serialTesters.createUpgraderTeam(personRepository, groupRepository, friendshipRepository);
    }

    @Test @Transactional
    public void shouldBeAbleToTurnQueryResultIntoAPOJO() throws Exception {
        MemberDataPOJO nickisMemberData = personRepository.findMemberDataPojo(serialTesters.nicki);
        assertPOJOContainsExpectedData(nickisMemberData);
    }

    @Test @Transactional
    public void shouldBeAbleToSerializedPOJOReturnedFromQueryResult() throws Exception {
        MemberDataPOJO nickisOrigMemberData = personRepository.findMemberDataPojo(serialTesters.nicki);
        assertPOJOContainsExpectedData(nickisOrigMemberData);
        assertThat(nickisOrigMemberData, instanceOf(Serializable.class));
        byte[] bos = serializeIt(nickisOrigMemberData);
        MemberDataPOJO nickisDeserMemberData =  deserializeIt(bos);
        assertPOJOContainsExpectedData(nickisDeserMemberData);
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


}
