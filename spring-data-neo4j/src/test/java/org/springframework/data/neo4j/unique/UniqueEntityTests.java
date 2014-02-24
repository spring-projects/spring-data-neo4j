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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.unique.domain.Club;
import org.springframework.data.neo4j.invalid.unique.InvalidClub;
import org.springframework.data.neo4j.unique.domain.UniqueClub;
import org.springframework.data.neo4j.unique.domain.UniqueNumericIdClub;
import org.springframework.data.neo4j.unique.repository.ClubRepository;
import org.springframework.data.neo4j.invalid.unique.InvalidClubRepository;
import org.springframework.data.neo4j.unique.repository.UniqueClubRepository;
import org.springframework.data.neo4j.unique.repository.UniqueNumericIdClubRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:unique-test-context.xml"})
@Transactional
public class UniqueEntityTests {

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private UniqueClubRepository uniqueClubRepository;

    @Autowired
    protected GraphDatabaseService graphDatabaseService;

    @Autowired
    private UniqueNumericIdClubRepository uniqueNumericIdClubRepository;

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

    @Test(expected = MappingException.class)
    public void shouldFailOnNullPropertyValue() {
        UniqueClub club = new UniqueClub();
        club.setName(null);
        uniqueClubRepository.save(club);
    }

    @Test
    public void shouldOnlyCreateSingleInstanceForUniqueNumericNodeEntity() {
        UniqueNumericIdClub club = new UniqueNumericIdClub();
        club.setClubId(100L);
        uniqueNumericIdClubRepository.save(club);

        club = new UniqueNumericIdClub(100L);
        uniqueNumericIdClubRepository.save(club);

        assertEquals(1, uniqueNumericIdClubRepository.count());
    }

    @Test(expected = MappingException.class)
    public void shouldFailOnNullNumericPropertyValue() {
        UniqueNumericIdClub club = new UniqueNumericIdClub();
        club.setClubId(null);
        uniqueNumericIdClubRepository.save(club);
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
        assertEquals(1, uniqueClubRepository.count());

        uniqueClubRepository.delete(club);
        assertEquals(0, uniqueClubRepository.count());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void updatingToADuplicateValueShouldCauseAnException() {
        uniqueClubRepository.save(new UniqueClub("foo"));
        UniqueClub club2 = uniqueClubRepository.save(new UniqueClub("bar"));
        assertEquals(2, uniqueClubRepository.count());
        club2.setName("foo");
        uniqueClubRepository.save(club2);
    }

    @Test
    public void updatingToANewValueShouldKeepTheEntityUnique() {
        UniqueClub club = uniqueClubRepository.save(new UniqueClub("foo"));
        assertEquals(1, uniqueClubRepository.count());
        club.setName("bar");
        uniqueClubRepository.save(club);
        assertEquals(1, uniqueClubRepository.count());
        final UniqueClub club2 = uniqueClubRepository.save(new UniqueClub("bar"));
        assertEquals(club.getId(),club2.getId());
    }
    @Test
    public void updatingToANewValueShouldAlsoUpdateTheIndex() {
        UniqueClub club = uniqueClubRepository.save(new UniqueClub("foo"));
        assertEquals(1, uniqueClubRepository.count());
        assertEquals(club.getId(),uniqueClubRepository.findByPropertyValue("name","foo").getId());
        club.setName("bar");
        uniqueClubRepository.save(club);
        assertEquals(club.getId(),uniqueClubRepository.findByPropertyValue("name","bar").getId());
    }
}
