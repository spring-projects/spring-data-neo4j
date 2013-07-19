/**
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.neo4j.annotation.relatedtovia;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.neo4j.helpers.collection.IteratorUtil.first;
import static org.springframework.data.neo4j.SetHelper.asSet;
import static org.springframework.data.neo4j.annotation.RelationshipDelegates.getRelationshipNames;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-via-test-context.xml"})
@Transactional
public class RelatedToViaTests {
    public static final String DISTRICT_LINE = "District Line";
    public static final String CENTRAL_LINE = "Central Line";

    @Autowired
    private UndergroundRepository tfl;

    @Autowired
    private CountryRepository countries;

    @Autowired
    private SuperStateRepository dystopia;

    @Autowired
    private MetroLineRepository metroLines;

    @Autowired
    private CityRepository cities;

    @Autowired
    private PlayerRepository players;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private Neo4jTemplate template;

    @Before
    public void before() {
        tfl.deleteAll();
        countries.deleteAll();
        dystopia.deleteAll();
        metroLines.deleteAll();
        cities.deleteAll();
        players.deleteAll();
        teams.deleteAll();
    }

    @Test
    public void shouldMapRelationshipAsFirstClassCitizen() throws Exception {
        TubeStation westHam = tfl.save(new TubeStation("West Ham"));
        TubeStation stratford = tfl.save(new TubeStation("Stratford"));
        TubeStation mileEnd = new TubeStation("Mile End");
        mileEnd.connectsTo(westHam, DISTRICT_LINE);
        mileEnd.connectsTo(stratford, CENTRAL_LINE);

        tfl.save(mileEnd);

        assertThat(first(template.getNode(mileEnd.getId()).getRelationships()).getType().name(), is("route"));
        mileEnd = tfl.findOne(mileEnd.getId());
        Line line = first(mileEnd.getLines());
        assertThat(mileEnd, is(equalTo(line.getOrigin())));
        assertThat(asList(DISTRICT_LINE, CENTRAL_LINE), hasItem(line.getName()));
        assertThat(asList(westHam, stratford), hasItem(line.getDestination()));
    }

    @Test
    public void shouldGivePrecedenceToAnnotationProvidedRelationshipTypeOverDefault() throws Exception {
        Country france = countries.save(new Country("République française"));
        Country usa = countries.save(new Country("United States of America"));
        Country uk = new Country("United Kingdom of Great Britain and Northern Ireland");
        uk.hasCordialRelationsWith(france, "Entente Cordiale");
        uk.hasSpecialRelationsWith(usa);

        countries.save(uk);

        assertThat(getRelationshipNames(template, uk), is(equalTo(asSet("cordial", "special"))));
    }

    @Test
    public void shouldGivePrecedenceToAnnotationProvidedRelationshipTypeOverDefaultForCollections() throws Exception {
        MetroLine m1 = metroLines.save(new MetroLine("M1 - Vanløse to Vestamager"));
        MetroLine m2 = metroLines.save(new MetroLine("M2 - Vanløse to Lufthavnen"));
        MetroLine m3 = metroLines.save(new MetroLine("M3 - Cityringen"));
        MetroLine m4 = metroLines.save(new MetroLine("M4 - Nørrebro to København H"));
        City copenhagen = new City("Copenhagen");
        copenhagen.has(m1, m2);
        copenhagen.plans(m3, m4);

        cities.save(copenhagen);

        assertThat(getRelationshipNames(template, copenhagen), is(equalTo(asSet("existing", "planned"))));
        assertThat(copenhagen.getCurrentMetroLines(), is(equalTo(asSet(m1, m2))));
        assertThat(copenhagen.getFutureMetroLines(), is(equalTo(asSet(m3, m4))));
    }

    @Test
    public void shouldGivePrecedenceToDynamicRelationshipTypeOverAnnotationProvidedRelationshipType() throws Exception {
        SuperState eastasia = dystopia.save(new SuperState("Eastasia"));
        SuperState eurasia = dystopia.save(new SuperState("Eurasia"));
        SuperState oceania = new SuperState("Oceania");
        oceania.isAlliedWith(eastasia);

        dystopia.save(oceania);

        assertThat(getRelationshipNames(template, oceania), is(equalTo(asSet("alliedWith"))));

        oceania = dystopia.findOne(oceania.getId());
        oceania.isAtWarWith(eurasia);

        dystopia.save(oceania);

        assertThat(getRelationshipNames(template, oceania), is(equalTo(asSet("atWarWith"))));
    }

    @Test
    public void shouldGivePrecedenceToDynamicRelationshipTypeOverAnnotationProvidedRelationshipTypeForCollections()
            throws Exception {
        Player jordan = players.save(new Player("Michael Jordan"));
        Player pippen = players.save(new Player("Scottie Pippen"));
        Team bulls = new Team("Chicago Bulls");
        bulls.add(new PlayerStatus(bulls, jordan));
        bulls.add(new PlayerStatus(bulls, pippen, "substitute"));

        teams.save(bulls);

        assertThat(getRelationshipNames(template, bulls), is(equalTo(asSet("starter", "substitute"))));
    }

    @Test
    public void shouldValidateEndNode() throws Exception {
        TubeStation mileEnd = new TubeStation("East Ham");
        mileEnd.connectsTo(null, DISTRICT_LINE);

        try {
            tfl.save(mileEnd);

            fail();
        } catch (InvalidDataAccessApiUsageException e) {
            assertThat(e.getCause().getMessage(), is(equalTo("End node must not be null (" + Line.class.getName() + ")")));
        }
    }
}
