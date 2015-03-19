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

import org.junit.Test;
import org.neo4j.ogm.metadata.RelationshipUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author Mark Angrish
 */
public class RelationshipUtilsTest {

    @Test
    public void testFieldNameInferenceFromRelationshipType() {
        expect("writesPolicy", RelationshipUtils.inferFieldName("WRITES_POLICY"));
    }

    @Test
    public void testGetterNameInference() {
        expect("getWritesPolicy", RelationshipUtils.inferGetterName("WRITES_POLICY"));
    }

    @Test
    public void testSetterNameInference() {
        expect("setWritesPolicy", RelationshipUtils.inferSetterName("WRITES_POLICY"));
    }

    //
    @Test
    public void testRelationshipTypeInferenceFromFieldName() {
        expect("WRITES_POLICY", RelationshipUtils.inferRelationshipType("writesPolicy"));
    }

    @Test
    public void testRelationshipTypeInferenceFromGetterName() {
        expect("WRITES_POLICY", RelationshipUtils.inferRelationshipType("getWritesPolicy"));
    }

    @Test
    public void testRelationshipTypeInferenceSetterName() {
        expect("WRITES_POLICY", RelationshipUtils.inferRelationshipType("setWritesPolicy"));
    }

    //
    @Test
    public void testSimpleFieldNameInferenceFromRelationshipType() {
        expect("policy", RelationshipUtils.inferFieldName("POLICY"));
    }

    @Test
    public void testSimpleGetterNameInference() {
        expect("getPolicy", RelationshipUtils.inferGetterName("POLICY"));
    }

    @Test
    public void testSimpleSetterNameInference() {
        expect("setPolicy", RelationshipUtils.inferSetterName("POLICY"));
    }

    //
    @Test
    public void testSimpleRelationshipTypeInferenceFromFieldName() {
        expect("POLICY", RelationshipUtils.inferRelationshipType("policy"));
    }

    @Test
    public void testSimpleRelationshipTypeInferenceFromGetterName() {
        expect("POLICY", RelationshipUtils.inferRelationshipType("getPolicy"));
    }

    @Test
    public void testSimpleRelationshipTypeInferenceSetterName() {
        expect("POLICY", RelationshipUtils.inferRelationshipType("setPolicy"));
    }

    private void expect(String expected, String actual) {
        assertEquals(expected, actual);
    }
}
