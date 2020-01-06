/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.nativetypes;

import static org.apache.webbeans.util.Asserts.*;
import static org.apache.webbeans.util.Asserts.assertNotNull;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import lombok.Builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.examples.restaurants.repo.RestaurantRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Useful links: https://maps.google.com/maps?q=55.612191,12.994823 where q -> latLong
 * <p>
 * https://maps.google.com/maps?q=37.61649,-122.38681
 * <p>
 * https://www.latlong.net/Show-Latitude-Longitude.html
 */
@ContextConfiguration(classes = { SpatialPersistenceContextConfiguration.class })
@RunWith(SpringRunner.class)
public class SpatialConversionTests {

	@Autowired
	private SpatialDomainRepository repository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private SessionFactory sessionFactory;

	@Builder
	static class ParamHolder {
		String name;
		double latitude;
		double longitude;

		Map<String, Object> toParameterMap() {
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("name", this.name);
			parameters.put("latitude", this.latitude);
			parameters.put("longitude", this.longitude);
			return parameters;
		}
	}

	private static final ParamHolder NEO_HQ = ParamHolder.builder().latitude(55.612191).longitude(12.994823)
			.name("Neo4j HQ").build();
	private static final ParamHolder CLARION = ParamHolder.builder().latitude(55.607726).longitude(12.994243)
			.name("Clarion").build();
	private static final ParamHolder MINC = ParamHolder.builder().latitude(55.611496).longitude(12.994039).name("Minc")
			.build();

	@Test
	public void shouldReadAndWriteSpringDataGeoPoints() {

		// The SDN Points have been converted this way by org.springframework.data.neo4j.conversion.PointConverter
		Point sdnPoint = new Point(NEO_HQ.latitude, NEO_HQ.longitude);

		SpatialDomain domainObject = new SpatialDomain();
		domainObject.setSdnPoint(sdnPoint);
		domainObject.setSdnPoints(Collections.singletonList(sdnPoint));
		domainObject.setMorePoints(new Point[] { sdnPoint });

		domainObject = repository.save(domainObject);

		SpatialDomain reloadedDomainObject = repository.findById(domainObject.getId()).get();

		assertThat(reloadedDomainObject.getSdnPoint()).isEqualTo(sdnPoint);
		assertThat(reloadedDomainObject.getSdnPoints()).hasSize(1).first().isEqualTo(sdnPoint);
		assertThat(reloadedDomainObject.getMorePoints()).hasSize(1).containsExactly(sdnPoint);
	}


	@Test
	public void findByPointShouldWorkWhenConvertedToNative() {

		Driver driver = sessionFactory.unwrap(Driver.class);
		try (Session session = driver.session(); Transaction transaction = session.beginTransaction()) {

			for (ParamHolder useNativePoints : Arrays.asList(NEO_HQ, CLARION)) {

				transaction.run(
						"CREATE (n:SpatialDomain {name: $name, sdnPoint: point({latitude: $latitude, longitude: $longitude})})",
						useNativePoints.toParameterMap());
			}
			transaction.run("CREATE (n:SpatialDomain {name: $name, latitude: $latitude, longitude: $longitude})",
					MINC.toParameterMap());

			transaction.commit();
		}

		List<SpatialDomain> result = repository.findBySdnPointNear(new Distance(60.0 / 1000.0, Metrics.KILOMETERS),
				new Point(55.611883, 12.994608));
		assertThat(result).hasSize(2).extracting(SpatialDomain::getName).containsExactlyInAnyOrder("Neo4j HQ", "Minc");
	}

	@Test
	public void registeredOldConverterShouldStillWork() {
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
}
