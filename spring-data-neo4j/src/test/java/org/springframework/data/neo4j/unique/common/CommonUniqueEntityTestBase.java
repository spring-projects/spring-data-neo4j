/**
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.neo4j.unique.common;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.unique.schemabased.domain.Club;
import org.springframework.data.neo4j.unique.schemabased.domain.UniqueClub;
import org.springframework.data.neo4j.unique.schemabased.domain.UniqueNumericIdClub;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public abstract class CommonUniqueEntityTestBase {

    @Before
    public void setup() {
        clearDownAllRepositories();
    }

    @Test
    public void shouldOnlyCreateSingleInstanceForUniqueNodeEntity() {
        CommonUniqueClub club1 = createUniqueClub("foo", null);
        CommonUniqueClub club2 = createUniqueClub("foo", null);
        CommonUniqueClub club3 = createUniqueClub("foo", null);

        assertEquals(1, getUniqueClubRepository().count());
        assertEquals("Expected same node Ids", club1.getId(),club2.getId());
    }

    @Test
    public void shouldMergeNewUniqueNodeEntityDataWithExistingDataWhenSaving() {
        CommonUniqueClub club1 = createUniqueClub("bar", "description-1");
        CommonUniqueClub club2 = createUniqueClub("bar", "description-2");
        assertEquals(1, getUniqueClubRepository().count());
        assertEquals("Expected same node Ids", club1.getId(),club2.getId());
        CommonUniqueClub retrievedClub = (CommonUniqueClub)getUniqueClubRepository().findOne(club1.getId());
        assertEquals("Description not merged as expected",
                "description-2", retrievedClub.getDescription());
    }

    @Test(expected = MappingException.class)
    public void shouldFailOnNullPropertyValue() {
        createUniqueClub(null, null);
    }

    @Test(expected = MappingException.class)
    public void shouldFailOnNullNumericPropertyValue() {
        createUniqueNumericClub(null);
    }

    @Test
    public void shouldOnlyCreateSingleInstanceForUniqueNumericNodeEntity() {
        CommonUniqueNumericIdClub club1 = createUniqueNumericClub(100L);
        CommonUniqueNumericIdClub club2 = createUniqueNumericClub(100L);
        assertEquals(1, getUniqueNumericIdClubRepository().count());
        assertEquals("Expected same node Ids", club1.getId(),club2.getId());
    }

    @Test
    public void shouldCreateMultipleInstancesForNonUniqueNodeEntity() {
        CommonClub club1 = createNonUniqueClub("foo");
        CommonClub club2 = createNonUniqueClub("foo");
        assertEquals(2, getClubRepository().count());
        assertNotEquals("Expected different node Ids", club1.getId(), club2.getId());
    }

    @Test
    public void deletingUniqueNodeShouldRemoveItFromTheUniqueIndex() {
        CommonUniqueClub club1 = createUniqueClub("foo", null);
        assertEquals("Expected one unique entity",1, getUniqueClubRepository().count());
        getUniqueClubRepository().delete(club1);
        assertEquals("Expected zero unique entities",0, getUniqueClubRepository().count());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void updatingToADuplicateValueShouldCauseAnException() {
        CommonUniqueClub club1 = createUniqueClub("foo", "foo description");
        CommonUniqueClub club2 = createUniqueClub("bar", "bar description");
        assertEquals(2, getUniqueClubRepository().count());
        assertNotEquals("Expected different node Ids", club1.getId(), club2.getId());
        club2.setName("foo");
        getUniqueClubRepository().save(club2);
    }

    @Test
    public void updatingToANewValueShouldAlsoUpdateTheIndex() {
        CommonUniqueClub club1 = createUniqueClub("foo", "foo description");
        assertEquals(1, getUniqueClubRepository().count());
        CommonUniqueClub fooClub = lookupEntityByUniquePropertyValue("name", "foo");
        assertNotNull(fooClub);
        assertEquals(club1.getId(),fooClub.getId());

        club1.setName("bar");
        getUniqueClubRepository().save(club1);
        assertEquals(1, getUniqueClubRepository().count());
        CommonUniqueClub currentClub = lookupEntityByUniquePropertyValue("name", "bar");
        assertNotNull(currentClub);
        assertEquals(club1.getId(),currentClub.getId());

        // We should not find "foo" now
        CommonUniqueClub redundantClub = lookupEntityByUniquePropertyValue("name", "foo");
        assertNull(redundantClub);

    }

    @Test
    public void updatingToANewValueShouldKeepTheEntityUnique() {

        CommonUniqueClub club1 = createUniqueClub("foo", "foo description");
        assertEquals(1, getUniqueClubRepository().count());
        CommonUniqueClub fooClub = lookupEntityByUniquePropertyValue("name", "foo");
        assertNotNull(fooClub);
        assertEquals(club1.getId(),fooClub.getId());

        club1.setName("bar");
        getUniqueClubRepository().save(club1);
        assertEquals(1, getUniqueClubRepository().count());
        CommonUniqueClub currentClub = lookupEntityByUniquePropertyValue("name", "bar");
        assertNotNull(currentClub);
        assertEquals(club1.getId(),currentClub.getId());

        // We should not find "foo" now
        CommonUniqueClub redundantClub = lookupEntityByUniquePropertyValue("name", "foo");
        assertNull(redundantClub);
    }

    @Ignore("This scenario does not work, could be transactional issues")
    @Test
    public void updatingToANewValueShouldKeepTheEntityUniqueAndOldValueShouldBeReusableThereafter() {

        updatingToANewValueShouldAlsoUpdateTheIndex();

        // At this stage we should find "bar" but not "foo"
        CommonUniqueClub currentClub = (CommonUniqueClub)getUniqueClubRepository().findBySchemaPropertyValue("name", "bar");
        assertNotNull(currentClub);
        CommonUniqueClub redundantClub = (CommonUniqueClub)getUniqueClubRepository().findBySchemaPropertyValue("name", "foo");
        assertNull(redundantClub);

        CommonUniqueClub fooReusingClub = createUniqueClub("foo", "foo description");
        assertNotEquals("A new id should have been created for re-use of foo but it was not!",currentClub.getId(),fooReusingClub.getId());
        assertEquals(2, getUniqueClubRepository().count());

    }

    protected abstract CommonUniqueClub lookupEntityByUniquePropertyValue(String propertyName, Object value);

    protected abstract void clearDownAllRepositories();

    protected abstract CommonUniqueClub createUniqueClub(String name, String description);

    protected abstract CommonUniqueNumericIdClub createUniqueNumericClub(Long clubId);

    protected abstract CommonClub createNonUniqueClub(String name);

    protected abstract GraphRepository getUniqueNumericIdClubRepository();

    protected abstract GraphRepository getUniqueClubRepository();

    protected abstract GraphRepository getClubRepository();


}
