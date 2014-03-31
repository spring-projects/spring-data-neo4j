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
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.*;

import static java.lang.String.format;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.*;

/**
 * @author Nicki Watt
 * @since 09-02-2014
 */
public abstract class SchemaIndexingEntityTestBase {

	@Autowired
	protected NodeTypeRepresentationStrategy nodeTypeRepresentationStrategy;

    @Autowired
    protected GraphDatabaseService graphDatabaseService;
    @Autowired
    protected Neo4jTemplate neo4jTemplate;
    @Autowired
    protected Neo4jMappingContext ctx;

    protected QueryEngine queryEngine;
    protected ThingHierarchy thingHierarchy;

    @Before
    public void cleanDb() throws Exception {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Neo4jHelper.cleanDb(neo4jTemplate);
            tx.success();
        }
        queryEngine = neo4jTemplate.queryEngineFor();
        thingHierarchy = new ThingHierarchy(neo4jTemplate);
        thingHierarchy.createThingHierarchyWithTypeAliases();
        thingHierarchy.createThingHierarchyWithoutTypeAliases();
    }

    @Test
    public void testSchemaBasedPropertyIndexesExistPostCreationForBaseEntity() {
        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.typeAliasedThingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName");
    }

    @Test
    public void testSchemaBasedPropertyIndexesExistPostCreationForHierarchicalEntities() {
        testSchemaBasedPropertyIndexesExistPostCreationForTypeAliasedHierarchicalEntities();
        testSchemaBasedPropertyIndexesExistPostCreationForNonTypeAliasedHierarchicalEntities();
    }

    @Test
    public void testAbleToQueryUsingSchemaIndexWhenTypeAliased() {
        Collection<Long> nodeIds = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.typeAliasedThingType.getAlias(), "schemaIndexedThingName", "thing-theSchemaIndexedThingName");
        assertThat(  nodeIds , contains(
                ((NodeBacked)thingHierarchy.typeAliasedThing).getNodeId()));
    }

    @Test
    public void testAbleToQueryUsingSchemaIndexWhenNonTypeAliased() {
        Collection<Long> nodeIds = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.thingType.getAlias(), "schemaIndexedThingName", "thing-theSchemaIndexedThingName");
        assertThat(  nodeIds , contains(
                ((NodeBacked)thingHierarchy.thing).getNodeId()));
    }

    @Test
    public void testAbleToQueryUsingSchemaIndexAgainstHierarchyWhenTypeAliased() {
        Collection<Long> nodeIds1 = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.typeAliasedThingType.getAlias(), "schemaIndexedThingName", "thing-theSchemaIndexedThingName");
        assertThat(  nodeIds1 , contains(
                ((NodeBacked)thingHierarchy.typeAliasedThing).getNodeId()));

        Collection<Long> nodeIds2 = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.typeAliasedThingType.getAlias(), "schemaIndexedThingName", "subThing-theSchemaIndexedThingName");
        assertThat(  nodeIds2 , contains(
                ((NodeBacked)thingHierarchy.typeAliasedSubThing).getNodeId()));

        Collection<Long> nodeIds3 = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.typeAliasedThingType.getAlias(), "schemaIndexedThingName", "subSubThing-theSchemaIndexedThingName");
        assertThat(  nodeIds3 , contains(
                ((NodeBacked)thingHierarchy.typeAliasedSubSubThing).getNodeId()));

    }

    @Test
    public void testAbleToQueryUsingSchemaIndexAgainstHierarchyWhenNonTypeAliased() {
        Collection<Long> nodeIds1 = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.thingType.getAlias(), "schemaIndexedThingName", "thing-theSchemaIndexedThingName");
        assertThat(  nodeIds1 , contains(
                ((NodeBacked) thingHierarchy.thing).getNodeId()));

        Collection<Long> nodeIds2 = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.thingType.getAlias(), "schemaIndexedThingName", "subThing-theSchemaIndexedThingName");
        assertThat(  nodeIds2 , contains(
                ((NodeBacked)thingHierarchy.subThing).getNodeId()));

        Collection<Long> nodeIds3 = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.thingType.getAlias(), "schemaIndexedThingName", "subSubThing-theSchemaIndexedThingName");
        assertThat(  nodeIds3 , contains(
                ((NodeBacked)thingHierarchy.subSubThing).getNodeId()));

    }

    @Test
    public void testAbleToQueryAndFindCommonValuesUsingSchemaIndexAcrossHierarchyWhenTypeAliased() {
        Collection<Long> nodeIds = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.typeAliasedThingType.getAlias(), "schemaIndexedCommonName", "common");
        assertThat(  nodeIds , containsInAnyOrder(
                ((NodeBacked)thingHierarchy.typeAliasedThing).getNodeId(),
                ((NodeBacked)thingHierarchy.typeAliasedSubThing).getNodeId(),
                ((NodeBacked)thingHierarchy.typeAliasedSubSubThing).getNodeId()));
    }

    @Test
    public void testAbleToQueryAndFindCommonValuesUsingSchemaIndexAcrossHierarchyWhenNonTypeAliased() {
        Collection<Long> nodeIds = getNodeIdsAgainstSchemaIndexedPropertyValue((String) thingHierarchy.thingType.getAlias(), "schemaIndexedCommonName", "common");
        assertThat(  nodeIds , containsInAnyOrder(
                ((NodeBacked)thingHierarchy.thing).getNodeId(),
                ((NodeBacked)thingHierarchy.subThing).getNodeId(),
                ((NodeBacked)thingHierarchy.subSubThing).getNodeId()));
    }

    private Collection<Long> getNodeIdsAgainstSchemaIndexedPropertyValue(String label, String indexedPropName, String indexedPropValue) {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("indexedPropValue",indexedPropValue);
        Result result = queryEngine.query(
                "MATCH (n:`"+label+"`) " +
                "USING INDEX n:`" + label + "`(" + indexedPropName + ") " +
                "where n.`" +indexedPropName + "` = {indexedPropValue} " +
                "return DISTINCT ID(n)", params);

        assertNotNull(result);
        Result<Long> results = result.to(Long.class);
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
            assertThat( format("For Label %s, not all of the properties which were expected to be created as schema indexes were found - missing (%s)",label, propNames2Find), propNames2Find, hasSize(0));
            assertThat( format("For Label %s, additional (unexpected) properties were created as schema indexes - these [%s] were not expected",label,extraPropNames) , extraPropNames, hasSize(0));
        }
    }

    public void testSchemaBasedPropertyIndexesExistPostCreationForNonTypeAliasedHierarchicalEntities() {
        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.thingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName");

        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.subThingType.getAlias(),
                "schemaIndexedSubThingName");

        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.subSubThingType.getAlias(),
                "schemaIndexedSubSubThingName");
    }

    public void testSchemaBasedPropertyIndexesExistPostCreationForTypeAliasedHierarchicalEntities() {
        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.typeAliasedThingType.getAlias(),
                "schemaIndexedCommonName",
                "schemaIndexedThingName");

        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.typeAliasedSubThingType.getAlias(),
//                "schemaIndexedCommonName",
//                "schemaIndexedThingName",
                "schemaIndexedSubThingName");

        assertOnlyValidPropertyIndexedLabelsExist(
                (String) thingHierarchy.typeAliasedSubSubThingType.getAlias(),
//                "schemaIndexedCommonName",
//                "schemaIndexedThingName",
//                "schemaIndexedSubThingName",
                "schemaIndexedSubSubThingName");
    }





}
