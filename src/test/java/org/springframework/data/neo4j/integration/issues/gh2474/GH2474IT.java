/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2474;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Stephen Jackson
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class GH2474IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	Driver driver;

	@Autowired
	BookmarkCapture bookmarkCapture;

	@Autowired
	CityModelRepository cityModelRepository;

	@Autowired
	PersonModelRepository personModelRepository;

	@Autowired
	Neo4jTemplate neo4jTemplate;

	@BeforeEach
	void setupData() {

		cityModelRepository.deleteAll();

		CityModel aachen = new CityModel();
		aachen.setName("Aachen");
		aachen.setExoticProperty("Cars");

		CityModel utrecht = new CityModel();
		utrecht.setName("Utrecht");
		utrecht.setExoticProperty("Bikes");

		cityModelRepository.saveAll(Arrays.asList(aachen, utrecht));
	}

	@Test
	public void testStoreExoticProperty() {

		CityModel cityModel = new CityModel();
		cityModel.setName("The Jungle");
		cityModel.setExoticProperty("lions");
		cityModel = cityModelRepository.save(cityModel);

		CityModel reloaded = cityModelRepository.findById(cityModel.getCityId())
				.orElseThrow(RuntimeException::new);
		assertThat(reloaded.getExoticProperty()).isEqualTo("lions");

		long cnt = cityModelRepository.deleteAllByExoticProperty("lions");
		assertThat(cnt).isOne();
	}

	@Test
	public void testSortOnExoticProperty() {

		Sort sort = Sort.by(Sort.Order.asc("exoticProperty"));
		List<CityModel> cityModels = cityModelRepository.findAll(sort);

		assertThat(cityModels).extracting(CityModel::getExoticProperty).containsExactly("Bikes", "Cars");
	}

	@Test
	public void testSortOnExoticPropertyCustomQuery_MakeSureIUnderstand() {

		Sort sort = Sort.by(Sort.Order.asc("n.name"));
		List<CityModel> cityModels = cityModelRepository.customQuery(sort);

		assertThat(cityModels).extracting(CityModel::getExoticProperty).containsExactly("Cars", "Bikes");
	}

	@Test
	public void testSortOnExoticPropertyCustomQuery() {
		Sort sort = Sort.by(Sort.Order.asc("n.`exotic.property`"));
		List<CityModel> cityModels = cityModelRepository.customQuery(sort);

		assertThat(cityModels).extracting(CityModel::getExoticProperty).containsExactly("Bikes", "Cars");
	}

	@Test // GH-2475
	public void testCityModelProjectionPersistence() {
		CityModel cityModel = new CityModel();
		cityModel.setName("New Cool City");
		cityModel = cityModelRepository.save(cityModel);

		PersonModel personModel = new PersonModel();
		personModel.setName("Mr. Mayor");
		personModel.setAddress("1600 City Avenue");
		personModel.setFavoriteFood("tacos");
		personModelRepository.save(personModel);

		CityModelDTO cityModelDTO = cityModelRepository.findByCityId(cityModel.getCityId())
				.orElseThrow(RuntimeException::new);
		cityModelDTO.setName("Changed name");
		cityModelDTO.setExoticProperty("tigers");

		CityModelDTO.PersonModelDTO personModelDTO = new CityModelDTO.PersonModelDTO();
		personModelDTO.setPersonId(personModelDTO.getPersonId());

		CityModelDTO.JobRelationshipDTO jobRelationshipDTO = new CityModelDTO.JobRelationshipDTO();
		jobRelationshipDTO.setPerson(personModelDTO);

		cityModelDTO.setMayor(personModelDTO);
		cityModelDTO.setCitizens(Collections.singletonList(personModelDTO));
		cityModelDTO.setCityEmployees(Collections.singletonList(jobRelationshipDTO));
		neo4jTemplate.save(CityModel.class).one(cityModelDTO);

		CityModel reloaded = cityModelRepository.findById(cityModel.getCityId())
				.orElseThrow(RuntimeException::new);
		assertThat(reloaded.getName()).isEqualTo("Changed name");
		assertThat(reloaded.getMayor()).isNotNull();
		assertThat(reloaded.getCitizens()).hasSize(1);
		assertThat(reloaded.getCityEmployees()).hasSize(1);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
