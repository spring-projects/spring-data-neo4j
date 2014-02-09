/**
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.aspects.support;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import java.util.*;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

/**
 * @author Nicki Watt
 * @since 09-02-2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:org/springframework/data/neo4j/aspects/support/LabelBasedIndexedPropertyEntityTests-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/LabelingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class})
public class LabelBasedIndexedPropertyEntityTests {

	@Autowired
	protected NodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

    @Autowired
    protected GraphDatabaseService graphDatabaseService;
    @Autowired
    protected Neo4jTemplate neo4jTemplate;
    @Autowired
    protected Neo4jMappingContext ctx;

    protected QueryEngine queryEngine;

    protected Thing thing;
    protected SubThing subThing;
    protected SubSubThing subSubThing;
    protected StoredEntityType thingType;
    protected StoredEntityType subThingType;
    protected StoredEntityType subSubThingType;

    @Before
	public void cleanDb() {
        assertTrue("This test expects a Label Based TRS to be in place and it is not!",neo4jTemplate.isLabelBased());
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Neo4jHelper.cleanDb(neo4jTemplate);
            tx.success();
        }
        queryEngine = neo4jTemplate.queryEngineFor(QueryType.Cypher);

        // Hangs if these are done in the same TX see
        // LabelBasedIndexedPropertyHangingEntityTests
        try (Transaction tx = graphDatabaseService.beginTx()) {
            createThing();
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx()) {
            createSubThing();
            tx.success();
        }
        try (Transaction tx = graphDatabaseService.beginTx()) {
            createSubSubThing();
            tx.success();
        }
	}

    @Test
    public void testSchemaBasedPropertyIndexesExistPostCreationForBaseEntity() {
        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName");
    }

    @Test
    public void testSchemaBasedPropertyIndexesExistPostCreationForHierarchicalEntities() {
        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName");

        assertOnlyValidPropertyIndexedLabelsExist(
                (String) subThingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName",
                "schemaIndexedSubThingName");

        assertOnlyValidPropertyIndexedLabelsExist(
                (String) subSubThingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName",
                "schemaIndexedSubThingName",
                "schemaIndexedSubSubThingName");
    }

    @Test
    public void testAbleToQueryUsingSchemaIndex() {
        Collection<String> names = executeQuery("Thing","schemaIndexedThingName","thing-theSchemaIndexedThingName");
        assertThat(  names , containsInAnyOrder( "thing" ));
    }

    @Test
    public void testAbleToQueryUsingSchemaIndexAgainstHierarchy() {
        Collection<String> names1 = executeQuery( "Thing", "schemaIndexedThingName","thing-theSchemaIndexedThingName");
        assertThat(  names1 , containsInAnyOrder( "thing" ));

        Collection<String> names2 = executeQuery( "Thing", "schemaIndexedThingName","subThing-theSchemaIndexedThingName");
        assertThat(  names2 , containsInAnyOrder( "subThing" ));

        Collection<String> names3 = executeQuery( "Thing", "schemaIndexedThingName","subSubThing-theSchemaIndexedThingName");
        assertThat(  names3 , containsInAnyOrder( "subSubThing" ));
    }

    @Test
    public void testAbleToQueryAndFindCommonValuesUsingSchemaIndexAcrossHierarchy() {
        Collection<String> names1 = executeQuery( "Thing", "schemaIndexedCommonName","common");
        assertThat(  names1 , containsInAnyOrder( "thing" ,"subThing" ,"subSubThing" ));

    }

    private Collection<String> executeQuery(String label, String indexedPropName, String indexedPropValue) {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("indexedPropValue",indexedPropValue);
        Result result = queryEngine.query(
                "MATCH (n:"+label+") " +
                        "USING INDEX n:Thing(" + indexedPropName + ") " +
                        "where n." +indexedPropName + " = {indexedPropValue}" +
                        "return n.name", params);
        assertNotNull(result);
        EndResult<String> results = result.to(String.class);
        return IteratorUtil.asCollection(results.iterator());
    }

    /**
     * For a given Label, this method will ensure that the only label index
     * definitions associated with it, are those passed in to the method,
     * throwing an Assertion error if this is not the case
     *
     * @param label
     * @param propertyNames
     */
    private void assertOnlyValidPropertyIndexedLabelsExist(String label, String... propertyNames) {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Iterable<IndexDefinition> idefs = graphDatabaseService.schema().getIndexes(DynamicLabel.label(label));
            Set<String> propNames2Find = new HashSet<String>();
            Set<String> extraPropNames = new HashSet<String>();
            propNames2Find.addAll( Arrays.asList(propertyNames));

            for (IndexDefinition idef: idefs) {
                assertEquals( idef.getLabel().name() , label  );
                for (String key : idef.getPropertyKeys()) {
                    if (propNames2Find.contains(key))
                        propNames2Find.remove(key);
                    else
                        extraPropNames.add(key);
                }
            }

            // We remove all the property names we find so by this point,
            // all we are looking for should be removed
            assertThat( format("Not all properties expected to be created as schema labels found missing (%s)",propNames2Find), propNames2Find, hasSize(0));
            assertThat( format("Additional properties created as schema labels (%s) not expected",extraPropNames) , extraPropNames, hasSize(0));
        }
    }


    protected void createThing() {
        thing = new Thing();
        thing.setName("thing");
        thing.setLegacyIndexedThingName("thing-theLegacyIndexedThingName");
        thing.setSchemaIndexedThingName("thing-theSchemaIndexedThingName");
        thing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(thing);

        thingType = neo4jTemplate.getStoredEntityType(thing);
    }

    protected void createSubThing() {
        subThing = new SubThing();
        subThing.setName("subThing");
        subThing.setLegacyIndexedThingName("subThing-theLegacyIndexedThingName");
        subThing.setSchemaIndexedThingName("subThing-theSchemaIndexedThingName");
        subThing.setLegacyIndexedSubThingName("subThing-theLegacyIndexedSubThingName");
        subThing.setSchemaIndexedSubThingName("subThing-theSchemaIndexedSubThingName");
        subThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(subThing);

        subThingType = neo4jTemplate.getStoredEntityType(subThing);
    }

    protected void createSubSubThing() {
        subSubThing = new SubSubThing();
        subSubThing.setName("subSubThing");
        subSubThing.setLegacyIndexedThingName("subSubThing-theLegacyIndexedThingName");
        subSubThing.setSchemaIndexedThingName("subSubThing-theSchemaIndexedThingName");
        subSubThing.setLegacyIndexedSubThingName("subSubThing-theLegacyIndexedSubThingName");
        subSubThing.setSchemaIndexedSubThingName("subSubThing-theSchemaIndexedSubThingName");
        subSubThing.setLegacyIndexedSubSubThingName("subSubThing-theLegacyIndexedSubSubThingName");
        subSubThing.setSchemaIndexedSubSubThingName("subSubThing-theSchemaIndexedSubSubThingName");
        subSubThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(subSubThing);

        subSubThingType = neo4jTemplate.getStoredEntityType(subSubThing);
    }


	@NodeEntity
	public static class Thing {

		String name;

        @Indexed(indexType = IndexType.SIMPLE)
        String legacyIndexedThingName;

        @Indexed(indexType = IndexType.LABEL, numeric = false)
        String schemaIndexedCommonName;

        @Indexed(indexType = IndexType.LABEL, numeric = false)
        String schemaIndexedThingName;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getLegacyIndexedThingName() {
            return legacyIndexedThingName;
        }

        public void setLegacyIndexedThingName(String legacyIndexedThingName) {
            this.legacyIndexedThingName = legacyIndexedThingName;
        }

        public String getSchemaIndexedThingName() {
            return schemaIndexedThingName;
        }

        public void setSchemaIndexedThingName(String schemaIndexedThingName) {
            this.schemaIndexedThingName = schemaIndexedThingName;
        }

        public String getSchemaIndexedCommonName() {
            return schemaIndexedCommonName;
        }

        public void setSchemaIndexedCommonName(String schemaIndexedCommonName) {
            this.schemaIndexedCommonName = schemaIndexedCommonName;
        }
    }

	public static class SubThing extends Thing {
        @Indexed(indexType = IndexType.SIMPLE)
        String legacyIndexedSubThingName;

        @Indexed(indexType = IndexType.LABEL, numeric = false)
        String schemaIndexedSubThingName;

        public String getLegacyIndexedSubThingName() {
            return legacyIndexedSubThingName;
        }

        public void setLegacyIndexedSubThingName(String legacyIndexedSubThingName) {
            this.legacyIndexedSubThingName = legacyIndexedSubThingName;
        }

        public String getSchemaIndexedSubThingName() {
            return schemaIndexedSubThingName;
        }

        public void setSchemaIndexedSubThingName(String schemaIndexedSubThingName) {
            this.schemaIndexedSubThingName = schemaIndexedSubThingName;
        }
    }

    public static class SubSubThing extends SubThing {
        @Indexed(indexType = IndexType.SIMPLE)
        String legacyIndexedSubSubThingName;

        @Indexed(indexType = IndexType.LABEL, numeric = false)
        String schemaIndexedSubSubThingName;

        public String getLegacyIndexedSubSubThingName() {
            return legacyIndexedSubSubThingName;
        }

        public void setLegacyIndexedSubSubThingName(String legacyIndexedSubSubThingName) {
            this.legacyIndexedSubSubThingName = legacyIndexedSubSubThingName;
        }

        public String getSchemaIndexedSubSubThingName() {
            return schemaIndexedSubSubThingName;
        }

        public void setSchemaIndexedSubSubThingName(String schemaIndexedSubSubThingName) {
            this.schemaIndexedSubSubThingName = schemaIndexedSubSubThingName;
        }
    }
}
