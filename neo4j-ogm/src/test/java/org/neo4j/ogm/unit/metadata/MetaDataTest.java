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

package org.neo4j.ogm.unit.metadata;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.education.Student;
import org.neo4j.ogm.domain.forum.Member;
import org.neo4j.ogm.domain.forum.activity.Activity;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vince Bickers
 */
public class MetaDataTest {

    private MetaData metaData;

    @Before
    public void setUp() {
        metaData = new MetaData("org.neo4j.ogm.domain.forum", "org.neo4j.ogm.domain.canonical","org.neo4j.ogm.integration.hierarchy.domain");
    }

    /**
     * A class can be found if its simple name is unique in the domain
     */
    @Test
    public void testClassInfo() {
        assertEquals("org.neo4j.ogm.domain.forum.Topic", metaData.classInfo("Topic").name());
    }

    /**
     * A class can be found via its annotated label
     */
    @Test
    public void testAnnotatedClassInfo() {
        assertEquals("org.neo4j.ogm.domain.forum.Member", metaData.classInfo("User").name());
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.classInfo("Bronze").name());
    }

    /**
     * The default identity field is a Long type called "id"
     */
    @Test
    public void testIdentity() {
        ClassInfo classInfo = metaData.classInfo("Login");
        assertEquals("id", classInfo.identityField().getName());
        classInfo = metaData.classInfo("Bronze");
        assertEquals("id", classInfo.identityField().getName());
    }

    /**
     * The annotated identity field is a Long type but called whatever you want
     */
    @Test
    public void testAnnotatedIdentity() {
        ClassInfo classInfo = metaData.classInfo("Topic");
        assertEquals("topicId", classInfo.identityField().getName());
    }


    /**
     * Fields mappable to node properties
     */
    @Test
    public void testPropertyFieldInfo() {

        ClassInfo classInfo = metaData.classInfo("Bronze");
        Collection<FieldInfo> fieldInfos = classInfo.propertyFields();

        int count = 1;
        assertEquals(count, fieldInfos.size());
        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo.getName().equals("fees")) count--;
        }
        assertEquals(0, count);
    }

    /**
     * Node property names available via .property() (annotation)
     */
    @Test
    public void testAnnotatedPropertyFieldInfo() {

        ClassInfo classInfo = metaData.classInfo("Bronze");
        Collection<FieldInfo> fieldInfos = classInfo.propertyFields();

        FieldInfo fieldInfo = fieldInfos.iterator().next();
        assertEquals("annualFees", fieldInfo.property()); // the node property name
        assertEquals("fees", fieldInfo.getName()); // the field name

    }

    /**
     * A property field cannot be used as a relationship (node entry)
     */
    @Test
    public void testPropertyFieldIsNotARelationshipField() {

        ClassInfo classInfo = metaData.classInfo("Bronze");
        Collection<FieldInfo> fieldInfos = classInfo.propertyFields();

        FieldInfo fieldInfo = fieldInfos.iterator().next();
        assertNull(fieldInfo.relationship());

    }

    /**
     * Find all fields that will be mapped as objects at the end of a relationship
     */
    @Test
    public void testRelationshipFieldInfo() {
        ClassInfo classInfo = metaData.classInfo("Member");
        Collection<FieldInfo> fieldInfos = classInfo.relationshipFields();

        int count = 4;
        assertEquals(count, fieldInfos.size());
        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo.getName().equals("activityList")) count--;
            if (fieldInfo.getName().equals("followees")) count--;
            if (fieldInfo.getName().equals("memberShip")) count--;
            if (fieldInfo.getName().equals("followers")) count--;
        }
        assertEquals(0, count);

    }

    /**
     * Relationship fields provide relationship name via .relationship()
     */
    @Test
    public void testAnnotatedRelationshipFieldInfo() {
        ClassInfo classInfo = metaData.classInfo("Topic");
        Collection<FieldInfo> fieldInfos = classInfo.relationshipFields();

        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo.getName().equals("posts")) assertEquals("HAS_POSTS", fieldInfo.relationship());
        }
    }


    /**
     * Relationship fields provide relationship name via .relationship()
     */
    @Test
    public void testNonAnnotatedRelationshipFieldInfo() {
        ClassInfo classInfo = metaData.classInfo("Topic");
        Collection<FieldInfo> fieldInfos = classInfo.relationshipFields();

        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo.getName().equals("posts")) assertEquals("HAS_POSTS", fieldInfo.relationship());
        }
    }

    /**
     * Relationship fields are not mappable to node properties
     */
    @Test
    public void testRelationshipFieldIsNotAPropertyField() {

        ClassInfo classInfo = metaData.classInfo("Member");
        Collection<FieldInfo> fieldInfos = classInfo.relationshipFields();

        FieldInfo fieldInfo = fieldInfos.iterator().next();
        assertNull(fieldInfo.property());

    }


    /**
     * A property field can be found using its annotated name (node property value)
     */
    @Test
    public void testNamedPropertyField() {
        ClassInfo classInfo = metaData.classInfo("Gold");
        FieldInfo fieldInfo = classInfo.propertyField("annualFees");
        assertEquals("fees", fieldInfo.getName());
    }

    /**
     * A relationship field can be found using its annotated name (relationship type value)
     */
    @Test
    public void testNamedRelationshipField() {
        ClassInfo classInfo = metaData.classInfo("Topic");
        FieldInfo fieldInfo = classInfo.relationshipField("HAS_POSTS");
        assertEquals("posts", fieldInfo.getName());
    }


    /**
     * The default identity getter can be found if it exists.
     */
    @Test
    public void testIdentityGetter() {
        ClassInfo classInfo = metaData.classInfo("Member"); // can also use 'User' here
        MethodInfo methodInfo = classInfo.identityGetter();

        assertEquals("getId", methodInfo.getName());
        //assertEquals(null, methodInfo.property());       todo: fixme
        //assertEquals(null, methodInfo.relationship());
    }

    /**
     * The default identity setter can be found if it exists.
     */
    @Test
    public void testIdentitySetter() {
        ClassInfo classInfo = metaData.classInfo("Member"); // can also use 'User' here
        MethodInfo methodInfo = classInfo.identitySetter();

        assertEquals("setId", methodInfo.getName());
        //assertEquals(null, methodInfo.property());       todo: fixme
        //assertEquals(null, methodInfo.relationship());
    }

    /**
     * A non-default identity getter can be found if it is annotated.
     */
    @Test
    public void testAnnotatedIdentityGetter() {
        ClassInfo classInfo = metaData.classInfo("Activity");
        MethodInfo methodInfo = classInfo.identityGetter();
        assertEquals("getActivityId", methodInfo.getName());
    }

    /**
     * A non-default identity setter can be found if it is annotated.
     */
    @Test
    public void testAnnotatedIdentitySetter() {
        ClassInfo classInfo = metaData.classInfo("Activity");
        MethodInfo methodInfo = classInfo.identitySetter();
        assertEquals("setActivityId", methodInfo.getName());
    }

    @Test
    public void testRelationshipGetters() {
        ClassInfo classInfo = metaData.classInfo("User");
        Collection<MethodInfo> relationshipGetters = classInfo.relationshipGetters();
        int count = 4;
        assertEquals(count, relationshipGetters.size());
        for (MethodInfo relationshipGetter : relationshipGetters) {
            if (relationshipGetter.getName().equals("getActivityList")) count--;
            if (relationshipGetter.getName().equals("getFollowees")) count--;
            if (relationshipGetter.getName().equals("getMemberShip")) count--;
            if (relationshipGetter.getName().equals("getFollowers")) count--;
        }
        assertEquals(0, count);
    }

    /**
     * Can find methods for setting objects which are nodes in the graph as opposed to node properties.
     */
    @Test
    public void testRelationshipSetters() {
        ClassInfo classInfo = metaData.classInfo("User");
        Collection<MethodInfo> relationshipSetters = classInfo.relationshipSetters();
        int count = 4;
        assertEquals(count, relationshipSetters.size());
        for (MethodInfo relationshipSetter : relationshipSetters) {
            if (relationshipSetter.getName().equals("setActivityList")) count--;
            if (relationshipSetter.getName().equals("setFollowees")) count--;
            if (relationshipSetter.getName().equals("setMemberShip")) count--;
            if (relationshipSetter.getName().equals("setFollowers")) count--;
        }
    }

    /**
     * Can find methods for getting objects which can be represented as node properties in the graph
     */
    @Test
    public void testPropertyGetters() {
        ClassInfo classInfo = metaData.classInfo("User");
        Collection<MethodInfo> propertyGetters = classInfo.propertyGetters();
        int count = 5;
        assertEquals(count, propertyGetters.size());
        for (MethodInfo propertyGetter : propertyGetters) {
            if (propertyGetter.getName().equals("getRenewalDate")) count--;
            if (propertyGetter.getName().equals("getUserName")) count--;
            if (propertyGetter.getName().equals("getPassword")) count--;
            if (propertyGetter.getName().equals("getMembershipNumber")) count--;
            if (propertyGetter.getName().equals("getNicknames")) count--;
        }
        assertEquals(0, count);
    }

    /**
     * Can find methods for setting objects which can be represented as node properties in the graph.
     */
    @Test
    public void testPropertySetters() {
        ClassInfo classInfo = metaData.classInfo("User");
        Collection<MethodInfo> propertySetters = classInfo.propertySetters();
        int count = 5;
        assertEquals(count, propertySetters.size());
        for (MethodInfo propertySetter : propertySetters) {
            if (propertySetter.getName().equals("setRenewalDate")) count--;
            if (propertySetter.getName().equals("setUserName")) count--;
            if (propertySetter.getName().equals("setPassword")) count--;
            if (propertySetter.getName().equals("setMembershipNumber")) count--;
            if (propertySetter.getName().equals("setNicknames")) count--;
        }
        assertEquals(0, count);
    }

    @Test
    public void testNamedPropertyGetter() {
        ClassInfo classInfo = metaData.classInfo("Comment");
        MethodInfo methodInfo = classInfo.propertyGetter("remark");
        assertEquals("getComment", methodInfo.getName());
    }

    @Test
    public void testNamedPropertySetter() {
        ClassInfo classInfo = metaData.classInfo("Comment");
        MethodInfo methodInfo = classInfo.propertySetter("remark");
        assertEquals("setComment", methodInfo.getName());
    }

    @Test
    public void testNamedRelationshipGetter() {
        ClassInfo classInfo = metaData.classInfo("Member");
        MethodInfo methodInfo = classInfo.relationshipGetter("HAS_ACTIVITY");
        assertEquals("getActivityList", methodInfo.getName());
    }

    @Test
    public void testNamedRelationshipSetter() {
        ClassInfo classInfo = metaData.classInfo("Member");
        MethodInfo methodInfo = classInfo.relationshipSetter("HAS_ACTIVITY");
        assertEquals("setActivityList", methodInfo.getName());
    }

    @Test
    public void testCanResolveRelationshipEntityFromRelationshipType() {
        ClassInfo classInfo = metaData.resolve("MEMBER_OF");
        assertNotNull("The resolved class info shouldn't be null", classInfo);
        assertEquals("org.neo4j.ogm.domain.canonical.ArbitraryRelationshipEntity", classInfo.name());
    }

    @org.junit.Ignore("I do think we should implement this, but it's not really possible without loading classes")
    @Test
    public void testResolutionOfRelationshipTypeFromMethodInfo() {
        ClassInfo classInfo = metaData.resolve("Forum");
        assertNotNull("The resolved class info shouldn't be null", classInfo);
        assertEquals("org.neo4j.ogm.domain.forum.Forum", classInfo.name());

        final String relationshipType = "HAS_TOPIC";

        // test getters
        MethodInfo relationshipEntityGetter = classInfo.relationshipGetter(relationshipType);
        assertNotNull(relationshipEntityGetter);
        assertEquals(relationshipType, relationshipEntityGetter.relationship());

        // test setters
        MethodInfo relationshipEntitySetter = classInfo.relationshipSetter(relationshipType);
        assertNotNull(relationshipEntitySetter);
        assertEquals(relationshipType, relationshipEntitySetter.relationship());
    }

    @Test
    public void testCanResolveClassHierarchies() {
        ClassInfo classInfo = metaData.resolve("Login", "User");
        assertEquals("org.neo4j.ogm.domain.forum.Member", classInfo.name());
    }

    @Test
    public void testCannotResolveInconsistentClassHierarchies() {
        ClassInfo classInfo = metaData.resolve("Login", "Topic");
        assertNull(classInfo);
    }

    @Test
    public void testClassInfoIsFoundForFQN() {
        String fqn = "org.neo4j.ogm.domain.forum.Topic";
        ClassInfo classInfo = metaData.classInfo(fqn);
        assertEquals(fqn, classInfo.name());
    }

    @Test
    public void testCollectionFieldInfo() {

        ClassInfo classInfo = metaData.classInfo("Member");
        FieldInfo fieldInfo = classInfo.relationshipField("followers");

        assertFalse(classInfo.isScalar( fieldInfo));

    }

    @Test
    public void testArrayFieldInfo() {

        ClassInfo classInfo = metaData.classInfo("Member");
        FieldInfo fieldInfo = classInfo.fieldsInfo().get("nicknames");

        assertFalse(classInfo.isScalar(fieldInfo));

    }

    @Test
    public void testScalarFieldInfo() {

        ClassInfo classInfo = metaData.classInfo("Member");
        FieldInfo fieldInfo = classInfo.fieldsInfo().get("userName");

        assertTrue(classInfo.isScalar(fieldInfo));

    }

    @Test
    public void testFindDateSetter() {
        ClassInfo classInfo = metaData.classInfo("Member");
        List<MethodInfo> methodInfos = classInfo.findSetters(Date.class);
        MethodInfo methodInfo = methodInfos.iterator().next();
        assertEquals("setRenewalDate",methodInfo.getName());
        assertTrue(methodInfo.hasConverter());
    }

    @Test
    public void testFindDateGetter() {
        ClassInfo classInfo = metaData.classInfo("Member");
        List<MethodInfo> methodInfos = classInfo.findGetters(Date.class);
        MethodInfo methodInfo = methodInfos.iterator().next();
        assertEquals("getRenewalDate", methodInfo.getName());
        assertTrue(methodInfo.hasConverter());
    }

    @Test
    public void testFindDateField() {
        ClassInfo classInfo = metaData.classInfo("Member");
        List<FieldInfo> fieldInfos = classInfo.findFields( Date.class);
        FieldInfo fieldInfo = fieldInfos.iterator().next();
        assertEquals("renewalDate", fieldInfo.getName());
        assertTrue(fieldInfo.hasConverter());
    }

    @Test
    public void testFindListFields() {
        ClassInfo classInfo = metaData.classInfo("User");
        List<FieldInfo> fieldInfos = classInfo.findFields(List.class);
        int count = 2;
        assertEquals(count, fieldInfos.size());
        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo.getName().equals("followees")) count--;
            if (fieldInfo.getName().equals("followers")) count--;
        }
        assertEquals(0, count);
    }

    @Test
    public void testFindIterableFields() {
        ClassInfo classInfo = metaData.classInfo("User");
        List<FieldInfo> fieldInfos = classInfo.findIterableFields();
        int count = 4;
        assertEquals(count, fieldInfos.size());
        for (FieldInfo fieldInfo : fieldInfos) {
            if (fieldInfo.getName().equals("followees")) count--;
            if (fieldInfo.getName().equals("followers")) count--;
            if (fieldInfo.getName().equals("activityList")) count--;
            if (fieldInfo.getName().equals("nicknames")) count--;
        }
        assertEquals(0, count);
    }

    @Test
    public void testFindMultipleIterableMethodsWithSameParameterisedType() {
        ClassInfo classInfo = metaData.classInfo("User");
        List<MethodInfo> methodInfos = classInfo.findIterableSetters(Member.class);
        int count = 2;
        assertEquals(count, methodInfos.size());
        for (MethodInfo methodInfo : methodInfos) {
            if (methodInfo.getName().equals("setFollowees")) count--;
            if (methodInfo.getName().equals("setFollowers")) count--;
        }
        assertEquals(count, 0);
    }

    @Test
    public void testFindIterableMethodWithUniqueParameterisedType() {
        ClassInfo classInfo = metaData.classInfo("User");
        List<MethodInfo> methodInfos = classInfo.findIterableSetters(Activity.class);
        int count = 1;
        assertEquals(count, methodInfos.size());
        for (MethodInfo methodInfo : methodInfos) {
            if (methodInfo.getName().equals("setActivityList")) count--;
        }
        assertEquals(count, 0);
    }

    @Test
    /**
     * Taxa corresponding to interfaces with multiple implementations can't be resolved
     */
    public void testInterfaceWithMultipleImplTaxa() {
        assertEquals(null, metaData.resolve("IMembership"));
    }

    @Test
    /**
     * Taxa corresponding to interfaces with a single implementor can be resolved
     *
     * @see DATAGRAPH-577
     */
    public void testInterfaceWithSingleImplTaxa() {
        ClassInfo classInfo = metaData.resolve("AnnotatedInterfaceWithSingleImpl");
        assertNotNull(classInfo);
        assertEquals("org.neo4j.ogm.integration.hierarchy.domain.annotated.AnnotatedChildWithAnnotatedInterface",classInfo.name());
    }

    @Test
    /**
     * Taxa corresponding to abstract classes can't be resolved
     */
    public void testAbstractClassTaxa() {
        assertEquals(null, metaData.resolve("Membership"));
    }

    @Test
    /**
     * Taxa not forming a class hierarchy cannot be resolved.
     */
    public void testNoCommonLeafInTaxa() {
        assertEquals(null, metaData.resolve("Topic", "Member"));
    }

    @Test
    /**
     * The ordering of taxa is unimportant.
     */
    public void testOrderingOfTaxaIsUnimportant() {
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.resolve("Bronze", "Membership", "IMembership").name());
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.resolve("Bronze", "IMembership", "Membership").name());
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.resolve("Membership", "IMembership", "Bronze").name());
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.resolve("Membership", "Bronze", "IMembership").name());
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.resolve("IMembership", "Bronze", "Membership").name());
        assertEquals("org.neo4j.ogm.domain.forum.BronzeMembership", metaData.resolve("IMembership", "Membership", "Bronze").name());
    }

    @Test
    public void testLiskovSubstitutionPrinciple() {
        assertEquals("org.neo4j.ogm.domain.forum.Member", metaData.resolve("Member").name());
        assertEquals("org.neo4j.ogm.domain.forum.Member", metaData.resolve("Login", "Member").name());
    }

    @Test
    /**
     * Taxa not in the domain will be ignored.
     */
    public void testAllNonMemberTaxa() {
        assertEquals(null, metaData.resolve("Knight", "Baronet"));
    }

    @Test
    /**
     * Mixing domain and non-domain taxa is permitted.
     */
    public void testNonMemberAndMemberTaxa() {
        assertEquals("org.neo4j.ogm.domain.forum.SilverMembership", metaData.resolve("Silver", "Pewter", "Tin").name());
    }

    @Test
    public void testLabelsForClassInfo() {
        ClassInfo annotatedClassInfo = metaData.classInfo(Member.class.getSimpleName());
        assertEquals(Arrays.asList("User", "Login"), annotatedClassInfo.labels());

        ClassInfo simpleClassInfo = metaData.classInfo("Topic");
        assertEquals(Arrays.asList("Topic"), simpleClassInfo.labels());


        ClassInfo nonAnnotatedClassInfo = new MetaData("org.neo4j.ogm.domain.education").classInfo(Student.class.getSimpleName());
        assertEquals(Arrays.asList("Student", "DomainObject"), nonAnnotatedClassInfo.labels());
    }

    @Test
    public void testClassInfoForAbstractClassImplementingInterface() {
        assertEquals(1, metaData.classInfo("Membership").interfacesInfo().list().size());
    }

    @Test
    public void testClassInfoForAbstractClassImplementingInterfaceName() {
        assertTrue(metaData.classInfo("Membership").interfacesInfo().list().iterator().next().toString().contains("IMembership"));
    }




}
