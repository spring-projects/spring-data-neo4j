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


package org.springframework.data.neo4j.examples.restaurants;

import static org.apache.webbeans.util.Asserts.assertNotNull;
import static org.junit.Assert.*;

import java.util.Collection;

import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.examples.restaurants.context.RestaurantContext;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.examples.restaurants.repo.RestaurantRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = {RestaurantContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RestaurantIT extends MultiDriverTestClass {

	@Autowired
	private RestaurantRepository restaurantRepository;

	@After
	public void tearDown()
	{
		restaurantRepository.deleteAll();
	}


	@Test
	public void shouldFindRestaurantsNear_nameParameterFirst() {
		Assume.assumeTrue(Components.neo4jVersion() >= 3);
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)",
				new Point(37.61649, -122.38681), 94128);
		restaurantRepository.save(restaurant);

		Collection<Restaurant> results = restaurantRepository.findByNameAndLocationNear(
				"San Francisco International Airport (SFO)",
				new Distance(150, Metrics.KILOMETERS), new Point(37.6, -122.3));

		assertNotNull(results);
		assertEquals(1, results.size());
	}

	@Test
	public void shouldFindRestaurantsNear_locationFirst() {
		Assume.assumeTrue(Components.neo4jVersion() >= 3);
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)",
				new Point(37.61649, -122.38681), 94128);
		restaurantRepository.save(restaurant);

		Collection<Restaurant> results = restaurantRepository.findByLocationNearAndName(
				new Distance(150, Metrics.KILOMETERS), new Point(37.6, -122.3),
				"San Francisco International Airport (SFO)");

		assertNotNull(results);
		assertEquals(1, results.size());
	}
}
