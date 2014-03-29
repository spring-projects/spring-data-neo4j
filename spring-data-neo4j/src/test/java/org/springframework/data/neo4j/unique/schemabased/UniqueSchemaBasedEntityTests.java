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
package org.springframework.data.neo4j.unique.schemabased;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.unique.common.CommonClub;
import org.springframework.data.neo4j.unique.common.CommonUniqueClub;
import org.springframework.data.neo4j.unique.common.CommonUniqueEntityTestBase;
import org.springframework.data.neo4j.unique.common.CommonUniqueNumericIdClub;
import org.springframework.data.neo4j.unique.schemabased.domain.Club;
import org.springframework.data.neo4j.unique.schemabased.domain.UniqueClub;
import org.springframework.data.neo4j.unique.schemabased.domain.UniqueNumericIdClub;
import org.springframework.data.neo4j.unique.schemabased.repository.ClubRepository;
import org.springframework.data.neo4j.unique.schemabased.repository.UniqueClubRepository;
import org.springframework.data.neo4j.unique.schemabased.repository.UniqueNumericIdClubRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:unique-schema-test-context.xml"})
@Transactional
public class UniqueSchemaBasedEntityTests extends CommonUniqueEntityTestBase {

    @Autowired
    private Neo4jTemplate neo4jTemplate;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UniqueClubRepository uniqueClubRepository;

    @Autowired
    private UniqueNumericIdClubRepository uniqueNumericIdClubRepository;

    @Autowired
    protected GraphDatabaseService graphDatabaseService;

    @Before
    public void setup() {
        super.setup();
    }

    @Override
    protected void clearDownAllRepositories() {
        uniqueClubRepository.deleteAll();
        clubRepository.deleteAll();
        uniqueNumericIdClubRepository.deleteAll();
    }

    @Override
//    @Ignore("This scenario does not currently work")
    @Test
    public void updatingToANewValueShouldKeepTheEntityUniqueAndOldValueShouldBeReusableThereafter() {
        super.updatingToANewValueShouldKeepTheEntityUniqueAndOldValueShouldBeReusableThereafter();
    }

    @Override
    protected CommonUniqueClub lookupEntityByUniquePropertyValue(String propertyName, Object value) {
        return (CommonUniqueClub)getUniqueClubRepository().findBySchemaPropertyValue(propertyName, value);
    }

    @Test
    public void creatingDistinctUniqueEntitiesViaNeo4jTemplateShouldResolveToDifferentEntities() {
        Collection labels = Arrays.asList( UniqueClub.class.getSimpleName(),"_"+ UniqueClub.class.getSimpleName() );

        Map<String, Object> fooParams =  new HashMap<String,Object>();
        fooParams.put("name","foo");
        fooParams.put("description","foo description");
        Map<String, Object> barParams = new HashMap<String,Object>();
        barParams.put("name","bar");
        barParams.put("description","foo description");

        Node club1 = neo4jTemplate.merge(UniqueClub.class.getSimpleName(), "name", "foo", fooParams, labels);
        Node club2 = neo4jTemplate.merge(UniqueClub.class.getSimpleName(),"name","bar", barParams, labels);

        assertNotEquals("Expected different node Ids", club1.getId(), club2.getId());
        assertEquals(2, getUniqueClubRepository().count());
    }

    @Test
    public void creatingTheSameUniqueEntitiesViaNeo4jTemplateShouldResolveToOriginalEntity() {
        Collection labels = Arrays.asList( UniqueClub.class.getSimpleName(),"_"+ UniqueClub.class.getSimpleName() );

        Map<String, Object> fooParams = new HashMap<String,Object>();
        fooParams.put("name","foo");
        fooParams.put("description","foo description");
        Map<String, Object> foo2Params = new HashMap<String,Object>();
        foo2Params.put("name","foo");
        foo2Params.put("description","bar description"); // Note: description differs but will be discarded

        Node club1 = neo4jTemplate.getOrCreateNode(UniqueClub.class.getSimpleName(), "name", "foo", fooParams, labels);
        Node club2 = neo4jTemplate.getOrCreateNode(UniqueClub.class.getSimpleName(),"name","foo", foo2Params, labels);

        assertEquals("Expected the same node Ids", club1.getId(), club2.getId());
        assertEquals(1, getUniqueClubRepository().count());
        assertEquals("foo description", club2.getProperty("description"));
    }


    @Override
    protected CommonClub createNonUniqueClub(String name) {
        Club club = new Club();
        club.setName(name);
        clubRepository.save(club);
        return club;
    }

    @Override
    protected CommonUniqueClub createUniqueClub(String name, String description) {
        UniqueClub club = new UniqueClub();
        club.setName(name);
        club.setDescription(description);
        uniqueClubRepository.save(club);
        return club;    }

    @Override
    protected CommonUniqueNumericIdClub createUniqueNumericClub(Long clubId) {
        UniqueNumericIdClub club = new UniqueNumericIdClub();
        club.setClubId(clubId);
        uniqueNumericIdClubRepository.save(club);
        return club;
    }

    @Override
    protected GraphRepository getUniqueNumericIdClubRepository() {
        return uniqueNumericIdClubRepository;
    }

    @Override
    protected GraphRepository getUniqueClubRepository() {
        return uniqueClubRepository;
    }

    @Override
    protected GraphRepository getClubRepository() {
        return clubRepository;
    }
}
