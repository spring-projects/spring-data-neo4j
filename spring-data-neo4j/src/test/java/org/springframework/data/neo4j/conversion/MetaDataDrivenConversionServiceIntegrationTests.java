/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.conversion;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.conversion.support.ConvertedClass;
import org.springframework.data.neo4j.conversion.support.EntityRepository;
import org.springframework.data.neo4j.conversion.support.EntityWithConvertedAttributes;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Michael J. Simons
 * @soundtrack Murray Gold - Doctor Who Season 9
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MetaDataDrivenConversionServiceIntegrationTests.Config.class)
public class MetaDataDrivenConversionServiceIntegrationTests {

	@Autowired private GraphDatabaseService graphDatabaseService;

	@Autowired private EntityRepository entityRepository;

	@Test // DATAGRAPH-1131
	public void conversionWithConverterHierarchyShouldWork() {

		ConvertedClass convertedClass = new ConvertedClass();
		convertedClass.setValue("Some value");
		EntityWithConvertedAttributes entity = new EntityWithConvertedAttributes("name");
		entity.setConvertedClass(convertedClass);
		entity.setDoubles(Arrays.asList(21.0, 21.0));
		entity.setTheDouble(42.0);
		entityRepository.save(entity);

		Result result = graphDatabaseService
				.execute("MATCH (e:EntityWithConvertedAttributes) RETURN e.convertedClass, e.doubles, e.theDouble");

		assertThat(result.hasNext()).isTrue();
		Map<String, Object> row = result.next();
		assertThat(row) //
				.containsEntry("e.convertedClass", "n/a") //
				.containsEntry("e.doubles", "21.0,21.0") //
				.containsEntry("e.theDouble", "that has been a double");

	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.conversion.support",
			repositoryPackages = "org.springframework.data.neo4j.conversion.support")
	static class Config {}
}
