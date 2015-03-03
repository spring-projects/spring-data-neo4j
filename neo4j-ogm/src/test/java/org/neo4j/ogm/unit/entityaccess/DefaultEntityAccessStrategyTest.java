/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.unit.entityaccess;

import org.junit.Test;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.domain.forum.ForumTopicLink;
import org.neo4j.ogm.domain.forum.Member;
import org.neo4j.ogm.domain.forum.Topic;
import org.neo4j.ogm.domain.forum.activity.Activity;
import org.neo4j.ogm.domain.forum.activity.Comment;
import org.neo4j.ogm.domain.forum.activity.Post;
import org.neo4j.ogm.domain.satellites.Location;
import org.neo4j.ogm.domain.satellites.Program;
import org.neo4j.ogm.domain.satellites.Satellite;
import org.neo4j.ogm.entityaccess.*;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;

import java.util.*;

import static org.junit.Assert.*;

public class DefaultEntityAccessStrategyTest {

    private DefaultEntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();
    private DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.unit.entityaccess",
            "org.neo4j.ogm.domain.forum", "org.neo4j.ogm.domain.satellites");

    @Test
    public void shouldPreferAnnotatedMethodToAnnotatedFieldWhenFindingPropertyToSet() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getPropertyWriter(classInfo, "testAnnoProp");
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);

        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, "Arbitrary Value");
        assertEquals("Arbitrary Value", domainObject.fullyAnnotatedProperty);
        assertTrue("The accessor method wasn't used to set the value", domainObject.fullyAnnotatedPropertyAccessorWasCalled);
    }

    @Test
    public void shouldPreferAnnotatedFieldToPlainMethodWhenFindingPropertyToSet() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        // testProp matches the setter/getter name but because the field is annotated then it should be used instead
        EntityAccess objectAccess = this.entityAccessStrategy.getPropertyWriter(classInfo, "testProp");
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);

        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, "TEST");
        assertEquals("TEST", domainObject.annotatedTestProperty);
    }

    @Test
    public void shouldReturnAccessorMethodInPreferenceToFieldIfNoAnnotationsArePresent() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getPropertyWriter(classInfo, "nonAnnotatedTestProperty");
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);

        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, 8.14);
        assertEquals(8.14, domainObject.nonAnnotatedTestProperty, 0.0);
        assertTrue("The setter method wasn't called to write the value", domainObject.nonAnnotatedTestPropertyAccessorWasCalled);
    }

    @Test
    public void shouldAccessViaFieldCorrespondingToPropertyIfNoAnnotationsOrAccessorMethodsArePresent() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.propertyWithoutAccessorMethods = 9;

        // test writing via field
        EntityAccess writer = this.entityAccessStrategy.getPropertyWriter(classInfo, "propertyWithoutAccessorMethods");
        assertNotNull("The resultant writer shouldn't be null", writer);
        writer.write(domainObject, 27);
        assertEquals(27, domainObject.propertyWithoutAccessorMethods);

        // test reading via field
        PropertyReader reader = this.entityAccessStrategy.getPropertyReader(classInfo, "propertyWithoutAccessorMethods");
        assertNotNull("The resultant reader shouldn't be null", reader);
        assertEquals(domainObject.propertyWithoutAccessorMethods, reader.read(domainObject));
   }

    @Test
    public void shouldRetrieveObjectAccessForWritingIterableObject() {
        ClassInfo classInfo = this.domainInfo.getClass(Program.class.getName());

        // TODO: this supports the behaviour required currently, but what happens if there's more than one collection of X?
        EntityAccess iterableAccess = this.entityAccessStrategy.getIterableWriter(classInfo, Satellite.class);
        assertNotNull("The resultant object accessor shouldn't be null", iterableAccess);
        Program spaceProgramme = new Program();
        iterableAccess.write(spaceProgramme, Arrays.asList(new Satellite()));
        assertNotNull("The satellites list wasn't set correctly", spaceProgramme.getSatellites());
        assertFalse("The satellites list wasn't set correctly", spaceProgramme.getSatellites().isEmpty());
    }

    @Test
    public void shouldNotRetrieveSetterMethodObjectAccessIfTypesAreIncompatible() {
        ClassInfo classInfo = this.domainInfo.getClass(Program.class.getName());

        Satellite singleSatellite = new Satellite();

        // the SATELLITES type matches the setter that takes an Iterable argument
        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "SATELLITES", singleSatellite);
        assertNull("A compatible object accessor shouldn't have been found", objectAccess);
    }

    @Test
    public void shouldPreferAnnotatedMethodToAnnotatedFieldWhenSettingRelationshipObject() {
        // 1st, try to find a method annotated with the relationship type.
        ClassInfo classInfo = this.domainInfo.getClass(Member.class.getName());
        List<? extends Activity> parameter = Arrays.asList(new Comment());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "HAS_ACTIVITY", parameter);
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the setter", objectAccess instanceof MethodWriter);
        Member member = new Member();
        objectAccess.write(member, parameter);
        assertEquals(member.getActivityList(), parameter);
    }

    @Test
    public void shouldPreferAnnotatedFieldToPlainSetterMatchingRelationshipTypeWhenSettingRelationshipObject() {
        // 2nd, try to find a field annotated with with relationship type
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        Member parameter = new Member();

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "CONTAINS", parameter);
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, parameter);
        assertEquals(domainObject.member, parameter);
    }

    @Test
    public void shouldPreferSetterBasedOnRelationshipTypeToFieldInObjectWithoutAnnotations() {
        // 3rd, try to find a "setXYZ" method where XYZ is derived from the relationship type
        ClassInfo classInfo = this.domainInfo.getClass(Satellite.class.getName());

        Location satelliteLocation = new Location();
        satelliteLocation.setName("Outer Space");

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "LOCATION", satelliteLocation);
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the setter", objectAccess instanceof MethodWriter);
        Satellite satellite = new Satellite();
        objectAccess.write(satellite, satelliteLocation);
        assertEquals(satellite.getLocation(), satelliteLocation);
    }

    @Test
    public void shouldPreferFieldBasedOnRelationshipTypeToPlainSetterWithMatchingParameterType() {
        // 4th, try to find a "XYZ" field name where XYZ is derived from the relationship type
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());
        Topic favouriteTopic = new Topic();

        // NB: the setter is called setTopic here, so a relationship type of just "TOPIC" would choose the setter
        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "FAVOURITE_TOPIC", favouriteTopic);
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, favouriteTopic);
        assertEquals(domainObject.favouriteTopic, favouriteTopic);
        assertFalse("The access should be via the field", domainObject.topicAccessorWasCalled);
    }

    @Test
    public void shouldDefaultToFindingSetterThatMatchesTheParameterTypeIfRelationshipTypeCannotBeMatched() {
        // 5th, try to find a single setter that takes the parameter
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());
        Topic favouriteTopic = new Topic();

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "DOES_NOT_MATCH", favouriteTopic);
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, favouriteTopic);
        assertEquals(domainObject.favouriteTopic, favouriteTopic);
        assertTrue("The access should be via the setter method", domainObject.topicAccessorWasCalled);
    }

    @Test
    public void shouldDefaultToFieldThatMatchesTheParameterTypeIfRelationshipTypeCannotBeMatchedAndNoSetterExists() {
        // 6th, try to find a field that shares the same type as the parameter
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());
        Post forumPost = new Post();

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "UTTER_RUBBISH", forumPost);
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        DummyDomainObject domainObject = new DummyDomainObject();
        objectAccess.write(domainObject, forumPost);
        assertEquals(domainObject.postWithoutAccessorMethods, forumPost);
    }

    @Test
    public void shouldPreferAnnotatedMethodToAnnotatedFieldWhenReadingFromAnObject() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.fullyAnnotatedProperty = "test text";

        PropertyReader objectAccess = this.entityAccessStrategy.getPropertyReader(classInfo, "testAnnoProp");
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertEquals(domainObject.fullyAnnotatedProperty, objectAccess.read(domainObject));
        assertTrue("The accessor method wasn't used to get the value", domainObject.fullyAnnotatedPropertyAccessorWasCalled);
    }

    @Test
    public void shouldPreferAnnotatedFieldToPlainGetterWhenReadingFromAnObject() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.annotatedTestProperty = "more arbitrary text";

        PropertyReader objectAccess = this.entityAccessStrategy.getPropertyReader(classInfo, "testProp");
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertEquals(domainObject.annotatedTestProperty, objectAccess.read(domainObject));
    }

    @Test
    public void shouldPreferMethodBasedAccessToFieldAccessWhenReadingFromObjectsWithoutAnnotations() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.nonAnnotatedTestProperty = new Double(30.16);

        PropertyReader objectAccess = this.entityAccessStrategy.getPropertyReader(classInfo, "nonAnnotatedTestProperty");
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertEquals(domainObject.nonAnnotatedTestProperty, objectAccess.read(domainObject));
    }

    @Test
    public void shouldPreferAnnotatedMethodToAnnotatedFieldMatchingRelationshipTypeWhenReadingRelationshipObject() {
        ClassInfo classInfo = this.domainInfo.getClass(Member.class.getName());
        Member member = new Member();
        member.setActivityList(Arrays.<Activity>asList(new Comment()));

        RelationalReader reader = this.entityAccessStrategy.getRelationalReader(classInfo, "HAS_ACTIVITY");
        assertNotNull("The resultant object reader shouldn't be null", reader);
        assertTrue("The access mechanism should be via the getter", reader instanceof MethodReader);
        assertSame(member.getActivityList(), reader.read(member));
        assertEquals("HAS_ACTIVITY", reader.relationshipType());
    }

    @Test
    public void shouldPreferAnnotatedFieldToPlainGetterMethodMatchingRelationshipType() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.member = new Member();

        RelationalReader reader = this.entityAccessStrategy.getRelationalReader(classInfo, "CONTAINS");
        assertNotNull("The resultant object reader shouldn't be null", reader);
        assertSame(domainObject.member, reader.read(domainObject));
        assertEquals("CONTAINS", reader.relationshipType());
    }

    @Test
    public void shouldPreferGetterBasedOnRelationshipTypeToFieldInObjectWithoutAnnotations() {
        ClassInfo classInfo = this.domainInfo.getClass(Satellite.class.getName());

        Satellite satellite = new Satellite();
        Location satelliteLocation = new Location();
        satelliteLocation.setName("Outer Space");
        satellite.setLocation(satelliteLocation);

        RelationalReader reader = this.entityAccessStrategy.getRelationalReader(classInfo, "LOCATION");
        assertNotNull("The resultant object accessor shouldn't be null", reader);
        assertTrue("The access mechanism should be via the getter", reader instanceof MethodReader);
        assertSame(satellite.getLocation(), reader.read(satellite));
        assertEquals("LOCATION", reader.relationshipType());
    }

    @Test
    public void shouldReadFromFieldMatchingRelationshipTypeInObjectWithoutAnnotationsOrAccessorMethods() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.postWithoutAccessorMethods = new Post();

        RelationalReader reader = this.entityAccessStrategy.getRelationalReader(classInfo, "POST_WITHOUT_ACCESSOR_METHODS");
        assertNotNull("The resultant object accessor shouldn't be null", reader);
        assertSame(domainObject.postWithoutAccessorMethods, reader.read(domainObject));
        assertEquals("POST_WITHOUT_ACCESSOR_METHODS", reader.relationshipType());
    }

    @Test
    public void shouldUseFieldAccessUnconditionallyForReadingIdentityProperty() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        final long id = 593L;
        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.setId(id);

        PropertyReader idReader = this.entityAccessStrategy.getIdentityPropertyReader(classInfo);
        assertNotNull("The resultant ID reader shouldn't be null", idReader);
        assertEquals(id, idReader.read(domainObject));
    }

    @Test
    public void shouldRetrieveAppropriateObjectAccessToAllRelationalAttributesForParticularClass() {
        ClassInfo classInfo = this.domainInfo.getClass(DummyDomainObject.class.getName());

        DummyDomainObject domainObject = new DummyDomainObject();
        domainObject.postWithoutAccessorMethods = new Post();
        domainObject.favouriteTopic = new Topic();
        domainObject.member = new Member();
        domainObject.readOnlyComment = new Comment();

        Collection<RelationalReader> relationalAccessors = this.entityAccessStrategy.getRelationalReaders(classInfo);
        assertNotNull("The resultant list of object accessors shouldn't be null", relationalAccessors);
        assertEquals("An unexpected number of accessors was returned", 4, relationalAccessors.size());

        Map<String, Class<? extends RelationalReader>> expectedRelationalReaders = new HashMap<>();
        expectedRelationalReaders.put("COMMENT", MethodReader.class);
        expectedRelationalReaders.put("FAVOURITE_TOPIC", FieldReader.class);
        expectedRelationalReaders.put("CONTAINS", FieldReader.class);
        expectedRelationalReaders.put("POST_WITHOUT_ACCESSOR_METHODS", FieldReader.class);

        for (RelationalReader objectAccess : relationalAccessors) {
            String relType = objectAccess.relationshipType();
            assertTrue("Relationship type " + relType + " wasn't expected", expectedRelationalReaders.containsKey(relType));
            assertEquals(expectedRelationalReaders.get(relType), objectAccess.getClass());
            assertNotNull(objectAccess.read(domainObject));
        }
    }

    @Test
    public void shouldRetrieveAppropriateObjectAccessToEndNodeAttributeOnRelationshipEntity() {
        ClassInfo relationshipEntityClassInfo = domainInfo.getClass(ForumTopicLink.class.getName());

        RelationalReader endNodeReader = this.entityAccessStrategy.getEndNodeReader(relationshipEntityClassInfo);
        assertNotNull("The resultant end node reader shouldn't be null", endNodeReader);

        ForumTopicLink forumTopicLink = new ForumTopicLink();
        Topic topic = new Topic();
        forumTopicLink.setTopic(topic);
        assertSame("The value wasn't read correctly", topic, endNodeReader.read(forumTopicLink));
    }

    @Test
    public void shouldReturnNullOnAttemptToAccessNonExistentEndNodeAttributeOnRelationshipEntity() {
        ClassInfo classInfoOfNonRelationshipEntity = domainInfo.getClass(Member.class.getName());
        assertNull(this.entityAccessStrategy.getEndNodeReader(classInfoOfNonRelationshipEntity));
    }

    /**
     * Domain object exhibiting various annotation configurations on its properties for test purposes.
     */
    public static class DummyDomainObject {

        // interestingly, if I extend DomainObject then the inherited ID field isn't found within a nested class
        @SuppressWarnings("unused")
        private Long id;

        @Property(name = "testProp")
        String annotatedTestProperty;

        Double nonAnnotatedTestProperty;
        boolean nonAnnotatedTestPropertyAccessorWasCalled;

        @Property(name = "testAnnoProp")
        String fullyAnnotatedProperty;
        boolean fullyAnnotatedPropertyAccessorWasCalled;

        int propertyWithoutAccessorMethods;

        @Relationship(type = "CONTAINS")
        Member member;

        Topic favouriteTopic;
        boolean topicAccessorWasCalled;

        Post postWithoutAccessorMethods;

        Comment readOnlyComment;

        public Long getId() {
            throw new UnsupportedOperationException("Shouldn't be calling the ID getter");
        }

        public void setId(Long id) {
            this.id = id;
        }

        public void setTestProp(String value) {
            throw new UnsupportedOperationException("Shouldn't be calling the setter with: " + value);
        }

        public String getTestProp() {
            throw new UnsupportedOperationException("Shouldn't be calling the getter");
        }

        public void setNonAnnotatedTestProperty(Double value) {
            this.nonAnnotatedTestPropertyAccessorWasCalled = true;
            this.nonAnnotatedTestProperty = value;
        }

        public Double getNonAnnotatedTestProperty() {
            this.nonAnnotatedTestPropertyAccessorWasCalled = true;
            return this.nonAnnotatedTestProperty;
        }

        @Property(name = "testAnnoProp")
        public String getFullyAnnotatedProperty() {
            this.fullyAnnotatedPropertyAccessorWasCalled = true;
            return fullyAnnotatedProperty;
        }

        @Property(name = "testAnnoProp")
        public void setFullyAnnotatedProperty(String fullyAnnotatedProperty) {
            this.fullyAnnotatedPropertyAccessorWasCalled = true;
            this.fullyAnnotatedProperty = fullyAnnotatedProperty;
        }

        public Member getContains() {
            throw new UnsupportedOperationException("Shouldn't be calling the getter");
        }

        public void setContains(Member nestedObject) {
            throw new UnsupportedOperationException("Shouldn't be calling the setter with: " + nestedObject);
        }

        public Topic getTopic() {
            this.topicAccessorWasCalled = true;
            return favouriteTopic;
        }

        public void setTopic(Topic favouriteTopic) {
            this.topicAccessorWasCalled = true;
            this.favouriteTopic = favouriteTopic;
        }

        @Relationship(type = "COMMENT")
        public Comment getReadOnlyComment() {
            return this.readOnlyComment;
        }

    }

}
