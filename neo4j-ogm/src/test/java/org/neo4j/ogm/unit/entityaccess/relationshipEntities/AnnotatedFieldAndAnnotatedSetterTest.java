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

package org.neo4j.ogm.unit.entityaccess.relationshipEntities;

import org.junit.Test;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccess;
import org.neo4j.ogm.entityaccess.MethodWriter;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Luanne Misquitta
 */
public class AnnotatedFieldAndAnnotatedSetterTest {

    private DefaultEntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();
    private DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.unit.entityaccess.relationshipEntities");


    @Test
    public void shouldPreferAnnotatedSetterWhenAnnotatedFieldIsPresentForRelationshipEntity() {
        ClassInfo classInfo = this.domainInfo.getClass(End.class.getName());
        Set<? extends RelEntity> parameter = new HashSet<>();

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "REL_ENTITY_TYPE", new RelEntity());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        End end = new End();
        objectAccess.write(end, parameter);
        assertEquals(end.getRelEntities(), parameter);
    }

    @RelationshipEntity(type="REL_ENTITY_TYPE")
    public static class RelEntity {
        Long id;
        @StartNode
        Start start;
        @EndNode
        End end;

        public RelEntity() {
        }

        public End getEnd() {
            return end;
        }

        public void setEnd(End end) {
            this.end = end;
        }

        public Start getStart() {
            return start;
        }

        public void setStart(Start start) {
            this.start = start;
        }
    }

    public static class Start {
        Long id;
        String name;
        @Relationship(type = "REL_ENTITY_TYPE", direction = "OUTGOING")
        Set<RelEntity> relEntities;

        public Start() {
        }
    }

    public static class End {
        Long id;
        String name;
        @Relationship(type = "REL_ENTITY_TYPE", direction="INCOMING")
        Set<RelEntity> relEntities;

        public End() {
        }

        public Set<RelEntity> getRelEntities() {
            return relEntities;
        }

        @Relationship(type = "REL_ENTITY_TYPE", direction="INCOMING")
        public void setRelEntities(Set<RelEntity> relEntities) {
            this.relEntities = relEntities;
        }
    }
}
