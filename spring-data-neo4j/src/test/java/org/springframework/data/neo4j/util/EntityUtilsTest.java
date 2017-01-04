/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
package org.springframework.data.neo4j.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repositories.context.RepositoriesTestContext;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Eric Spiegelberg
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {RepositoriesTestContext.class})
public class EntityUtilsTest extends MultiDriverTestClass {

	@Autowired
    private MovieRepository movieRepository;
	
	@Test
	public void isNew() {
		
		Airplane airplane = new Airplane();
		Boolean isNew = EntityUtils.isNew(airplane);
		Assert.assertTrue(isNew);
		
		// Simulate that the entity has been saved (never assign an id in real life)
		airplane.setId(123);
		
		isNew = EntityUtils.isNew(airplane);
		Assert.assertFalse(isNew);
		
	}
	
	@Test
	public void isNew_GraphId_property_returns_non_Long() {
		
		Unicycle unicycle = new Unicycle();
		Boolean isNew = EntityUtils.isNew(unicycle);
		Assert.assertNull(isNew);
		
	}
	
	/**
	 * Demonstrate determining if the entity is new by using the default "id" 
	 * property (ie: the property is not annotated with @GraphId).
	 */
	@Test
	public void isNew_by_default_property_name_of_id() {
	
		Movie movie = new Movie();
		Boolean isNew = EntityUtils.isNew(movie);
		Assert.assertTrue(isNew);
		
		movieRepository.save(movie);
		isNew = EntityUtils.isNew(movie);
		Assert.assertFalse(isNew);
		
	}
	
	protected class Airplane {

		@GraphId
	    private Long id;

	    // This would never be done in real life but is used to demonstrate a quick test
		public void setId(long id) {
			this.id = id;
		}

	}
	
	protected class Unicycle {

		@GraphId
		// This would never be done in real life but is used to demonstrate a quick test
	    private String guid;

	}
	
}
