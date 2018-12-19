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
 */

package org.springframework.data.neo4j.nativetypes;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.CartesianPoint3d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint3d;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = NativeTypesContextConfiguration.class)
public class SpatialNearTests {

	@Autowired private SpatialDomainRepository repository;

	@Test
	public void findByNearCartesianPoint2d() {
		SpatialDomain spatialDomain = new SpatialDomain();
		spatialDomain.setCartesianPoint2d(new CartesianPoint2d(1, 2));
		repository.save(spatialDomain);
		List<SpatialDomain> result = repository.findByCartesianPoint2dNear(new Distance(2, Metrics.NEUTRAL),
				new CartesianPoint2d(2, 2));
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByNearCartesianPoint2dNoMatch() {
		SpatialDomain spatialDomain = new SpatialDomain();
		spatialDomain.setCartesianPoint2d(new CartesianPoint2d(1, 2));
		repository.save(spatialDomain);
		List<SpatialDomain> result = repository.findByCartesianPoint2dNear(new Distance(2, Metrics.NEUTRAL),
				new CartesianPoint2d(20, 20));
		assertThat(result).hasSize(0);
	}

	@Test
	public void findByNearCartesianPoint3d() {
		SpatialDomain spatialDomain = new SpatialDomain();
		spatialDomain.setCartesianPoint3d(new CartesianPoint3d(1, 2, 3));
		repository.save(spatialDomain);
		List<SpatialDomain> result = repository.findByCartesianPoint3dNear(new Distance(2, Metrics.NEUTRAL),
				new CartesianPoint3d(2, 2, 2));
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByNearCartesianPoint3dNoMatch() {
		SpatialDomain spatialDomain = new SpatialDomain();
		spatialDomain.setCartesianPoint3d(new CartesianPoint3d(1, 2, 3));
		repository.save(spatialDomain);
		List<SpatialDomain> result = repository.findByCartesianPoint3dNear(new Distance(2, Metrics.NEUTRAL),
				new CartesianPoint3d(20, 20, 20));
		assertThat(result).hasSize(0);
	}

	@Test
	public void findByNearGeographicPoint2d() {
		SpatialDomain centralStation = new SpatialDomain();

		GeographicPoint2d centralStationLocation = new GeographicPoint2d(55.6093093, 13.0004377);
		centralStation.setGeographicPoint2d(centralStationLocation);
		repository.save(centralStation);

		GeographicPoint2d office = new GeographicPoint2d(55.611851, 12.9949028);
		List<SpatialDomain> result = repository.findByGeographicPoint2dNear(new Distance(0.4486, Metrics.KILOMETERS),
				office);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByNearGeographicPoint2dNoMatch() {
		SpatialDomain centralStation = new SpatialDomain();

		GeographicPoint2d centralStationLocation = new GeographicPoint2d(55.6093093, 13.0004377);
		centralStation.setGeographicPoint2d(centralStationLocation);
		repository.save(centralStation);

		GeographicPoint2d office = new GeographicPoint2d(55.611851, 12.9949028);
		List<SpatialDomain> result = repository.findByGeographicPoint2dNear(new Distance(0.4485, Metrics.KILOMETERS),
				office);
		assertThat(result).hasSize(0);
	}

	@Test
	public void findByNearGeographicPoint3d() {
		SpatialDomain centralStation = new SpatialDomain();

		GeographicPoint3d centralStationLocation = new GeographicPoint3d(55.6093093, 13.0004377, -5);
		centralStation.setGeographicPoint3d(centralStationLocation);
		repository.save(centralStation);

		GeographicPoint3d office = new GeographicPoint3d(55.611851, 12.9949028, 15);
		List<SpatialDomain> result = repository.findByGeographicPoint3dNear(new Distance(0.4489591, Metrics.KILOMETERS),
				office);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByNearGeographicPoint3dNoMatch() {
		SpatialDomain centralStation = new SpatialDomain();

		GeographicPoint3d centralStationLocation = new GeographicPoint3d(55.6093093, 13.0004377, -5);
		centralStation.setGeographicPoint3d(centralStationLocation);
		repository.save(centralStation);

		GeographicPoint3d office = new GeographicPoint3d(55.611851, 12.9949028, 15);
		List<SpatialDomain> result = repository.findByGeographicPoint3dNear(new Distance(0.448950, Metrics.KILOMETERS),
				office);
		assertThat(result).hasSize(0);
	}

	@After
	public void cleanUp() {
		repository.deleteAll();
	}
}
