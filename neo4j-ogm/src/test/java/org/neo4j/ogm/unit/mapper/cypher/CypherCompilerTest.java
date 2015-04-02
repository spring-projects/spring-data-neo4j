/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.unit.mapper.cypher;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.cypher.statement.ParameterisedStatements;
import org.neo4j.ogm.domain.education.Course;
import org.neo4j.ogm.domain.education.School;
import org.neo4j.ogm.domain.education.Student;
import org.neo4j.ogm.domain.education.Teacher;
import org.neo4j.ogm.domain.forum.Forum;
import org.neo4j.ogm.domain.forum.ForumTopicLink;
import org.neo4j.ogm.domain.forum.Topic;
import org.neo4j.ogm.domain.music.Album;
import org.neo4j.ogm.domain.music.Artist;
import org.neo4j.ogm.mapper.EntityGraphMapper;
import org.neo4j.ogm.mapper.EntityToGraphMapper;
import org.neo4j.ogm.mapper.MappedRelationship;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.metadata.MetaData;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class CypherCompilerTest {

    private EntityToGraphMapper mapper;
    private static MetaData mappingMetadata;
    private static MappingContext mappingContext;

    @BeforeClass
    public static void setUpTestDatabase() {
        mappingMetadata = new MetaData("org.neo4j.ogm.domain.education", "org.neo4j.ogm.domain.forum", "org.neo4j.ogm.domain.social", "org.neo4j.domain.policy","org.neo4j.ogm.domain.music");
        mappingContext = new MappingContext(mappingMetadata);
    }

    @Before
    public void setUpMapper() {
        this.mapper = new EntityGraphMapper(mappingMetadata, mappingContext);
    }

    @After
    public void cleanGraph() {
        mappingContext.clear();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionOnAttemptToMapNullObjectToCypherQuery() {
        this.mapper.map(null);
    }

    @Test
    public void createSingleObjectWithLabelsAndProperties() {

        Student newStudent = new Student("Gary");
        ParameterisedStatements cypher = new ParameterisedStatements(this.mapper.map(newStudent).getStatements());

        assertNull(newStudent.getId());

        expectOnSave(newStudent,
                "CREATE (_0:`Student`:`DomainObject`{_0_props}) " +
                        "RETURN id(_0) AS _0");
    }

    @Test
    public void updateSingleObjectPropertyAndLabel() {

        Student sheila = new Student("Sheila Smythe");
        Long sid = 0L;
        sheila.setId(sid);
        mappingContext.remember(sheila);

        String sheilaNode = var(sid);

        // now update the object's properties locally
        sheila.setName("Sheila Smythe-Jones");

        expectOnSave(sheila, "MATCH (" + sheilaNode + ") " +
                "WHERE id(" + sheilaNode + ")=" + sid + " " +
                "SET " + sheilaNode + ":`Student`:`DomainObject`, " + sheilaNode + "+={" + sheilaNode + "_props}");
    }

    @Test
    public void doNothingIfNothingHasChanged() {

        Long existingNodeId = 0L;
        Student sheila = new Student();
        sheila.setId(existingNodeId);
        sheila.setName("Sheila Smythe");
        mappingContext.remember(sheila);

        expectOnSave(sheila, "");
    }

    @Test
    public void createSimpleRelationshipsBetweenObjects() {

        School waller = new School("Waller");
        Teacher mary = new Teacher("Mary");

        mary.setSchool(waller);
        waller.getTeachers().add(mary);

        String cypher=
                "CREATE (_0:`School`:`DomainObject`{_0_props}), (_2:`Teacher`{_2_props}) " +
                "WITH _0,_2 MERGE (_0)-[_1:`TEACHERS`]->(_2) " +
                "WITH _0,_1,_2 MERGE (_2)-[_3:`SCHOOL`]->(_0) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_2) AS _2, id(_3) AS _3";
        // we expect 2 outgoing relationships, as there are no directions
        expectOnSave(waller, cypher);

    }

    @Test
    public void expectNoChangesWhenDomainUnchanged() {

        // create
        Long wallerId = 0L;
        Long maryId = 1L;

        School waller = new School("Waller");
        waller.setId(wallerId);

        Teacher mary = new Teacher("Mary");
        mary.setId(maryId);

        // relate
        mary.setSchool(waller);

        // validate the domain model
        assertTrue(mary.getSchool().equals(waller));
        assertTrue(waller.getTeachers().contains(mary));
        assertTrue(waller.getTeachers().size() == 1);

        // set the mapping context accordingly
        mappingContext.remember(mary);
        mappingContext.remember(waller);
        mappingContext.registerRelationship(new MappedRelationship(maryId, "SCHOOL", wallerId));
        mappingContext.registerRelationship(new MappedRelationship(wallerId, "TEACHERS", maryId));

        expectOnSave(waller, "");
        expectOnSave(mary, "");

    }


    @Test
    public void addObjectToExistingCollection() {

        // create
        Long wallerId = 0L;
        Long maryId = 1L;

        School waller = new School("Waller");
        waller.setId(wallerId);

        Teacher mary = new Teacher("Mary");
        mary.setId(maryId);

        // relate
        mary.setSchool(waller);

        // validate the domain model
        assertTrue(mary.getSchool().equals(waller));
        assertTrue(waller.getTeachers().contains(mary));
        assertTrue(waller.getTeachers().size() == 1);

        // set the mapping context accordingly
        mappingContext.remember(mary);
        mappingContext.remember(waller);
        mappingContext.registerRelationship(new MappedRelationship(maryId, "SCHOOL", wallerId));
        mappingContext.registerRelationship(new MappedRelationship(wallerId, "TEACHERS", maryId));

        Teacher jim = new Teacher("Jim");
        jim.setSchool(waller);

        // we expect 1 new node and 2 new outgoing relationships: jim-[:SCHOOL]->school and school-[:TEACHERS]->jim

        // each of these will result in slightly different clause ordering. Ideally we want to check facts, not syntax
        // but in the interim, the syntax for each is at least idempotent

        expectOnSave(jim,
                "CREATE (_0:`Teacher`{_0_props}) " +
                "WITH _0 MATCH ($0) WHERE id($0)=0 MERGE (_0)-[_1:`SCHOOL`]->($0) " +
                "WITH $0,_0,_1 MERGE ($0)-[_2:`TEACHERS`]->(_0) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_2) AS _2",
                // or
                "CREATE (_0:`Teacher`{_0_props}) " +
                "WITH _0 MATCH ($0) WHERE id($0)=0 MERGE (_0)-[_1:`SCHOOL`]->($0) " +
                "WITH $0,_0,_1 MERGE ($0)-[_4:`TEACHERS`]->(_0) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_4) AS _4");

        expectOnSave(waller,
                "CREATE (_1:`Teacher`{_1_props}) " +
                "WITH _1 MATCH ($0) WHERE id($0)=0 MERGE ($0)-[_0:`TEACHERS`]->(_1) " +
                "WITH $0,_0,_1 MERGE (_1)-[_2:`SCHOOL`]->($0) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_2) AS _2",
                // or
                "CREATE (_3:`Teacher`{_3_props}) " +
                "WITH _3 MATCH ($0) WHERE id($0)=0 MERGE ($0)-[_2:`TEACHERS`]->(_3) " +
                "WITH $0,_2,_3 MERGE (_3)-[_4:`SCHOOL`]->($0) " +
                "RETURN id(_2) AS _2, id(_3) AS _3, id(_4) AS _4");

        expectOnSave(mary,
                "CREATE (_2:`Teacher`{_2_props}) " +
                "WITH _2 MATCH ($0) WHERE id($0)=0 MERGE ($0)-[_1:`TEACHERS`]->(_2) " +
                "WITH $0,_1,_2 MERGE (_2)-[_3:`SCHOOL`]->($0) " +
                "RETURN id(_1) AS _1, id(_2) AS _2, id(_3) AS _3",
                // or
                "CREATE (_3:`Teacher`{_3_props}) " +
                "WITH _3 MATCH ($0) WHERE id($0)=0 MERGE ($0)-[_2:`TEACHERS`]->(_3) " +
                "WITH $0,_2,_3 MERGE (_3)-[_4:`SCHOOL`]->($0) " +
                "RETURN id(_2) AS _2, id(_3) AS _3, id(_4) AS _4");

    }


    @Test
    public void shouldCorrectlyPersistObjectGraphsSeveralLevelsDeep() {

        Student sheila = new Student();
        sheila.setName("Sheila Smythe");
        Student gary = new Student();
        gary.setName("Gary Jones");
        Student winston = new Student();
        winston.setName("Winston Charles");

        Course physics = new Course();
        physics.setName("GCSE Physics");
        physics.setStudents(Arrays.asList(gary, sheila));
        Course maths = new Course();
        maths.setName("A-Level Mathematics");
        maths.setStudents(Arrays.asList(sheila, winston));

        Teacher teacher = new Teacher();
        teacher.setName("Mrs Kapoor");
        teacher.setCourses(Arrays.asList(physics, maths));

        // todo: too many with clauses for merge statements
        // todo: we can build larger merge paths from single-hop merge fragments (but check behaviour of partial paths?)
        String cypher=
                "CREATE (_0:`Teacher`{_0_props}), " +
                        "(_11:`Student`:`DomainObject`{_11_props}), " +
                        "(_2:`Course`{_2_props}), (_4:`Student`:`DomainObject`{_4_props}), " +
                        "(_6:`Student`:`DomainObject`{_6_props}), (_8:`Course`{_8_props}) " +
                "WITH _0,_11,_2,_4,_6,_8 MERGE (_0)-[_1:`COURSES`]->(_2) " +
                "WITH _0,_1,_11,_2,_4,_6,_8 MERGE (_8)-[_10:`STUDENTS`]->(_11) " +
                "WITH _0,_1,_10,_11,_2,_4,_6,_8 MERGE (_2)-[_3:`STUDENTS`]->(_4) " +
                "WITH _0,_1,_10,_11,_2,_3,_4,_6,_8 MERGE (_2)-[_5:`STUDENTS`]->(_6) " +
                "WITH _0,_1,_10,_11,_2,_3,_4,_5,_6,_8 MERGE (_0)-[_7:`COURSES`]->(_8) " +
                "WITH _0,_1,_10,_11,_2,_3,_4,_5,_6,_7,_8 MERGE (_8)-[_9:`STUDENTS`]->(_6) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_10) AS _10, id(_11) AS _11, id(_2) AS _2, id(_3) AS _3, id(_4) AS _4, id(_5) AS _5, id(_6) AS _6, id(_7) AS _7, id(_8) AS _8, id(_9) AS _9";

        expectOnSave(teacher, cypher);

    }

    @Test
    public void shouldCorrectlyRemoveRelationshipWhenItemIsRemovedFromCollection() {

        // simple music course with three students

        Long mid = (Long) 0L;

        Long xid = (Long) 1L;
        Long yid = (Long) 2L;
        Long zid = (Long) 3L;

        Course music = new Course("GCSE Music");
        music.setId(mid);

        Student xavier = new Student("xavier");
        xavier.setId(xid);

        Student yvonne = new Student("Yvonne");
        yvonne.setId(yid);

        Student zack = new Student("Zack");
        zack.setId(zid);

        music.setStudents(Arrays.asList(yvonne, xavier, zack));

        mappingContext.registerRelationship(new MappedRelationship(mid, "STUDENTS", xid));
        mappingContext.registerRelationship(new MappedRelationship(mid, "STUDENTS", yid));
        mappingContext.registerRelationship(new MappedRelationship(mid, "STUDENTS", zid));

        mappingContext.remember(xavier);
        mappingContext.remember(yvonne);
        mappingContext.remember(zack);
        mappingContext.remember(music);

        // now, update the domain model, setting yvonne as the only music student (i.e remove zack and xavier)
        music.setStudents(Arrays.asList(yvonne));

        // expect(for now) two separate delete clauses
        String cypher=
                "MATCH ($0)-[_2:STUDENTS]->($1) WHERE id($0)=0 AND id($1)=1 " +
                "DELETE _2 " +
                "WITH $0,$1 MATCH ($0)-[_1:STUDENTS]->($3) WHERE id($3)=3 " +
                "DELETE _1";

        expectOnSave(music, cypher);
    }

    @Test
    public void shouldCorrectlyRemoveRelationshipWhenItemIsMovedToDifferentCollection() {

        Long teacherId = 0L;
        Long businessStudiesCourseId = 1L;
        Long designTechnologyCourseId = 2L;
        Long shivaniId = 3L;

        Course designTech = new Course("GCSE Design & Technology");
        designTech.setId(designTechnologyCourseId);

        Course businessStudies = new Course("GNVQ Business Studies");
        businessStudies.setId(businessStudiesCourseId);

        Teacher msThompson = new Teacher();
        msThompson.setId(teacherId);
        msThompson.setName("Ms Thompson");
        msThompson.setCourses(Arrays.asList(businessStudies, designTech));

        Student shivani = new Student("Shivani");
        shivani.setId(shivaniId);

        mappingContext.remember(msThompson);
        mappingContext.remember(businessStudies);
        mappingContext.remember(designTech);
        mappingContext.remember(shivani);

        mappingContext.registerRelationship(new MappedRelationship(teacherId, "COURSES", businessStudiesCourseId));
        mappingContext.registerRelationship(new MappedRelationship(teacherId, "COURSES", designTechnologyCourseId));
        mappingContext.registerRelationship(new MappedRelationship(businessStudiesCourseId, "STUDENTS", shivaniId));

        // move shivani from one course to the other
        businessStudies.setStudents(Collections.<Student>emptyList());
        designTech.setStudents(Arrays.asList(shivani));

        // we expect a new relationship to be created, and an old one deleted
        expectOnSave(msThompson,
                "MATCH ($2) WHERE id($2)=2 MATCH ($3) WHERE id($3)=3 MERGE ($2)-[_2:`STUDENTS`]->($3) " +
                "WITH $2,$3,_2 MATCH ($1)-[_3:STUDENTS]->($3) WHERE id($1)=1 DELETE _3 " +
                "RETURN id(_2) AS _2");

        // fixme: these other tests now need to be in their own test method, because
        // a bug fix to the deletion code means that a second deletion won't (correctly) fire again
        // expect a delete, but don't expect the new relationship to be created, because the fact of it
        // is inaccessible from the businessStudies object
//        expectOnSave(businessStudies,
//                "MATCH ($1)-[_0:STUDENTS]->($3) WHERE id($1)=1 AND id($3)=3 DELETE _0");
//
//        // expect the new relationship, but don't expect the old one to be deleted, because the fact
//        // of it is inaccessible from the designTech object
//        expectOnSave(designTech,
//                "MATCH ($2) WHERE id($2)=2 MATCH ($3) WHERE id($3)=3 MERGE ($2)-[_0:`STUDENTS`]->($3) RETURN id(_0) AS _0");
//
//        // we can't explore the object model from shivani at all, so no changes.
//        expectOnSave(shivani, "");


    }

    @Test
    public void shouldCorrectlyRemoveRelationshipWhenItemIsDisconnectedFromNonOwningSide() {

        Long schoolId = 0L;
        Long whiteId = 1L;
        Long jonesId = 2L;

        School hillsRoad = new School("Hills Road Sixth Form College");
        hillsRoad.setId(schoolId);

        Teacher mrWhite = new Teacher("Mr White");
        mrWhite.setId(whiteId);

        Teacher missJones = new Teacher("Miss Jones");
        missJones.setId(jonesId);

        // need to ensure teachers list is mutable
        hillsRoad.setTeachers(new ArrayList<>(Arrays.asList(missJones, mrWhite)));


        mappingContext.remember(hillsRoad);
        mappingContext.remember(mrWhite);
        mappingContext.remember(missJones);

        mappingContext.registerRelationship(new MappedRelationship(schoolId, "TEACHERS", whiteId));
        mappingContext.registerRelationship(new MappedRelationship(schoolId, "TEACHERS", jonesId));
        mappingContext.registerRelationship(new MappedRelationship(whiteId, "SCHOOL", schoolId));
        mappingContext.registerRelationship(new MappedRelationship(jonesId, "SCHOOL", schoolId));


        // Fire Mr White:
        mrWhite.setSchool(null);

        // validate model:
        assertNull(mrWhite.getSchool());
        assertFalse(hillsRoad.getTeachers().contains(mrWhite));

        // we expect hillsRoad relationship to mrWhite to be removed.
        // however, the change to MrWhite's relationship is not detected.
        // this is because MrWhite is not "visited" during the traversal of
        // hillsRoad - his reference is now inaccessible. this looks like a FIXME
        expectOnSave(hillsRoad,
                "MATCH ($0)-[_2:TEACHERS]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _2");

        // we expect mrWhite's relationship to hillsRoad to be removed
        // but the change to hillsRoad's relationship with MrWhite is not detected
        // this is because hillsRoad object is no longer directly accessible from MrWhite
        // looks like a FIXME (infer symmetric deletions)
        expectOnSave(mrWhite,
                "MATCH ($1)-[_0:SCHOOL]->($0) WHERE id($1)=1 AND id($0)=0 DELETE _0");

        // because missJones has a reference to hillsRoad, we expect an outcome
        // the same as if we had saved hillsRoiad directly.
        //expectOnSave(missJones,
        //        "MATCH ($0)-[_2:TEACHERS]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _2");
    }


    @Test
    public void shouldCreateRelationshipWithPropertiesFromRelationshipEntity() {

        Forum forum = new Forum();
        forum.setName("SDN FAQs");

        Topic topic = new Topic();

        ForumTopicLink link = new ForumTopicLink();
        link.setForum(forum);
        link.setTopic(topic);
        link.setTimestamp(1647209L);

        forum.setTopicsInForum(Arrays.asList(link));

        // the entire object tree is accessible from the forum
        // Note that a relationshipEntity has a direction by default (srcNode -> tgtNode)
        // because it has an annotation, so we should not create an inverse relationship.
        expectOnSave(forum,
                "CREATE (_0:`Forum`{_0_props}), (_2:`Topic`) " +
                "WITH _0,_2 MERGE (_0)-[_1:`HAS_TOPIC`{timestamp:{_1_props}.timestamp}]->(_2) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_2) AS _2");

        // the entire object tree is accessible from the link
        expectOnSave(link,
                "CREATE (_0:`Forum`{_0_props}), (_2:`Topic`) " +
                "WITH _0,_2 MERGE (_0)-[_1:`HAS_TOPIC`{timestamp:{_1_props}.timestamp}]->(_2) " +
                "RETURN id(_0) AS _0, id(_1) AS _1, id(_2) AS _2");


        // the related entity is not visible from the Topic object.
        expectOnSave(topic, "CREATE (_0:`Topic`) RETURN id(_0) AS _0");
    }

    @Test
    public void shouldUpdatingExistingRelationshipEntity() {

        Long forumId = 0L;
        Long topicId = 1L;
        Long relationshipId = 2L;

        Forum forum = new Forum();
        forum.setId(forumId);
        forum.setName("Spring Data Neo4j");

        Topic topic = new Topic();
        topic.setTopicId(topicId);
        topic.setInActive(Boolean.FALSE);

        ForumTopicLink link = new ForumTopicLink();
        link.setId(relationshipId);
        link.setForum(forum);
        link.setTopic(topic);

        forum.setTopicsInForum(Arrays.asList(link));

        mappingContext.remember(forum);
        mappingContext.remember(topic);
        mappingContext.remember(link);
        mappingContext.registerRelationship(new MappedRelationship(forumId, "HAS_TOPIC", topicId));

        // change the timestamp
        link.setTimestamp(327790L);

        // expect the property on the relationship entity to be updated on the graph relationship
        expectOnSave(forum, "MATCH ()-[_0]->() WHERE id(_0)=2 SET _0+={_0_props}");

    }



    @Test
    public void shouldDeleteExistingRelationshipEntity() {

        Long forumId = 0L;
        Long topicId = 1L;
        Long linkId  = 2L;

        Forum forum = new Forum();
        forum.setId(forumId);
        forum.setName("Spring Data Neo4j");

        Topic topic = new Topic();
        topic.setTopicId(topicId);
        topic.setInActive(Boolean.FALSE);

        ForumTopicLink link = new ForumTopicLink();
        link.setId(linkId);
        link.setForum(forum);
        link.setTopic(topic);

        forum.setTopicsInForum(Arrays.asList(link));

        mappingContext.remember(forum);
        mappingContext.remember(topic);
        mappingContext.remember(link);
        // the mapping context remembers the relationship between the forum and the topic in the graph
        mappingContext.registerRelationship(new MappedRelationship(forumId, "HAS_TOPIC", topicId));

        // unlink the objects manually
        forum.setTopicsInForum(null);
        link.setTopic(null);

        // expect the delete to be recognised when the forum is saved
        expectOnSave(forum, "MATCH ($0)-[_0:HAS_TOPIC]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _0");

        // expect the delete to be recognised if the RE is saved
//        expectOnSave(link, "MATCH ($0)-[_0:HAS_TOPIC]->($1) WHERE id($0)=0 AND id($1)=1 DELETE _0");
//
//        // expect nothing to happen if the topic is saved, because the domain model does not
//        // permit navigation from the topic to the RE (topic has no reference to it)
//        expectOnSave(topic, "");

        // todo: more tests re saving deletes from REs marked as incoming relationships

    }

    /**
     * @see DATAGRAPH-589
     */
    @Test
    public void createSimpleRelationshipWithIllegalCharactersBetweenObjects() {

        Artist theBeatles = new Artist("The Beatles");
        Album please = new Album("Please Please Me");
        theBeatles.getAlbums().add(please);
        please.setArtist(theBeatles);

        String cypher =
                "CREATE (_0:`l'artiste`{_0_props}), (_2:`l'album`{_2_props}) " +
                        "WITH _0,_2 MERGE (_0)-[_3:`HAS-ALBUM`]->(_2) " +
                        "RETURN id(_0) AS _0, id(_2) AS _2, id(_3) AS _3";
        expectOnSave(theBeatles, cypher);
    }

    private void expectOnSave(Object object, String... cypher) {
        ParameterisedStatements statements = new ParameterisedStatements(this.mapper.map(object).getStatements());
        for (String s : cypher) {
            if (s.equals(statements.getStatements().get(0).getStatement())) {
                return;
            }
        }
        fail("unexpected: '" + statements.getStatements().get(0).getStatement() + "'");
    }

    private String var(Long nodeId) {
        return "$" + nodeId;
    }
}
