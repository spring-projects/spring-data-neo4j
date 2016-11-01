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

import java.util.Date;
import java.util.List;

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

/**
 *
 * Tests that we support each kind of keyword specified by Part.Type
 *
 * @author Jasper Blues
 */
public class RestaurantIT extends MultiDriverTestClass {

	@Autowired
	private RestaurantRepository restaurantRepository;

	@After
	public void tearDown() {
		restaurantRepository.deleteAll();
	}


	/**
	 * @see DATAGRAPH-561
	 */
	@Test
	public void shouldFindRestaurantsNear_nameParameterFirst() {
		Assume.assumeTrue(Components.neo4jVersion() >= 3);
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)",
				new Point(37.61649, -122.38681), 94128);
		restaurantRepository.save(restaurant);

		List<Restaurant> results = restaurantRepository.findByNameAndLocationNear(
				"San Francisco International Airport (SFO)",
				new Distance(150, Metrics.KILOMETERS), new Point(37.6, -122.3));

		assertNotNull(results);
		assertEquals(1, results.size());
		Restaurant found = results.get(0);
		assertNotNull(found.getLocation());
		assertEquals(37.61649, found.getLocation().getX(), 0);
		assertEquals(-122.38681, found.getLocation().getY(), 0);
	}

	/**
	 * @see DATAGRAPH-561
	 */
	@Test
	public void shouldFindRestaurantsNear_locationFirst() {
		Assume.assumeTrue(Components.neo4jVersion() >= 3);
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)",
				new Point(37.61649, -122.38681), 94128);
		restaurantRepository.save(restaurant);

		List<Restaurant> results = restaurantRepository.findByLocationNearAndName(
				new Distance(150, Metrics.KILOMETERS), new Point(37.6, -122.3),
				"San Francisco International Airport (SFO)");

		assertNotNull(results);
		assertEquals(1, results.size());
		Restaurant found = results.get(0);
		assertNotNull(found.getLocation());
		assertEquals(37.61649, found.getLocation().getX(), 0);
		assertEquals(-122.38681, found.getLocation().getY(), 0);
	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindRestaurantsWithScoreBetween() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(70.0, 80.0);
		assertNotNull(results);
		assertEquals(1, results.size());

		List<Restaurant> shouldBeEmpty = restaurantRepository.findByScoreBetween(30.0, 40.0);
		assertNotNull(shouldBeEmpty);
		assertEquals(0, shouldBeEmpty.size());
	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByDescriptionIsNullOrNotNull() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)",
				new Point(37.61649, -122.38681), 94128);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", "Mostly Ramen");
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByDescriptionIsNull();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("San Francisco International Airport (SFO)", results.get(0).getName());

		results = restaurantRepository.findByDescriptionIsNotNull();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Kuroda", results.get(0).getName());
	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByScoreLessThan() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 81.3);
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByScoreLessThan(75);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Kuroda", results.get(0).getName());

		results = restaurantRepository.findByScoreLessThan(72.4);
		assertNotNull(results);
		assertEquals(0, results.size());

		results = restaurantRepository.findByScoreLessThanEqual(72.4);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Kuroda", results.get(0).getName());
	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByScoreGreaterThan() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 81.3);
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByScoreGreaterThan(75);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Cyma", results.get(0).getName());

		results = restaurantRepository.findByScoreGreaterThan(90.0);
		assertNotNull(results);
		assertEquals(0, results.size());

		results = restaurantRepository.findByScoreGreaterThanEqual(81.3);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Cyma", results.get(0).getName());
	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByLaunchDateBefore() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		kuroda.setLaunchDate(new Date(1000));
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.5);
		cyma.setLaunchDate(new Date(2000));
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByLaunchDateBefore(new Date(1001));
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Kuroda", results.get(0).getName());

		results = restaurantRepository.findByLaunchDateBefore(new Date(999));
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByLaunchDateAfter() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		kuroda.setLaunchDate(new Date(1000));
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.5);
		cyma.setLaunchDate(new Date(2000));
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByLaunchDateAfter(new Date(1500));
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("Cyma", results.get(0).getName());

		results = restaurantRepository.findByLaunchDateAfter(new Date(3000));
		assertNotNull(results);
		assertEquals(0, results.size());
	}

	/**
	 * All findByPropertyLike does currently is to require an exact match, ignoring case.
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByNameNotLike() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameNotLike("kuroda");
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("San Francisco International Airport (SFO)", results.get(0).getName());

	}


	/**
	 * All findByPropertyLike does currently is to require an exact match, ignoring case.
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByNameLike() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameLike("*san francisco international*");
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("San Francisco International Airport (SFO)", results.get(0).getName());

	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByNameStartingWith() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameStartingWith("San Francisco");
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("San Francisco International Airport (SFO)", results.get(0).getName());

	}

	/**
	 * @see DATAGRAPH-904
	 */
	@Test
	public void shouldFindByNameEndingWith() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameEndingWith("Airport (SFO)");
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals("San Francisco International Airport (SFO)", results.get(0).getName());

	}

}
