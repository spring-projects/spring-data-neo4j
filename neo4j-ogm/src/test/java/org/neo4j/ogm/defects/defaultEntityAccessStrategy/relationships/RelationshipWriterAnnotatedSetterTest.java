package org.neo4j.ogm.defects.defaultEntityAccessStrategy.relationships;


import org.junit.Test;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccess;
import org.neo4j.ogm.entityaccess.MethodWriter;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;

import java.util.List;

import static org.junit.Assert.*;

public class RelationshipWriterAnnotatedSetterTest {

    private DefaultEntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();
    private DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.defects.defaultEntityAccessStrategy.relationships");

    @Test
    public void shouldFindWriterForCollection() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "LIST", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        assertEquals("LIST", objectAccess.relationshipName());
        assertEquals(List.class, objectAccess.type()) ;

    }

    @Test
    public void shouldFindWriterForScalar() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "SCALAR", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        assertEquals("SCALAR", objectAccess.relationshipName());
        assertEquals(T.class, objectAccess.type());

    }


    @Test
    public void shouldFindWriterForArray() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "ARRAY", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the method", objectAccess instanceof MethodWriter);
        assertEquals("ARRAY", objectAccess.relationshipName());
        assertEquals(T[].class, objectAccess.type());

    }

    static class S {

        Long id;

        List<T> list;
        T[] array;
        T scalar;

        @Relationship(type="LIST", direction=Relationship.OUTGOING)
        public void setList(List<T> list) {
            this.list = list;
        }

        @Relationship(type="ARRAY", direction=Relationship.OUTGOING)
        public void setArray(T[] array) {
            this.array = array;
        }

        @Relationship(type="SCALAR", direction=Relationship.OUTGOING)
        public void setScalar(T scalar) {
            this.scalar = scalar;
        }

    }

    static class T {

        Long id;

    }

}
