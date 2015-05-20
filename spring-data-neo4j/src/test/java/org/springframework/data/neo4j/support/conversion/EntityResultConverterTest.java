package org.springframework.data.neo4j.support.conversion;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repository.MemberData;
import org.springframework.data.neo4j.repository.MemberDataPOJO;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit Tests for EntityResultConverter class.
 *
 * @author Nicki Watt
 * @since 12.08.2013
 */
public class EntityResultConverterTest {

    private EntityResultConverter converter;

    @Before
    public void setup() {
        converter = new EntityResultConverter(null);
    }

    @Test
    public void testInterfaceWithDeprecatedMapResultAnnotationIsIdentifiedAsNeedingInterfaceBasedMapping() {
        boolean result = converter.isInterfaceBasedMappingRequest(ADeprecatedMapResultInterface.class);
        assertTrue("Expect interfaces with deprecated @MapResult annotation to be identified correctly", result);
    }

    @Test
    public void testInterfaceWithQueryAnnotationIsIdentifiedAsNeedingInterfaceBasedMapping() {
        boolean result = converter.isInterfaceBasedMappingRequest(MemberData.class);
        assertTrue("Expect interfaces with new @QueryResult annotation to be identified correctly", result);
    }

    @Test
    public void testInterfaceWithNoAnnotationIsNotIdentifiedAsNeedingInterfaceBasedMapping() {
        boolean result = converter.isInterfaceBasedMappingRequest(APlainInterface.class);
        assertFalse("Expect interfaces with no @QueryResult or @MapResult annotation to be identified correctly", result);
    }

    @Test
    public void testPojoWithQueryAnnotationIsNotIdentifiedAsNeedingInterfaceBasedMapping() {
        boolean testResult = converter.isInterfaceBasedMappingRequest(MemberDataPOJO.class);
        assertFalse("POJO annotated class with @QueryResult should not be identified as requiring interface based mapping", testResult);
    }

    @Test
    public void testPojoWithQueryAnnotationIsIdentifiedAsNeedingPOJOBasedMapping() {
        boolean testResult = converter.isPojoBasedMappingRequest(MemberDataPOJO.class);
        assertTrue("POJO annotated class with @QueryResult should be identified as requiring POJO based mapping", testResult);
    }

    @Test
    public void testPojoWithDeprecatedIFAnnotationIsIdentifiedCorrectly() {
        boolean isPojoResult = converter.isPojoBasedMappingRequest(AConfusedPOJO.class);
        boolean isInterfaceResult = converter.isInterfaceBasedMappingRequest(AConfusedPOJO.class);
        assertFalse("POJO using deprecated @MapResult interface annotation should not be identified as requiring POJO based mapping", isPojoResult);
        assertFalse("POJO using deprecated @MapResult annotation should not be identified as requiring Interface based mapping", isInterfaceResult);
    }

    @Test
    public void testInheritedPojoIsFullyConverted() {
        Map<String, Object> value = new HashMap<>();
        value.put("a", 42);
        value.put("b", 21);

        ChildPojo result = (ChildPojo) converter.convert(value, ChildPojo.class);

        assertEquals(Integer.valueOf(42), result.getA());
        assertEquals(Integer.valueOf(21), result.getB());
    }

    @Test
    @Ignore("added for discussion")
    public void testClashingInheritedFieldNameForMojoMapping() {
        Map<String, Object> value = new HashMap<>();
        value.put("a", 23);
        value.put("a_bis", "23");

        ConflictingPojo result = (ConflictingPojo) converter.convert(value, ConflictingPojo.class);

        assertEquals(Integer.valueOf(23), result.getA());
        assertEquals("23", result.getABis());
    }

    @Test
    public void testInheritedInterfaceIsFullyConverted() {
        Map<String, Object> value = new HashMap<>();
        value.put("boss", person(22L, "Michael Hunger"));
        value.put("employeeId", 23);

        AChildInterface result = (AChildInterface) converter.convert(value, AChildInterface.class);

        assertEquals("Michael Hunger", result.getBoss().getName());
        assertEquals(Integer.valueOf(23), result.getEmployeeId());
    }

    private Person person(long id, String name) {
        Person person = new Person();
        ReflectionTestUtils.setField(person, "graphId", id);
        person.setName(name);
        return person;
    }

}

@MapResult
interface ADeprecatedMapResultInterface {

    @ResultColumn("collect(team)")
    Iterable<Group> getTeams();

    @ResultColumn("boss")
    Person getBoss();
}

interface AParentInterface {

    @ResultColumn("boss")
    Person getBoss();
}

@QueryResult
interface AChildInterface extends AParentInterface {

    @ResultColumn("employeeId")
    Integer getEmployeeId();
}

interface APlainInterface {

    Iterable<Group> getTeams();

    Person getBoss();
}

@MapResult
class AConfusedPOJO {

    @ResultColumn("collect(team)")
    private Iterable<Group> teams;

    @ResultColumn("boss")
    private Person boss;
}

class ParentPojo {

    @ResultColumn("a")
    private Integer a;

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }
}

@QueryResult
class ChildPojo extends ParentPojo {

    @ResultColumn("b")
    private Integer b;

    public Integer getB() {
        return b;
    }

    public void setB(Integer b) {
        this.b = b;
    }
}

@QueryResult
class ConflictingPojo extends ParentPojo {

    @ResultColumn("a_bis")
    private String a;

    public String getABis() {
        return a;
    }

    public void setABis(String a) {
        this.a = a;
    }
}
