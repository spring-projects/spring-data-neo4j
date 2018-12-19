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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.types.spatial.CartesianPoint2d;
import org.neo4j.ogm.types.spatial.CartesianPoint3d;
import org.neo4j.ogm.types.spatial.GeographicPoint2d;
import org.neo4j.ogm.types.spatial.GeographicPoint3d;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = NativeTypesContextConfiguration.class)
public class SpatialFindByTests {

	@Autowired
	private SpatialDomainRepository repository;

	private SpatialDomain spatialDomain;

	@Before
	public void setUpDomainObject() {
		repository.deleteAll();

		spatialDomain = new SpatialDomain();
		GeographicPoint2d geographicPoint2d = new GeographicPoint2d(1, 2);
		GeographicPoint3d geographicPoint3d = new GeographicPoint3d(3, 4, 5);
		CartesianPoint2d cartesianPoint2d = new CartesianPoint2d(6, 7);
		CartesianPoint3d cartesianPoint3d = new CartesianPoint3d(8, 9, 10);

		spatialDomain.setGeographicPoint2d(geographicPoint2d);
		spatialDomain.setGeographicPoint3d(geographicPoint3d);
		spatialDomain.setCartesianPoint2d(cartesianPoint2d);
		spatialDomain.setCartesianPoint3d(cartesianPoint3d);

		repository.save(spatialDomain);
	}

	@Test
	public void findByGeographicPoint2d() {
		GeographicPoint2d geographicPoint2d = new GeographicPoint2d(1, 2);

		List<SpatialDomain> result = repository.findByGeographicPoint2d(geographicPoint2d);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByGeographicPoint3d() {
		GeographicPoint3d geographicPoint3d = new GeographicPoint3d(3, 4, 5);

		List<SpatialDomain> result = repository.findByGeographicPoint3d(geographicPoint3d);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByCartesianPoint2d() {
		CartesianPoint2d cartesianPoint2d = new CartesianPoint2d(6, 7);

		List<SpatialDomain> result = repository.findByCartesianPoint2d(cartesianPoint2d);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByCartesianPoint3d() {
		CartesianPoint3d cartesianPoint3d = new CartesianPoint3d(8, 9, 10);

		List<SpatialDomain> result = repository.findByCartesianPoint3d(cartesianPoint3d);
		assertThat(result).hasSize(1);
	}
}
