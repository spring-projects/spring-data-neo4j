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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = NativeTypesContextConfiguration.class)
public class SpatialNearTests {

	@Autowired
	private SpatialDomainRepository repository;

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
