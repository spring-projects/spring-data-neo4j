/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.examples.restaurants;

import static org.apache.webbeans.util.Asserts.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.examples.restaurants.domain.Diner;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.examples.restaurants.repo.RestaurantRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests that we support each kind of keyword specified by Part.Type
 *
 * @author Jasper Blues
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { RestaurantTests.RestaurantContext.class })
@RunWith(SpringRunner.class)
@DirtiesContext
public class RestaurantTests {

	@Autowired private RestaurantRepository restaurantRepository;

	@After
	public void tearDown() {
		restaurantRepository.deleteAll();
	}

	/**
	 * This test, as the below one does, asserts that the parameter index for each query part is set correctly. Most query
	 * parts are associated with one parameter, while certain kinds, such as NEAR, require more.
	 */
	@Test // DATAGRAPH-561
	public void shouldFindRestaurantsNear_nameParameterFirst() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", new Point(37.61649, -122.38681),
				94128);
		restaurantRepository.save(restaurant);

		List<Restaurant> results = restaurantRepository.findByNameAndLocationNear(
				"San Francisco International Airport (SFO)", new Distance(150, Metrics.KILOMETERS), new Point(37.6, -122.3));

		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		Restaurant found = results.get(0);
		assertNotNull(found.getLocation());
		assertThat(found.getLocation().getX()).isCloseTo(37.61649, offset(0d));
		assertThat(found.getLocation().getY()).isCloseTo(-122.38681, offset(0d));
	}

	/**
	 * This test, as the above one does, asserts that the parameter index for each query part is set correctly. Most query
	 * parts are associated with one parameter, while certain kinds, such as NEAR, require more.
	 */
	@Test // DATAGRAPH-561
	public void shouldFindRestaurantsNear_locationFirst() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", new Point(37.61649, -122.38681),
				94128);
		restaurantRepository.save(restaurant);

		List<Restaurant> results = restaurantRepository.findByLocationNearAndName(new Distance(150, Metrics.KILOMETERS),
				new Point(37.6, -122.3), "San Francisco International Airport (SFO)");

		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		Restaurant found = results.get(0);
		assertNotNull(found.getLocation());
		assertThat(found.getLocation().getX()).isCloseTo(37.61649, offset(0d));
		assertThat(found.getLocation().getY()).isCloseTo(-122.38681, offset(0d));
	}

	@Test // DATAGRAPH-904
	public void shouldFindRestaurantsWithScoreBetween() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(70.0, 80.0);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);

		List<Restaurant> shouldBeEmpty = restaurantRepository.findByScoreBetween(30.0, 40.0);
		assertNotNull(shouldBeEmpty);
		assertThat(shouldBeEmpty.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-1027
	public void shouldFindRestaurantsWithScoreBetweenInclusive() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(20.0, 80.6);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(3);
	}

	@Test // DATAGRAPH-1202
	public void shouldFindRestaurantsWithScoreBetweenRangeInclusiveBothBounds() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		Range.Bound<Double> lowerBoundInclusive = Range.Bound.inclusive(20.0);
		Range.Bound<Double> upperBoundInclusive = Range.Bound.inclusive(80.6);
		Range<Double> inclusiveBound = Range.of(lowerBoundInclusive, upperBoundInclusive);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(inclusiveBound);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(3);
	}

	@Test // DATAGRAPH-1202
	public void shouldFindRestaurantsWithScoreBetweenRangeExclusiveBothBounds() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		Range.Bound<Double> lowerBoundInclusive = Range.Bound.exclusive(20.0);
		Range.Bound<Double> upperBoundInclusive = Range.Bound.exclusive(80.6);
		Range<Double> inclusiveBound = Range.of(lowerBoundInclusive, upperBoundInclusive);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(inclusiveBound);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
	}

	@Test // DATAGRAPH-1202
	public void shouldFindRestaurantsWithScoreBetweenRangeInclusiveLowerBounds() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		Range.Bound<Double> lowerBoundInclusive = Range.Bound.inclusive(20.0);
		Range.Bound<Double> upperBoundInclusive = Range.Bound.exclusive(80.6);
		Range<Double> inclusiveBound = Range.of(lowerBoundInclusive, upperBoundInclusive);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(inclusiveBound);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(2);
	}

	@Test // DATAGRAPH-1202
	public void shouldFindRestaurantsWithScoreBetweenRangeInclusiveUpperBounds() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.6);
		restaurantRepository.save(cyma);

		Restaurant awful = new Restaurant("Awful", 20.0);
		restaurantRepository.save(awful);

		Range.Bound<Double> lowerBoundInclusive = Range.Bound.exclusive(20.0);
		Range.Bound<Double> upperBoundInclusive = Range.Bound.inclusive(80.6);
		Range<Double> inclusiveBound = Range.of(lowerBoundInclusive, upperBoundInclusive);

		List<Restaurant> results = restaurantRepository.findByScoreBetween(inclusiveBound);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(2);
	}

	@Test // DATAGRAPH-904
	public void shouldFindByPropertyIsNull() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", new Point(37.61649, -122.38681),
				94128);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", "Mostly Ramen");
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByDescriptionIsNull();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName())
				.isEqualTo("San Francisco International Airport (SFO)");

	}

	@Test // DATAGRAPH-904
	public void shouldFindByPropertyIsNotNull() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", new Point(37.61649, -122.38681),
				94128);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", "Mostly Ramen");
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByDescriptionIsNotNull();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");
	}

	@Test // DATAGRAPH-904
	public void shouldFindBNestedProperty_different_entity_type_IsNull() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", new Point(37.61649, -122.38681),
				94128);

		Diner diner = new Diner("Jasper", null);
		restaurant.addRegularDiner(diner);

		restaurantRepository.save(restaurant);

		List<Restaurant> results = restaurantRepository.findByRegularDinersLastNameIsNull();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName())
				.isEqualTo("San Francisco International Airport (SFO)");
	}

	@Test // DATAGRAPH-904
	public void shouldFindBNestedProperty_same_entity_type_IsNull() {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", new Point(37.61649, -122.38681),
				94128);

		Diner diner = new Diner("Jasper", null);
		restaurant.addRegularDiner(diner);

		Restaurant kuroda = new Restaurant("Kuroda", null);
		restaurant.addSimilarRestaurant(kuroda);

		restaurantRepository.save(restaurant);

		List<Restaurant> results = restaurantRepository.findBySimilarRestaurantsDescriptionIsNull();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName())
				.isEqualTo("San Francisco International Airport (SFO)");
	}

	@Test // DATAGRAPH-904
	public void shouldFindByScoreLessThan() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 81.3);
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByScoreLessThan(75);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");

		results = restaurantRepository.findByScoreLessThan(72.4);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(0);

		results = restaurantRepository.findByScoreLessThanEqual(72.4);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");
	}

	@Test // DATAGRAPH-904
	public void shouldFindByScoreGreaterThan() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 81.3);
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByScoreGreaterThan(75);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Cyma");

		results = restaurantRepository.findByScoreGreaterThan(90.0);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(0);

		results = restaurantRepository.findByScoreGreaterThanEqual(81.3);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Cyma");
	}

	@Test // DATAGRAPH-904
	public void shouldFindByLaunchDateBefore() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		kuroda.setLaunchDate(new Date(1000));
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.5);
		cyma.setLaunchDate(new Date(2000));
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByLaunchDateBefore(new Date(1001));
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");

		results = restaurantRepository.findByLaunchDateBefore(new Date(999));
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(0);
	}

	@Test // DATAGRAPH-904
	public void shouldFindByLaunchDateAfter() {
		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		kuroda.setLaunchDate(new Date(1000));
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", 80.5);
		cyma.setLaunchDate(new Date(2000));
		restaurantRepository.save(cyma);

		List<Restaurant> results = restaurantRepository.findByLaunchDateAfter(new Date(1500));
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Cyma");

		results = restaurantRepository.findByLaunchDateAfter(new Date(3000));
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(0);
	}

	void runTestThatShouldReturnOnlySFO(Supplier<List<Restaurant>> restaurantSupplier) {
		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantSupplier.get();
		assertNotNull(results);
		assertThat(results).extracting(Restaurant::getName).containsExactly("San Francisco International Airport (SFO)");
	}

	/**
	 * All findByPropertyLike does currently is to require an exact match, ignoring case.
	 */
	@Test // DATAGRAPH-904
	public void shouldFindByNameNotLike() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameNotLike("kuroda"));
	}

	/**
	 * All findByPropertyLike does currently is to require an exact match, ignoring case.
	 */
	@Test // DATAGRAPH-904
	public void shouldFindByNameLike() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameLike("*san francisco international*"));
	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameStartingWith() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameStartingWith("San Francisco"));

	}

	@Test // DATAGRAPH-1862
	public void shouldFindByNameStartingWithIgnoringCase() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameStartingWithIgnoreCase("San Francisco".toLowerCase(Locale.ROOT)));
	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameEndingWith() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameEndingWith("Airport (SFO)"));
	}

	@Test // DATAGRAPH-1862
	public void shouldFindByNameEndingWithIgnoringCase() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameEndingWithIgnoreCase("Airport (SFO)".toLowerCase(Locale.ROOT)));
	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameContaining() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameContaining("International Airport");
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName())
				.isEqualTo("San Francisco International Airport (SFO)");

		results = restaurantRepository.findByNameNotContaining("International Airport");
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");

	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameContainingOrDescriptionIsNull() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		Restaurant cyma = new Restaurant("Cyma", "Greek Stuff");

		List<Restaurant> results = restaurantRepository.findByNameContaining("International Airport");
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName())
				.isEqualTo("San Francisco International Airport (SFO)");

		results = restaurantRepository.findByNameNotContainingOrDescriptionIsNull("International Airport");
		Collections.sort(results);
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(2);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");
		assertThat(results.get(1).getName())
				.isEqualTo("San Francisco International Airport (SFO)");

	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameIn() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameIn(Arrays.asList("Kuroda", "Foo", "Bar"));
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");

		results = restaurantRepository.findByNameNotIn(Arrays.asList("Kuroda", "Foo", "Bar"));
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName())
				.isEqualTo("San Francisco International Airport (SFO)");

	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameMatchesRegEx() {

		runTestThatShouldReturnOnlySFO(() -> restaurantRepository.findByNameMatchesRegex("(?i)san francisco.*"));
	}

	@Test // DATAGRAPH-904
	public void shouldFindByNameExists() {

		Restaurant restaurant = new Restaurant("San Francisco International Airport (SFO)", 68.0);
		restaurantRepository.save(restaurant);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByNameExists();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(2);

	}

	@Test // DATAGRAPH-904
	public void shouldFindByPropertyIsTrue() {
		Restaurant kazan = new Restaurant("Kazan", 77.0);
		kazan.setHalal(true);
		restaurantRepository.save(kazan);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		kuroda.setHalal(false);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByHalalIsTrue();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kazan");
	}

	@Test // DATAGRAPH-904
	public void shouldFindByPropertyIsFalse() {
		Restaurant kazan = new Restaurant("Kazan", 77.0);
		kazan.setHalal(true);
		restaurantRepository.save(kazan);

		Restaurant kuroda = new Restaurant("Kuroda", 72.4);
		kuroda.setHalal(false);
		restaurantRepository.save(kuroda);

		List<Restaurant> results = restaurantRepository.findByHalalIsFalse();
		assertNotNull(results);
		assertThat(results.size()).isEqualTo(1);
		assertThat(results.get(0).getName()).isEqualTo("Kuroda");
	}

	@Test // DATAGRAPH-1197
	public void shouldCheckExistenceForDerivedQueryMethod() {
		Restaurant restaurant = new Restaurant("R1", "good");
		restaurantRepository.save(restaurant);

		assertThat(restaurantRepository.existsByDescription("good")).isTrue();
	}

	@Test // DATAGRAPH-1197
	public void shouldCheckNonExistenceForDerivedQueryMethod() {
		Restaurant restaurant = new Restaurant("R1", "good");
		restaurantRepository.save(restaurant);

		assertThat(restaurantRepository.existsByDescription("bad")).isFalse();
	}

	@Test // DATAGRAPH-1197
	public void shouldCheckExistenceForQueryMethod() {
		Restaurant restaurant = new Restaurant("R1", "good");
		restaurantRepository.save(restaurant);

		assertThat(restaurantRepository.existenceOfAGoodRestaurant()).isTrue();
	}

	@Test // DATAGRAPH-1197
	public void shouldCheckNonExistenceForQueryMethod() {
		Restaurant restaurant = new Restaurant("R1", "bad");
		restaurantRepository.save(restaurant);

		assertThat(restaurantRepository.existenceOfAGoodRestaurant()).isFalse();
	}

	@Test // DATAGRAPH-1199
	public void shouldCheckExistenceForExistsQueryMethod() {
		Restaurant restaurant = new Restaurant("R1", "good");
		restaurantRepository.save(restaurant);

		assertThat(restaurantRepository.existenceOfAGoodRestaurantWithExistsQuery())
				.isTrue();
	}

	@Test // DATAGRAPH-1199
	public void shouldCheckNonExistenceForExistsQueryMethod() {
		Restaurant restaurant = new Restaurant("R1", "bad");
		restaurantRepository.save(restaurant);

		assertThat(restaurantRepository.existenceOfAGoodRestaurantWithExistsQuery())
				.isFalse();
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.examples.restaurants.domain",
			repositoryPackages = "org.springframework.data.neo4j.examples.restaurants.repo")
	@ComponentScan("org.springframework.data.neo4j.examples.restaurants")
	static class RestaurantContext {}
}
