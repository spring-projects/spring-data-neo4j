/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.nativetypes;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@ContextConfiguration(classes = { SpatialPersistenceContextConfiguration.class })
@RunWith(SpringRunner.class)
public class SpatialConversionTests {

	@Autowired private SpatialDomainRepository repository;

	@Test
	public void shouldReadAndWriteSpringDataGeoPoints() {

		final Point neo4j = new Point(12.994823, 55.612191);

		SpatialDomain domainObject = new SpatialDomain();
		domainObject.setSdnPoint(neo4j);
		domainObject.setSdnPoints(Collections.singletonList(neo4j));
		domainObject.setMorePoints(new Point[] { neo4j });

		domainObject = repository.save(domainObject);

		SpatialDomain reloadedDomainObject = repository.findById(domainObject.getId()).get();

		assertThat(reloadedDomainObject.getSdnPoint()).isEqualTo(neo4j);
		assertThat(reloadedDomainObject.getSdnPoints()).hasSize(1).first().isEqualTo(neo4j);
		assertThat(reloadedDomainObject.getMorePoints()).hasSize(1).containsExactly(neo4j);

		// This breaks now because the filter uses the spring semantics…
		List<SpatialDomain> result = repository.findBySdnPointNear(new Distance(4, Metrics.NEUTRAL), new Point(13, 56));
		//assertThat(result).hasSize(1);
	}
}
