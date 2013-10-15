package org.springframework.data.neo4j.repository.query;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit Test(s) of the StartClauseFactory
 */
public class StartClauseFactoryTest {

    PartInfo simpleIndexedPartInfo1;
    PartInfo simpleIndexedPartInfo2;
    PartInfo searchableIndexedPartInfo;
    PartInfo fullTextIndexedPartInfo;
    PartInfo idBasedPartInfo;
    PartInfo relBasedPartInfo;

    @Before
    public void setUp() throws Exception {

        // TODO - Perhaps rather use Spring to setup rather than mock
        simpleIndexedPartInfo1 = Mockito.mock(PartInfo.class);
        when(simpleIndexedPartInfo1.isIndexed()).thenReturn(true);
        when(simpleIndexedPartInfo1.isFullText()).thenReturn(false);

        simpleIndexedPartInfo2 = Mockito.mock(PartInfo.class);
        when(simpleIndexedPartInfo2.isIndexed()).thenReturn(true);
        when(simpleIndexedPartInfo2.isFullText()).thenReturn(false);

        searchableIndexedPartInfo = Mockito.mock(PartInfo.class);
        when(searchableIndexedPartInfo.isIndexed()).thenReturn(true);
        when(searchableIndexedPartInfo.isFullText()).thenReturn(false);
        when(searchableIndexedPartInfo.getType()).thenReturn(Part.Type.STARTING_WITH);

        fullTextIndexedPartInfo = Mockito.mock(PartInfo.class);
        when(fullTextIndexedPartInfo.isIndexed()).thenReturn(true);
        when(fullTextIndexedPartInfo.isFullText()).thenReturn(true);

        Neo4jPersistentProperty leafProperty1 = Mockito.mock(Neo4jPersistentProperty.class);
        idBasedPartInfo = Mockito.mock(PartInfo.class);
        when(idBasedPartInfo.isIndexed()).thenReturn(false);
        when(idBasedPartInfo.getLeafProperty()).thenReturn(leafProperty1);
        when(leafProperty1.isRelationship()).thenReturn(false);
        when(leafProperty1.isIdProperty()).thenReturn(true);

        Neo4jPersistentProperty leafProperty2 = Mockito.mock(Neo4jPersistentProperty.class);
        relBasedPartInfo = Mockito.mock(PartInfo.class);
        when(relBasedPartInfo.isIndexed()).thenReturn(false);
        when(relBasedPartInfo.getLeafProperty()).thenReturn(leafProperty2);
        when(leafProperty2.isRelationship()).thenReturn(true);
        when(leafProperty2.isIdProperty()).thenReturn(false);


    }

    @Test
    public void testCreateExactIndexBasedStartClause() {
       StartClause startClause = StartClauseFactory.create(simpleIndexedPartInfo1);
       assertTrue(startClause instanceof ExactIndexBasedStartClause);
    }

    @Test
    public void testCreateFullTextIndexBasedStartClauseWhenFullyIndexed() {
        StartClause startClause = StartClauseFactory.create(fullTextIndexedPartInfo);
        assertTrue(startClause instanceof FullTextIndexBasedStartClause);
    }

    @Test
    public void testCreateFullTextIndexBasedStartClauseWhenNormallyIndexedButWithSearchLikePart() {
        StartClause startClause = StartClauseFactory.create(searchableIndexedPartInfo);
        assertTrue(startClause instanceof FullTextIndexBasedStartClause);
    }

    @Test
    public void testCreateGraphIdStartClauseWhenIdPropertyPathInfo() {
        StartClause startClause = StartClauseFactory.create(idBasedPartInfo);
        assertTrue(startClause instanceof GraphIdStartClause);
    }

    @Test
    public void testCreateGraphIdStartClauseWhenIsRelationship() {
        // This is how it was in the original code, not sure why
        // relationships should always result in a GraphIdStartClause
        // TODO - find out why
        StartClause startClause = StartClauseFactory.create(relBasedPartInfo);
        assertTrue(startClause instanceof GraphIdStartClause);
    }

    @Test
    public void testCreateFullTextIndexBasedStartClauseWhenMultiplePartsProvidedWhichAreAllIndexedWithSameId() {
        List<PartInfo> partInfos = new ArrayList<PartInfo>();
        partInfos.add(simpleIndexedPartInfo1);
        partInfos.add(simpleIndexedPartInfo2);
        when(simpleIndexedPartInfo1.sameIdentifier(simpleIndexedPartInfo1)).thenReturn(true);
        when(simpleIndexedPartInfo2.sameIdentifier(simpleIndexedPartInfo1)).thenReturn(true);

        StartClause startClause = StartClauseFactory.create(partInfos);
        assertTrue(startClause instanceof FullTextIndexBasedStartClause);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionThrownWhenProvidingMultiplePartsWhichAreAllIndexedButDiffId() {
        List<PartInfo> partInfos = new ArrayList<PartInfo>();
        partInfos.add(simpleIndexedPartInfo1);
        partInfos.add(simpleIndexedPartInfo2);
        when(simpleIndexedPartInfo1.sameIdentifier(simpleIndexedPartInfo1)).thenReturn(true);
        when(simpleIndexedPartInfo2.sameIdentifier(simpleIndexedPartInfo1)).thenReturn(false);
        StartClauseFactory.create(partInfos);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExceptionThrownWhenProvidingMultiplePartsWhichAreNotAllIndexed() {
        List<PartInfo> partInfos = new ArrayList<PartInfo>();
        partInfos.add(simpleIndexedPartInfo1);
        partInfos.add(idBasedPartInfo);
        when(simpleIndexedPartInfo1.sameIdentifier(simpleIndexedPartInfo1)).thenReturn(true);
        StartClauseFactory.create(partInfos);
    }


}
