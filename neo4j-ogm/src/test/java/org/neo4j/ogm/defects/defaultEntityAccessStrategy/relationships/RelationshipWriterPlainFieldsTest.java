package org.neo4j.ogm.defects.defaultEntityAccessStrategy.relationships;


import org.junit.Test;
import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccess;
import org.neo4j.ogm.entityaccess.FieldWriter;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;

import java.util.List;

import static org.junit.Assert.*;

public class RelationshipWriterPlainFieldsTest {

    private DefaultEntityAccessStrategy entityAccessStrategy = new DefaultEntityAccessStrategy();
    private DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.defects.defaultEntityAccessStrategy.relationships");

    @Test
    public void shouldFindWriterForCollection() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "LIST", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the field", objectAccess instanceof FieldWriter);
        assertEquals("LIST", objectAccess.relationshipName());

    }

    @Test
    public void shouldFindWriterForScalar() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "SCALAR", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the field", objectAccess instanceof FieldWriter);
        assertEquals("SCALAR", objectAccess.relationshipName());

    }


    @Test
    public void shouldFindWriterForArray() {

        ClassInfo classInfo = this.domainInfo.getClass(S.class.getName());

        EntityAccess objectAccess = this.entityAccessStrategy.getRelationalWriter(classInfo, "ARRAY", new T());
        assertNotNull("The resultant object accessor shouldn't be null", objectAccess);
        assertTrue("The access mechanism should be via the field", objectAccess instanceof FieldWriter);
        assertEquals("ARRAY", objectAccess.relationshipName());

    }

    static class S {

        Long id;

        List<T> list;

        T[] array;

        T scalar;

    }

    static class T {

        Long id;

    }

}
