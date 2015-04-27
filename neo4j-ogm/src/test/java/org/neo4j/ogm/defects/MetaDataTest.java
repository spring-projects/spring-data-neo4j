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

package org.neo4j.ogm.defects;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.AnnotationInfo;
import org.neo4j.ogm.metadata.info.ClassInfo;

/**
 * @author Luanne Misquitta
 */
public class MetaDataTest {

	private MetaData metaData;

	@Before
	public void setUp() {
		metaData = new MetaData("org.neo4j.ogm.domain.forum", "org.neo4j.ogm.domain.canonical","org.neo4j.ogm.integration.hierarchy.domain","org.neo4j.ogm.domain.cineasts.annotated");
	}

	/**
	 * @see DATAGRAPH-615
	 */
	@Test
	public void testDefaultLabelOfNodeEntities() {
		ClassInfo classInfo = metaData.classInfo("Forum");
		AnnotationInfo annotationInfo = classInfo.annotationsInfo().get(NodeEntity.class.getName());
		assertEquals("Forum", annotationInfo.get("label", ""));
	}

	/**
	 * @see DATAGRAPH-615
	 */
	@Test
	public void testDefaultLabelOfRelationshipEntities() {
		ClassInfo classInfo = metaData.classInfo("Nomination");
		AnnotationInfo annotationInfo = classInfo.annotationsInfo().get(RelationshipEntity.class.getName());
		assertEquals("NOMINATION", annotationInfo.get("type",""));
	}

}
