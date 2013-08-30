package org.springframework.data.neo4j.support.conversion;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repository.MemberData;
import org.springframework.data.neo4j.repository.MemberDataPOJO;


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
        boolean testResult = converter.isPojoBasedMappingReqest(MemberDataPOJO.class);
        assertTrue("POJO annotated class with @QueryResult should be identified as requiring POJO based mapping", testResult);
    }

    @Test
    public void testPojoWithDeprecatedIFAnnotationIsIdentifiedCorrectly() {
        boolean isPojoResult = converter.isPojoBasedMappingReqest(AConfusedPOJO.class);
        boolean isInterfaceResult = converter.isInterfaceBasedMappingRequest(AConfusedPOJO.class);
        assertFalse("POJO using deprecated @MapResult interface annotation should not be identified as requiring POJO based mapping", isPojoResult);
        assertFalse("POJO using deprecated @MapResult annotation should not be identified as requiring Interface based mapping", isInterfaceResult);
    }

}

@MapResult
interface ADeprecatedMapResultInterface {

    @ResultColumn("collect(team)")
    Iterable<Group> getTeams();

    @ResultColumn("boss")
    Person getBoss();
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
