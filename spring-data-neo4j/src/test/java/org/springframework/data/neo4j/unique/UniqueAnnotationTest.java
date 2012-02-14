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
package org.springframework.data.neo4j.unique;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:unique-test-context.xml"})
@Transactional
public class UniqueAnnotationTest {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UniqueClubRepository uniqueClubRepository;

    @Autowired
    private InvalidClubRepository invalidClubRepository;

    @Autowired
    protected GraphDatabaseService graphDatabaseService;

    @Before
    public void setup() {
        clubRepository.deleteAll();
        uniqueClubRepository.deleteAll();
    }

    @Test
    public void shouldOnlyCreateSingleInstanceForUniqueNodeEntity() {
        UniqueClub club = new UniqueClub();
        club.setName("foo");
        uniqueClubRepository.save(club);

        club = new UniqueClub();
        club.setName("foo");
        uniqueClubRepository.save(club);

        assertEquals(1, uniqueClubRepository.count());
    }

    @Test
    public void shouldCreateMultipleInstancesForNonUniqueNodeEntity() {
        Club club = new Club();
        club.setName("foo");
        clubRepository.save(club);

        club = new Club();
        club.setName("foo");
        clubRepository.save(club);

        assertEquals(2, clubRepository.count());
    }

    @Test
    public void deletingUniqueNodeShouldRemoveItFromTheUniqueIndex() {
        UniqueClub club = new UniqueClub();
        club.setName("foo");
        uniqueClubRepository.save(club);

        uniqueClubRepository.delete(club);
        Index<Node> nodeIndex = graphDatabaseService.index().forNodes(String.format("%s.%s", UniqueClub.class.getSimpleName(), "name"));
        IndexHits<Node> nodeIndexHits = nodeIndex.get("name", "foo");
        assertEquals(0, nodeIndexHits.size());
    }

    @Test(expected = MappingException.class)
    public void shouldThrowExceptionWhenMultipleUniqueSpecified() {
        InvalidClub club = new InvalidClub();
        invalidClubRepository.save(club);
    }
}
