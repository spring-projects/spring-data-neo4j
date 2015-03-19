/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd"
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.testutil;

import static org.junit.Assert.fail;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.Iterables.count;
import static org.neo4j.tooling.GlobalGraphOperations.at;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;
import org.parboiled.common.StringUtils;

/**
 * Utility methods used to facilitate testing against a real Neo4j database.
 */
public final class GraphTestUtils {

    private GraphTestUtils() {
        // this class cannot be instantiated
    }

    /**
     * Checks that the graph in the specified {@link GraphDatabaseService} is the same as the graph that the given cypher
     * produces.
     *
     * @param graphDatabase The {@link GraphDatabaseService} to check
     * @param sameGraphCypher The Cypher create statement, which communicates the desired state of the database
     * @throws AssertionError if the cypher doesn't produce a graph that matches the state of the given database
     */
    public static void assertSameGraph(GraphDatabaseService graphDatabase, String sameGraphCypher) {
        GraphDatabaseService otherDatabase = new TestGraphDatabaseFactory().newImpermanentDatabase();

        new ExecutionEngine(otherDatabase).execute(sameGraphCypher);

        try {
            try (Transaction tx = graphDatabase.beginTx()) {
                try (Transaction tx2 = otherDatabase.beginTx()) {
                    doAssertSubgraph(graphDatabase, otherDatabase, "existing database");
                    doAssertSubgraph(otherDatabase, graphDatabase, "Cypher-created database");
                    tx2.failure();
                }
                tx.failure();
            }
        } finally {
            otherDatabase.shutdown();
        }
    }

    private static void doAssertSubgraph(GraphDatabaseService database, GraphDatabaseService otherDatabase, String firstDatabaseName) {
        Map<Long, Long[]> sameNodesMap = buildSameNodesMap(database, otherDatabase, firstDatabaseName);
        Set<Map<Long, Long>> nodeMappings = buildNodeMappingPermutations(sameNodesMap, otherDatabase);

        if (nodeMappings.size() == 1) {
            assertRelationshipsMappingExistsForSingleNodeMapping(database, otherDatabase, nodeMappings.iterator().next(), firstDatabaseName);
            return;
        }

        for (Map<Long, Long> nodeMapping : nodeMappings) {
            if (relationshipsMappingExists(database, otherDatabase, nodeMapping)) {
                return;
            }
        }

        fail("There is no corresponding relationship mapping for any of the possible node mappings");
    }

    private static Set<Map<Long, Long>> buildNodeMappingPermutations(Map<Long, Long[]> sameNodesMap, GraphDatabaseService otherDatabase) {
        Set<Map<Long, Long>> result = new HashSet<>();
        result.add(new HashMap<Long, Long>());

        for (Map.Entry<Long, Long[]> entry : sameNodesMap.entrySet()) {

            Set<Map<Long, Long>> newResult = new HashSet<>();

            for (Long target : entry.getValue()) {
                for (Map<Long, Long> mapping : result) {
                    if (!mapping.values().contains(target)) {
                        Map<Long, Long> newMapping = new HashMap<>(mapping);
                        newMapping.put(entry.getKey(), target);
                        newResult.add(newMapping);
                    }
                }
            }

            if (newResult.isEmpty()) {
                fail("Could not find a node corresponding to: " + print(otherDatabase.getNodeById(entry.getKey()))
                        + ". There are most likely more nodes with the same characteristics (labels, properties) in your "
                        + "cypher CREATE statement but fewer in the database.");
            }

            result = newResult;
        }

        return result;
    }

    private static Map<Long, Long[]> buildSameNodesMap(GraphDatabaseService database, GraphDatabaseService otherDatabase,
            String firstDatabaseName) {
        Map<Long, Long[]> sameNodesMap = new HashMap<>(); //map of nodeID and IDs of nodes that match

        for (Node node : at(otherDatabase).getAllNodes()) {
            Iterable<Node> sameNodes = findSameNodes(database, node); //List of all nodes that match this

            //fail fast
            if (!sameNodes.iterator().hasNext()) {
                fail("There is no corresponding node to " + print(node) + " in " + firstDatabaseName);
            }

            Set<Long> sameNodeIds = new HashSet<>();
            for (Node sameNode : sameNodes) {
                sameNodeIds.add(sameNode.getId());
            }
            sameNodesMap.put(node.getId(), sameNodeIds.toArray(new Long[sameNodeIds.size()]));
        }

        return sameNodesMap;
    }

    private static Iterable<Node> findSameNodes(GraphDatabaseService database, Node node) {
        Iterator<Label> labels = node.getLabels().iterator();
        if (labels.hasNext()) {
            return findSameNodesByLabel(database, node, labels.next());
        }

        return findSameNodesWithoutLabel(database, node);
    }

    private static Iterable<Node> findSameNodesWithoutLabel(GraphDatabaseService database, Node node) {
        Set<Node> result = new HashSet<>();

        for (Node candidate : GlobalGraphOperations.at(database).getAllNodes()) {
            if (areSame(node, candidate)) {
                result.add(candidate);
            }
        }

        return result;
    }

    private static Iterable<Node> findSameNodesByLabel(GraphDatabaseService database, Node node, Label label) {
        Set<Node> result = new HashSet<>();

        for (Node candidate : GlobalGraphOperations.at(database).getAllNodesWithLabel(label)) {
            if (areSame(node, candidate)) {
                result.add(candidate);
            }
        }

        return result;
    }

    private static void assertRelationshipsMappingExistsForSingleNodeMapping(GraphDatabaseService database,
            GraphDatabaseService otherDatabase, Map<Long, Long> mapping, String firstDatabaseName) {
        Set<Long> usedRelationships = new HashSet<>();
        for (Relationship relationship : at(otherDatabase).getAllRelationships()) {
            if (!relationshipMappingExists(database, relationship, mapping, usedRelationships)) {
                fail("No corresponding relationship found to " + print(relationship) + " in " + firstDatabaseName);
            }
        }
    }

    private static boolean relationshipsMappingExists(GraphDatabaseService database, GraphDatabaseService otherDatabase,
            Map<Long, Long> mapping) {
        Set<Long> usedRelationships = new HashSet<>();
        for (Relationship relationship : at(otherDatabase).getAllRelationships()) {
            if (!relationshipMappingExists(database, relationship, mapping, usedRelationships)) {
                return false;
            }
        }

        return true;
    }

    private static boolean relationshipMappingExists(GraphDatabaseService database, Relationship relationship, Map<Long, Long> nodeMapping,
            Set<Long> usedRelationships) {
        for (Relationship candidate : database.getNodeById(nodeMapping.get(relationship.getStartNode().getId())).getRelationships(OUTGOING)) {
            if (nodeMapping.get(relationship.getEndNode().getId()).equals(candidate.getEndNode().getId())) {
                if (areSame(candidate, relationship) && !usedRelationships.contains(candidate.getId())) {
                    usedRelationships.add(candidate.getId());
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean areSame(Node node1, Node node2) {
        return haveSameLabels(node1, node2) && haveSameProperties(node1, node2);

    }

    private static boolean areSame(Relationship relationship1, Relationship relationship2) {
        return haveSameType(relationship1, relationship2) && haveSameProperties(relationship1, relationship2);

    }

    private static boolean haveSameLabels(Node node1, Node node2) {
        if (count(node1.getLabels()) != count(node2.getLabels())) {
            return false;
        }

        for (Label label : node1.getLabels()) {
            if (!node2.hasLabel(label)) {
                return false;
            }
        }

        return true;
    }

    private static boolean haveSameType(Relationship relationship1, Relationship relationship2) {
        return relationship1.isType(relationship2.getType());
    }

    private static boolean haveSameProperties(PropertyContainer pc1, PropertyContainer pc2) {
        int pc1KeyCount = 0, pc2KeyCount = 0;
        for (String key : pc1.getPropertyKeys()) {
            pc1KeyCount++;
            if (!pc2.hasProperty(key)) {
                return false;
            }
            if (!stringRepresentationsMatch(pc1.getProperty(key), pc2.getProperty(key))) {
                return false;
            }
        }
        for (Iterator<String> it = pc2.getPropertyKeys().iterator(); it.hasNext(); it.next()) {
            pc2KeyCount++;
        }
        return pc1KeyCount == pc2KeyCount;
    }

    private static String print(Node node) {
        StringBuilder string = new StringBuilder("(");

        List<String> labelNames = new LinkedList<>();
        for (Label label : node.getLabels()) {
            labelNames.add(label.name());
        }
        Collections.sort(labelNames);

        for (String labelName : labelNames) {
            string.append(":").append(labelName);
        }

        String props = propertiesToString(node);
        if (StringUtils.isNotEmpty(props) && !labelNames.isEmpty()) {
            string.append(" ");
        }
        string.append(props);
        string.append(")");
        return string.toString();
    }

    private static String print(Relationship relationship) {
        StringBuilder string = new StringBuilder();

        string.append(print(relationship.getStartNode()));
        string.append("-[:").append(relationship.getType().name());
        String props = propertiesToString(relationship);
        if (StringUtils.isNotEmpty(props)) {
            string.append(" ");
        }
        string.append(props);
        string.append("]->");
        string.append(print(relationship.getEndNode()));

        return string.toString();
    }

    private static String propertiesToString(PropertyContainer propertyContainer) {
        if (!propertyContainer.getPropertyKeys().iterator().hasNext()) {
            return "";
        }

        StringBuilder string = new StringBuilder("{");

        List<String> propertyKeys = new LinkedList<>();
        for (String key : propertyContainer.getPropertyKeys()) {
            propertyKeys.add(key);
        }
        Collections.sort(propertyKeys);

        for (String key : propertyKeys) {
            string.append(key).append(": ").append(propertyValueToString(propertyContainer.getProperty(key))).append(", ");
        }
        string.setLength(string.length() - 2);

        string.append("}");

        return string.toString();
    }

    private static boolean stringRepresentationsMatch(Object one, Object other) {
        String oneString = propertyValueToString(one);
        String otherString = propertyValueToString(other);
        return oneString.equals(otherString);
    }

    private static String propertyValueToString(Object o) {
        if (o instanceof byte[]) {
            return Arrays.toString((byte[]) o);
        }
        if (o instanceof char[]) {
            return Arrays.toString((char[]) o);
        }
        if (o instanceof boolean[]) {
            return Arrays.toString((boolean[]) o);
        }
        if (o instanceof long[]) {
            return Arrays.toString((long[]) o);
        }
        if (o instanceof double[]) {
            return Arrays.toString((double[]) o);
        }
        if (o instanceof int[]) {
            return Arrays.toString((int[]) o);
        }
        if (o instanceof short[]) {
            return Arrays.toString((short[]) o);
        }
        if (o instanceof float[]) {
            return Arrays.toString((float[]) o);
        }
        if (o instanceof String[]) {
            return Arrays.toString((String[]) o);
        }
        return String.valueOf(o);
    }

}
