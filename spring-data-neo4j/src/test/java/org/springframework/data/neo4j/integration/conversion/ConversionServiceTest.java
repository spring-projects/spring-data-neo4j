/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.integration.conversion.domain.SiteMember;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Adam George
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ConversionServicePersistenceContext.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ConversionServiceTest {

    @ClassRule
    public static final Neo4jIntegrationTestRule testRule = new Neo4jIntegrationTestRule(7879);

    @Autowired
    private SiteMemberRepository siteMemberRepository;
    @Autowired
    private ConversionService conversionService;

    @Test
    public void shouldBeAbleToConvertBytesIntoBase64EncodedDataViaSpringConversionService() {
        // this should work by virtue of the fact that there's an OGM-level converter defined on a class we've scanned
        assertTrue(this.conversionService.canConvert(byte[].class, String.class));
        assertEquals("YmNkZWY=", this.conversionService.convert(new byte[] {98, 99, 100, 101, 102}, String.class));
    }

    @Test
    public void shouldActuallyConvertSomeStuffProperlyOutOfGraphDatabase() {
        ExecutionEngine execEngine = new ExecutionEngine(testRule.getGraphDatabaseService());
        ExecutionResult rs = execEngine.execute("CREATE (u:SiteMember {profilePictureData:'MTIzNDU2Nzg5'}) RETURN id(u) AS userId");
        Long userId = (Long) rs.columnAs("userId").next();

        byte[] expectedData = "123456789".getBytes();

        SiteMember siteMember = this.siteMemberRepository.findOne(userId);
        assertTrue("The data wasn't converted correctly", Arrays.equals(expectedData, siteMember.getProfilePictureData()));
    }

}
