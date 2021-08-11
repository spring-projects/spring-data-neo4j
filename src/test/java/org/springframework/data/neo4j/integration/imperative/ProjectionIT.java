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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.DepartmentEntity;
import org.springframework.data.neo4j.integration.shared.common.PersonDepartmentQueryResult;
import org.springframework.data.neo4j.integration.shared.common.PersonEntity;
import org.springframework.data.neo4j.integration.shared.common.NamesOnly;
import org.springframework.data.neo4j.integration.shared.common.NamesOnlyDto;
import org.springframework.data.neo4j.integration.shared.common.NamesWithSpELCity;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonSummary;
import org.springframework.data.neo4j.integration.shared.common.ProjectionTest1O1;
import org.springframework.data.neo4j.integration.shared.common.ProjectionTestLevel1;
import org.springframework.data.neo4j.integration.shared.common.ProjectionTestRoot;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ProjectionIT {

	private static final String FIRST_NAME = "Hans";
	private static final String FIRST_NAME2 = "Lieschen";
	private static final String LAST_NAME = "Mueller";
	private static final String CITY = "Braunschweig";

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final BookmarkCapture bookmarkCapture;
	private Long projectionTestRootId;
	private Long projectionTest1O1Id;
	private Long projectionTestLevel1Id;

	@Autowired
	ProjectionIT(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	void setup() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
		Transaction transaction = session.beginTransaction();) {

			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (p:PersonEntity {id: 'p1', email: 'p1@dep1.org'}) -[:MEMBER_OF]->(department:DepartmentEntity {id: 'd1', name: 'Dep1'}) RETURN p");

			for (Map.Entry<String, String> person : new Map.Entry[] {
					new AbstractMap.SimpleEntry(FIRST_NAME, LAST_NAME),
					new AbstractMap.SimpleEntry(FIRST_NAME2, LAST_NAME),
			}) {
				transaction.run(" MERGE (address:Address{city: $city})"
								+ "CREATE (:Person{firstName: $firstName, lastName: $lastName})"
								+ "-[:LIVES_AT]-> (address)",
						Values.parameters("firstName", person.getKey(), "lastName", person.getValue(), "city", CITY));
			}

			Record result = transaction.run("create (r:ProjectionTestRoot {name: 'root'}) \n"
									 + "create (o:ProjectionTest1O1 {name: '1o1'}) "
									 + "create (l11:ProjectionTestLevel1 {name: 'level11'})\n"
									 + "create (l12:ProjectionTestLevel1 {name: 'level12'})\n"
									 + "create (l21:ProjectionTestLevel2 {name: 'level21'})\n"
									 + "create (l22:ProjectionTestLevel2 {name: 'level22'})\n"
									 + "create (l23:ProjectionTestLevel2 {name: 'level23'})\n"
									 + "create (r) - [:ONE_OONE] -> (o)\n"
									 + "create (r) - [:LEVEL_1] -> (l11)\n"
									 + "create (r) - [:LEVEL_1] -> (l12)\n"
									 + "create (l11) - [:LEVEL_2] -> (l21)\n"
									 + "create (l11) - [:LEVEL_2] -> (l22)\n"
									 + "create (l12) - [:LEVEL_2] -> (l23)\n"
									 + "return id(r), id(l11), id(o)").single();

			projectionTestRootId = result.get(0).asLong();
			projectionTestLevel1Id = result.get(1).asLong();
			projectionTest1O1Id = result.get(2).asLong();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void loadNamesOnlyProjection(@Autowired ProjectionPersonRepository repository) {

		Collection<NamesOnly> people = repository.findByLastName(LAST_NAME);
		assertThat(people).hasSize(2);

		assertThat(people).extracting(NamesOnly::getFirstName).containsExactlyInAnyOrder(FIRST_NAME, FIRST_NAME2);
		assertThat(people).extracting(NamesOnly::getLastName).containsOnly(LAST_NAME);

		assertThat(people).extracting(NamesOnly::getFullName).containsExactlyInAnyOrder(FIRST_NAME + " " + LAST_NAME, FIRST_NAME2 + " " + LAST_NAME);
	}

	@Test // GH-2325
	void loadNamesWithCityProjection(@Autowired ProjectionPersonRepository repository) {

		Collection<NamesWithSpELCity> people = repository.findProjectionByLastName(LAST_NAME);
		assertThat(people).hasSize(2);

		assertThat(people).extracting(NamesWithSpELCity::getFirstName).containsExactlyInAnyOrder(FIRST_NAME, FIRST_NAME2);
		assertThat(people).extracting(NamesWithSpELCity::getLastName).containsOnly(LAST_NAME);

		assertThat(people).extracting(NamesWithSpELCity::getCity).containsExactlyInAnyOrder(CITY, CITY);
	}

	@Test
	void loadPersonSummaryProjection(@Autowired ProjectionPersonRepository repository) {
		Collection<PersonSummary> people = repository.findByFirstName(FIRST_NAME);
		assertThat(people).hasSize(1);

		PersonSummary person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);
		assertThat(person.getAddress()).isNotNull();

		PersonSummary.AddressSummary address = person.getAddress();
		assertThat(address.getCity()).isEqualTo(CITY);

	}

	@Test
	void loadNamesOnlyDtoProjection(@Autowired ProjectionPersonRepository repository) {
		Collection<NamesOnlyDto> people = repository.findByFirstNameAndLastName(FIRST_NAME, LAST_NAME);
		assertThat(people).hasSize(1);

		NamesOnlyDto person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

	}

	@Test
	void findDynamicProjectionForNamesOnly(@Autowired ProjectionPersonRepository repository) {
		Collection<NamesOnly> people = repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnly.class);
		assertThat(people).hasSize(1);

		NamesOnly person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

		String expectedFullName = FIRST_NAME + " " + LAST_NAME;
		assertThat(person.getFullName()).isEqualTo(expectedFullName);

	}

	@Test
	void findDynamicProjectionForPersonSummary(@Autowired ProjectionPersonRepository repository) {
		Collection<PersonSummary> people = repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME,
				PersonSummary.class);
		assertThat(people).hasSize(1);

		PersonSummary person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);
		assertThat(person.getAddress()).isNotNull();

		PersonSummary.AddressSummary address = person.getAddress();
		assertThat(address.getCity()).isEqualTo(CITY);

	}

	@Test
	void findDynamicProjectionForNamesOnlyDto(@Autowired ProjectionPersonRepository repository) {
		Collection<NamesOnlyDto> people = repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnlyDto.class);
		assertThat(people).hasSize(1);

		NamesOnlyDto person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

	}

	@Test // GH-2139
	void projectionsShouldBePaginatable(@Autowired ProjectionPersonRepository repository) {

		Page<NamesOnly> people = repository.findAllProjectedBy(PageRequest.of(1, 1, Sort.by("firstName").descending()));
		assertThat(people.hasPrevious()).isTrue();
		assertThat(people.hasNext()).isFalse();
		assertThat(people).hasSize(1);
		assertThat(people).extracting(NamesOnly::getFullName).containsExactly(FIRST_NAME + " " + LAST_NAME);
	}

	@Test // GH-2139
	void projectionsShouldBeSliceable(@Autowired ProjectionPersonRepository repository) {

		Slice<NamesOnly> people = repository.findSliceProjectedBy(PageRequest.of(1, 1, Sort.by("firstName").descending()));
		assertThat(people.hasPrevious()).isTrue();
		assertThat(people.hasNext()).isFalse();
		assertThat(people).hasSize(1);
		assertThat(people).extracting(NamesOnly::getFullName).containsExactly(FIRST_NAME + " " + LAST_NAME);
	}

	@Test // GH-2164
	void findByIdWithProjectionShouldWork(@Autowired TreestructureRepository repository) {

		Optional<SimpleProjection> optionalProjection = repository
				.findById(projectionTestRootId, SimpleProjection.class);
		assertThat(optionalProjection).map(SimpleProjection::getName).hasValue("root");
	}

	@Test // GH-2165
	void relationshipsShouldBeIncludedInProjections(@Autowired TreestructureRepository repository) {

		Optional<SimpleProjectionWithLevelAndLower> optionalProjection = repository
				.findById(projectionTestRootId, SimpleProjectionWithLevelAndLower.class);
		assertThat(optionalProjection).hasValueSatisfying(p -> {

			assertThat(p.getName()).isEqualTo("root");
			assertThat(p.getOneOone()).extracting(ProjectionTest1O1::getName).isEqualTo("1o1");
			assertThat(p.getLevel1()).hasSize(2);
			assertThat(p.getLevel1().stream()).anyMatch(e -> e.getId().equals(projectionTestLevel1Id) && e.getLevel2().size() == 2);
		});
	}

	@Test // GH-2165
	void nested1to1ProjectionsShouldWork(@Autowired TreestructureRepository repository) {

		Optional<ProjectedOneToOne> optionalProjection = repository
				.findById(projectionTestRootId, ProjectedOneToOne.class);
		assertThat(optionalProjection).hasValueSatisfying(p -> {

			assertThat(p.getName()).isEqualTo("root");
			assertThat(p.getOneOone()).extracting(ProjectedOneToOne.Subprojection::getFullName)
					.isEqualTo(projectionTest1O1Id + " 1o1");
		});
	}

	@Test
	void nested1to1ProjectionsWithNestedProjectionShouldWork(@Autowired TreestructureRepository repository) {

		Optional<ProjectionWithNestedProjection> optionalProjection = repository
				.findById(projectionTestRootId, ProjectionWithNestedProjection.class);
		assertThat(optionalProjection).hasValueSatisfying(p -> {

			assertThat(p.getName()).isEqualTo("root");
			assertThat(p.getLevel1()).extracting("name").containsExactlyInAnyOrder("level11", "level12");
			assertThat(p.getLevel1()).flatExtracting("level2").extracting("name")
					.containsExactlyInAnyOrder("level21", "level22", "level23");
		});
	}

	@Test // GH-2165
	void nested1toManyProjectionsShouldWork(@Autowired TreestructureRepository repository) {

		Optional<ProjectedOneToMany> optionalProjection = repository
				.findById(projectionTestRootId, ProjectedOneToMany.class);
		assertThat(optionalProjection).hasValueSatisfying(p -> {

			assertThat(p.getName()).isEqualTo("root");
			assertThat(p.getLevel1()).hasSize(2);
		});
	}

	@Test // GH-2164
	void findByIdInDerivedFinderMethodInRelatedObjectShouldWork(@Autowired TreestructureRepository repository) {

		Optional<ProjectionTestRoot> optionalProjection = repository.findOneByLevel1Id(projectionTestLevel1Id);
		assertThat(optionalProjection).map(ProjectionTestRoot::getName).hasValue("root");
	}

	@Test // GH-2164
	void findByIdInDerivedFinderMethodInRelatedObjectWithProjectionShouldWork(
			@Autowired TreestructureRepository repository) {

		Optional<SimpleProjection> optionalProjection = repository.findOneByLevel1Id(projectionTestLevel1Id, SimpleProjection.class);
		assertThat(optionalProjection).map(SimpleProjection::getName).hasValue("root");
	}

	@Test
	void findStringBasedClosedProjection(@Autowired ProjectionPersonRepository repository) {

		PersonSummary personSummary = repository.customQueryByFirstName(FIRST_NAME);
		assertThat(personSummary).isNotNull();
		assertThat(personSummary.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(personSummary.getLastName()).isEqualTo(LAST_NAME);
	}

	@Test
	void findCypherDSLClosedProjection(@Autowired ProjectionPersonRepository repository) {

		Optional<PersonSummary> personSummary = repository.findOne(whoHasFirstName(FIRST_NAME), PersonSummary.class);
		assertThat(personSummary).isPresent();
		assertThat(personSummary.get().getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(personSummary.get().getLastName()).isEqualTo(LAST_NAME);
	}

	@Test // GH-2349
	void projectionsContainingKnownEntitiesShouldWorkFromRepository(@Autowired PersonRepository personRepository) {

		List<PersonDepartmentQueryResult> results = personRepository.findPersonWithDepartment();
		assertThat(results)
				.hasSize(1)
				.first()
				.satisfies(ProjectionIT::projectedEntities);
	}

	@Test // GH-2349
	void projectionsContainingKnownEntitiesShouldWorkFromTemplate(@Autowired Neo4jTemplate template) {

		List<PersonDepartmentQueryResult> results = template.find(PersonEntity.class).as(PersonDepartmentQueryResult.class)
				.matching("MATCH (person:PersonEntity)-[:MEMBER_OF]->(department:DepartmentEntity) RETURN person, department")
				.all();
		assertThat(results)
				.hasSize(1)
				.first()
				.satisfies(ProjectionIT::projectedEntities);
	}

	private static void projectedEntities(PersonDepartmentQueryResult personAndDepartment) {
		assertThat(personAndDepartment.getPerson()).extracting(PersonEntity::getId).isEqualTo("p1");
		assertThat(personAndDepartment.getPerson()).extracting(PersonEntity::getEmail).isEqualTo("p1@dep1.org");
		assertThat(personAndDepartment.getDepartment()).extracting(DepartmentEntity::getId).isEqualTo("d1");
		assertThat(personAndDepartment.getDepartment()).extracting(DepartmentEntity::getName).isEqualTo("Dep1");
	}

	private static Statement whoHasFirstName(String firstName) {
		Node p = Cypher.node("Person").named("p");
		return Cypher.match(p)
				.where(p.property("firstName").isEqualTo(Cypher.anonParameter(firstName)))
				.returning(
						p.getRequiredSymbolicName()
				)
				.build();
	}

	interface ProjectionPersonRepository extends Neo4jRepository<Person, Long>, CypherdslStatementExecutor<Person>  {

		Collection<NamesOnly> findByLastName(String lastName);

		Collection<NamesWithSpELCity> findProjectionByLastName(String lastName);

		Page<NamesOnly> findAllProjectedBy(Pageable pageable);

		Slice<NamesOnly> findSliceProjectedBy(Pageable pageable);

		Collection<PersonSummary> findByFirstName(String firstName);

		@Query("MATCH (n:Person) where n.firstName = $firstName return n")
		PersonSummary customQueryByFirstName(@Param("firstName") String firstName);

		Collection<NamesOnlyDto> findByFirstNameAndLastName(String firstName, String lastName);

		<T> Collection<T> findByLastNameAndFirstName(String lastName, String firstName, Class<T> projectionClass);
	}

	interface TreestructureRepository extends Neo4jRepository<ProjectionTestRoot, Long> {

		<T> Optional<T> findById(Long id, Class<T> typeOfProjection);

		Optional<ProjectionTestRoot> findOneByLevel1Id(Long idOfLevel1);

		<T> Optional<T> findOneByLevel1Id(Long idOfLevel1, Class<T> typeOfProjection);
	}

	interface SimpleProjection {

		String getName();
	}

	interface SimpleProjectionWithLevelAndLower {

		String getName();

		ProjectionTest1O1 getOneOone();

		List<ProjectionTestLevel1> getLevel1();
	}

	interface ProjectedOneToOne {

		String getName();

		Subprojection getOneOone();

		interface Subprojection {

			/**
			 * @return Some arbitrary computed projection result to make sure that machinery works as well
			 */
			@Value("#{target.id + ' ' + target.name}")
			String getFullName();
		}
	}

	interface ProjectedOneToMany {

		String getName();

		List<Subprojection> getLevel1();

		interface Subprojection {

			/**
			 * @return Some arbitrary computed projection result to make sure that machinery works as well
			 */
			@Value("#{target.id + ' ' + target.name}")
			String getFullName();
		}
	}

	interface ProjectionWithNestedProjection {

		String getName();

		List<Subprojection1> getLevel1();

		interface Subprojection1 {
			String getName();
			List<Subprojection2> getLevel2();
		}

		interface Subprojection2 {
			String getName();
		}
	}

	interface PersonRepository extends Neo4jRepository<PersonEntity, String> {
		@Query("MATCH (person:PersonEntity)-[:MEMBER_OF]->(department:DepartmentEntity) RETURN person, department")
		List<PersonDepartmentQueryResult> findPersonWithDepartment();
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(DepartmentEntity.class.getPackage().getName());
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
