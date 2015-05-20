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

package org.neo4j.ogm.unit.mapper.cypher;

import org.junit.Test;
import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.request.strategy.VariableDepthQuery;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class NodeEntityQueryTest {

    private final QueryStatements query = new VariableDepthQuery();

    @Test
    public void testFindOne() throws Exception {
        assertEquals("MATCH (n) WHERE id(n) = { id } WITH n MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p)", query.findOne(0L, 2).getStatement());
    }

    @Test
    public void testFindAllCollection() throws Exception {
        assertEquals("MATCH (n) WHERE id(n) in { ids } WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), 1).getStatement());
    }

    @Test
    public void testFindAll() throws Exception {
        assertEquals("MATCH p=()-->() RETURN p", query.findAll().getStatement());
    }

    @Test
    public void testFindByLabel() throws Exception {
        assertEquals("MATCH (n:`Orbit`) WITH n MATCH p=(n)-[*0..3]-(m) RETURN collect(distinct p)", query.findByType("Orbit", 3).getStatement());
    }

    @Test
    public void testFindByProperty() throws Exception {
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` = { `diameter` } WITH n MATCH p=(n)-[*0..4]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add("diameter", 60.2), 4).getStatement());
    }

    @Test
    public void testFindOneZeroDepth() throws Exception {
        assertEquals("MATCH (n) WHERE id(n) = { id } RETURN n", query.findOne(0L, 0).getStatement());
    }

    @Test
    public void testFindAllCollectionZeroDepth() throws Exception {
        assertEquals("MATCH (n) WHERE id(n) in { ids } RETURN collect(n)", query.findAll(Arrays.asList(1L, 2L, 3L), 0).getStatement());
    }

    @Test
    public void testFindByLabelZeroDepth() throws Exception {
        assertEquals("MATCH (n:`Orbit`) RETURN collect(n)", query.findByType("Orbit", 0).getStatement());
    }

    @Test
    public void testFindByPropertyZeroDepth() throws Exception {
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` = { `diameter` } WITH n MATCH p=(n)-[*0..0]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add("diameter", 60.2), 0).getStatement());
    }

    /**
     * @see DATAGRAPH-589
     * @throws Exception
     */
    @Test
    public void testFindByLabelWithIllegalCharacters() throws Exception {
        assertEquals("MATCH (n:`l'artiste`) WITH n MATCH p=(n)-[*0..3]-(m) RETURN collect(distinct p)", query.findByType("l'artiste", 3).getStatement());
    }

    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindOneNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n)-[*0..]-(m) WHERE id(n) = { id } RETURN collect(distinct p)", query.findOne(0L, -1).getStatement());
    }

    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindAllCollectionNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n)-[*0..]-(m) WHERE id(n) in { ids } RETURN collect(distinct p)", query.findAll(Arrays.asList(1L, 2L, 3L), -1).getStatement());
    }


    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindByLabelNegativeDepth() throws Exception {
        assertEquals("MATCH p=(n:`Orbit`)-[*0..]-(m) RETURN collect(distinct p)", query.findByType("Orbit", -1).getStatement());
    }

    /**
     * @see DATAGRAPH-595
     * @throws Exception
     */
    @Test
    public void testFindByPropertyNegativeDepth() throws Exception {
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` = { `diameter` }  WITH n MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add("diameter", 60.2), -1).getStatement());
    }

    /**
     * @see DATAGRAPH-631
     * @throws Exception
     */
    @Test
    public void testFindByPropertyWithIllegalCharacters() throws Exception {
        assertEquals("MATCH (n:`Studio`) WHERE n.`studio-name` = { `studio-name` } WITH n MATCH p=(n)-[*0..3]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Studio", new Filters().add("studio-name", "Abbey Road Studios"), 3).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     * @throws Exception
     */
    @Test
    public void testFindByPropertyGreaterThan() throws Exception {
        Filter parameter = new Filter("diameter",60);
        parameter.setComparisonOperator(ComparisonOperator.GREATER_THAN);
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` > { `diameter` } WITH n MATCH p=(n)-[*0..4]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(parameter), 4).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByMultipleAndProperties() {
        Filter nameParam = new Filter("name","AST-1");
        Filter diameterParam = new Filter("diameter", 60);
        diameterParam.setComparisonOperator(ComparisonOperator.LESS_THAN);
        diameterParam.setBooleanOperator(BooleanOperator.AND);
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`name` = { `name` } AND n.`diameter` < { `diameter` } WITH n MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(nameParam).add(diameterParam), 2).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByMultipleOrProperties() {
        Filter nameParam = new Filter("name","AST-1");
        Filter diameterParam = new Filter("diameter", 60);
        diameterParam.setComparisonOperator(ComparisonOperator.GREATER_THAN);
        diameterParam.setBooleanOperator(BooleanOperator.OR);
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`name` = { `name` } OR n.`diameter` > { `diameter` } WITH n MATCH p=(n)-[*0..2]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(nameParam).add(diameterParam), 2).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByNestedPropertyOutgoing() {
        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("OUTGOING");
        assertEquals("MATCH (n:`Asteroid`) MATCH (x:`Planet`) WHERE x.`name` = { `name` }  MATCH (n)-[:`COLLIDES`]->(x) WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(planetParam), 1).getStatement());

    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByNestedPropertyIncoming() {
        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("INCOMING");
        assertEquals("MATCH (n:`Asteroid`) MATCH (x:`Planet`) WHERE x.`name` = { `name` }  MATCH (n)<-[:`COLLIDES`]-(x) WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(planetParam), 1).getStatement());

    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByNestedPropertyUndirected() {
        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("UNDIRECTED");
        assertEquals("MATCH (n:`Asteroid`) MATCH (x:`Planet`) WHERE x.`name` = { `name` }  MATCH (n)-[:`COLLIDES`]-(x) WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(planetParam), 1).getStatement());

    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByMultipleNestedProperties() {
        Filter diameterParam = new Filter("diameter", 60);
        diameterParam.setComparisonOperator(ComparisonOperator.GREATER_THAN);

        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setBooleanOperator(BooleanOperator.AND);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("OUTGOING");
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` > { `diameter` }  MATCH (x:`Planet`) WHERE x.`name` = { `name` }  MATCH (n)-[:`COLLIDES`]->(x) WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(diameterParam).add(planetParam), 1).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByMultipleNestedPropertiesInfiniteDepth() {
        Filter diameterParam = new Filter("diameter", 60);
        diameterParam.setComparisonOperator(ComparisonOperator.GREATER_THAN);

        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setBooleanOperator(BooleanOperator.AND);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("OUTGOING");
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` > { `diameter` }  MATCH (x:`Planet`) WHERE x.`name` = { `name` }  MATCH (n)-[:`COLLIDES`]->(x)  WITH n MATCH p=(n)-[*0..]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(diameterParam).add(planetParam), -1).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByMultipleNestedPropertiesOred() {
        Filter diameterParam = new Filter("diameter", 60);
        diameterParam.setComparisonOperator(ComparisonOperator.GREATER_THAN);

        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setBooleanOperator(BooleanOperator.OR);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("OUTGOING");
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` > { `diameter` }  OPTIONAL MATCH (x:`Planet`) WHERE x.`name` = { `name` }  OPTIONAL MATCH (n)-[:`COLLIDES`]->(x) WITH n MATCH p=(n)-[*0..1]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(diameterParam).add(planetParam), 1).getStatement());
    }

    /**
     * @see DATAGRAPH-629
     */
    @Test
    public void testFindByMultipleNestedPropertiesOredDepth0() {
        Filter diameterParam = new Filter("diameter", 60);
        diameterParam.setComparisonOperator(ComparisonOperator.GREATER_THAN);

        Filter planetParam = new Filter();
        planetParam.setPropertyName("name");
        planetParam.setPropertyValue("Earth");
        planetParam.setComparisonOperator(ComparisonOperator.EQUALS);
        planetParam.setBooleanOperator(BooleanOperator.OR);
        planetParam.setNestedPropertyName("collidesWith");
        planetParam.setNestedEntityTypeLabel("Planet");
        planetParam.setRelationshipType("COLLIDES");
        planetParam.setRelationshipDirection("OUTGOING");
        assertEquals("MATCH (n:`Asteroid`) WHERE n.`diameter` > { `diameter` }  OPTIONAL MATCH (x:`Planet`) WHERE x.`name` = { `name` }  OPTIONAL MATCH (n)-[:`COLLIDES`]->(x) WITH n MATCH p=(n)-[*0..0]-(m) RETURN collect(distinct p), ID(n)", query.findByProperties("Asteroid", new Filters().add(diameterParam).add(planetParam), 0).getStatement());
    }
}
