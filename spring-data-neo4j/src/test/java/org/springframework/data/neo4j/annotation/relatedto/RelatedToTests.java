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
package org.springframework.data.neo4j.annotation.relatedto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.either;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.neo4j.helpers.collection.IteratorUtil.single;
import static org.springframework.data.neo4j.annotation.RelationshipDelegates.getNumberOfRelationships;
import static org.springframework.data.neo4j.annotation.RelationshipDelegates.getRelationshipNames;
import static org.springframework.data.neo4j.annotation.SetHelper.asSet;
import static org.springframework.data.neo4j.annotation.relatedto.Mondrian.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:related-to-test-context.xml"})
@Transactional
public class RelatedToTests {
    @Autowired
    private BookRepository books;

    @Autowired
    private TownRepository towns;

    @Autowired
    private WarchiefRepository warchiefs;

    @Autowired
    private AdvisorRepository advisors;

    @Autowired
    private MarvelCharacterRepository marvelCharacters;

    @Autowired
    private ActivityRepository activities;

    @Autowired
    private AnimalRepository animals;

    @Autowired
    private ZooRepository zsl;

    @Autowired
    private ShopRepository shops;

    @Autowired
    private RestaurantRepository restaurants;

    @Autowired
    private ShoppingCenterRepository shoppingCenters;

    @Autowired
    private MainHandWeaponRepository mainHandWeapons;

    @Autowired
    private OffHandWeaponRepository offHandWeapons;
    @Autowired
    private WorldOfWarcraftCharacterRepository worldOfWarcraftCharacters;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private StudentRepository students;

    @Autowired
    private ExperienceRepository experiences;

    @Autowired
    private JobRepository jobs;

    @Autowired
    private CareerProfileRepository linkedin;

    @Autowired
    private DripBrewRepository dripBrews;

    @Autowired
    private EspressoBasedCoffeeRepository americanos;

    @Autowired
    private CoffeeMachineRepository coffeeMachines;

    @Autowired
    private AppleRepository apples;

    @Autowired
    private FruitBowlRepository fruitbowls;

    @Autowired
    private MacBookRepository macBookRepository;

    @Autowired
    private RetinaMacBookRepository retinaMacBooks;

    @Autowired
    private AppleStoreRepository appleStores;

    @Autowired
    private FriendRepository friends;

    @Autowired
    private ChairmanRepository chairmen;

    @Autowired
    private BoardMemberRepository boardMembers;

    @Autowired
    private OrganisationRepository organisations;

    @Autowired
    private SquareRepository squares;

    @Autowired
    private RectangleRepository rectangles;

    @Autowired
    private MondrianRepository mondrians;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private HumanRepository humans;

    @Autowired
    private SuperHumanRepository superHumans;

    @Autowired
    private SuperheroRepository superheroes;

    @Autowired
    private HeroRepository heroes;

    @Autowired
    private InitiativeRepository initiatives;

    @Autowired
    private Neo4jTemplate template;

    @BeforeTransaction
    public void before() {
        Neo4jHelper.cleanDb(template);
    }

    @Test
    public void shouldMapRelationshipEvenWithoutAnnotation() throws Exception {
        Book theHobbit = books.save(new Book("The Hobbit"));
        Book theTwoTowers = books.save(new Book("The Two Towers"));
        Book theReturnOfTheKing = books.save(new Book("The Return of the King"));
        Book theFellowship = new Book("The Fellowship of the Ring");
        theFellowship.follows(theHobbit);
        theFellowship.hasTrilogyPeers(theTwoTowers, theReturnOfTheKing);

        books.save(theFellowship);

        assertThat(getRelationshipNames(template, theFellowship), is(asSet("prequel", "peers")));
        theFellowship = books.findOne(theFellowship.getId());
        assertThat(theFellowship.getPrequel(), is(equalTo(theHobbit)));
        assertThat(theFellowship.getTrilogyPeers(), is(equalTo(asSet(theTwoTowers, theReturnOfTheKing))));
    }

    @Test
    public void shouldRelateNodesUsingFieldNameAsRelationshipType() throws Exception {
        Town malmo = towns.save(new Town("Malmö"));
        Town newcastle = new Town("Newcastle");
        newcastle.isTwinnedWith(malmo);
        Town gateshead = towns.save(new Town("Gateshead"));
        Town sunderland = towns.save(new Town("Sunderland"));
        newcastle.hasNeighbours(gateshead, sunderland);

        towns.save(newcastle);

        assertThat(getRelationshipNames(template, newcastle), is(asSet("twin", "neighbours")));
        newcastle = towns.findOne(newcastle.getId());
        assertThat(newcastle.getTwin(), is(equalTo(malmo)));
        assertThat(newcastle.getNeighbours(), is(equalTo(asSet(gateshead, sunderland))));
    }

    @Test
    public void shouldRelateNodesUsingAnnotationProvidedRelationshipType() throws Exception {
        Warchief orgrim = warchiefs.save(new Warchief("Orgrim Doomhammer"));
        Warchief thrall = new Warchief("Thrall, Son of Durotan");
        thrall.succeeds(orgrim);
        Advisor garrosh = advisors.save(new Advisor("Garrosh Hellscream"));
        Advisor rehgar = advisors.save(new Advisor("Rehgar Earthfury"));
        thrall.hasAdvisors(garrosh, rehgar);

        warchiefs.save(thrall);

        assertThat(getRelationshipNames(template, thrall), is(asSet("succeeds", "is_advised_by")));
        thrall = warchiefs.findOne(thrall.getId());
        assertThat(thrall.getMentor(), is(equalTo(orgrim)));
        assertThat(thrall.getAdvisors(), is(equalTo(asSet(garrosh, rehgar))));
    }

    /**
     * stacked entity cache means we do not load the node back polymorphically - should we?
     */
    @Test
    public void shouldNotDifferentiateByEndNodeClassWhenTargetTypeNotEnforced() throws Exception {
        DripBrew filteredCoffee = dripBrews.save(new DripBrew("Filtered Coffee"));
        EspressoBasedCoffee americano = americanos.save(new EspressoBasedCoffee("Americano"));
        CoffeeMachine coffeeMachine = new CoffeeMachine();
        coffeeMachine.produces(filteredCoffee);
        coffeeMachine.produces(americano);

        try {
            coffeeMachines.save(coffeeMachine);

            fail();
        } catch (ConverterNotFoundException e) {
            assertThat(asSet(DripBrew.class.getName(), EspressoBasedCoffee.class.getName()), hasItem(e.getSourceType().getName()));
            assertThat(asSet(DripBrew.class.getName(), EspressoBasedCoffee.class.getName()), hasItem(e.getTargetType().getName()));
            assertThat(e.getSourceType().getName(), is(not(equalTo(e.getTargetType().getName()))));
        }
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedBetweenFields() throws Exception {
        MarvelCharacter betty = marvelCharacters.save(new MarvelCharacter("Betty Ross"));
        Activity smashing = activities.save(new Activity("smashing"));
        MarvelCharacter hulk = new MarvelCharacter("Hulk");
        hulk.favours(betty);
        hulk.loves(smashing);

        marvelCharacters.save(hulk);

        assertThat(getRelationshipNames(template, hulk), is(equalTo(asSet("favourite"))));
        hulk = marvelCharacters.findOne(hulk.getId());
        assertThat(hulk.getFavourite(), is(equalTo(betty)));
        assertThat(hulk.getFavouriteActivity(), is(equalTo(smashing)));
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedBetweenCollections() throws Exception {
        Shop appleStore = shops.save(new Shop("Apple Store"));
        Shop hAndM = shops.save(new Shop("H&M"));
        Restaurant wahaca = restaurants.save(new Restaurant("Wahaca"));
        Restaurant busabaEathai = restaurants.save(new Restaurant("Busaba Eathai"));

        ShoppingCenter westfield = new ShoppingCenter("Westfield - Stratford City");
        westfield.houses(appleStore, hAndM);
        westfield.houses(wahaca, busabaEathai);
        shoppingCenters.save(westfield);

        assertThat(getRelationshipNames(template, westfield), is(equalTo(asSet("houses"))));
        westfield = shoppingCenters.findOne(westfield.getId());
        assertThat(westfield.getShops(), is(equalTo(asSet(appleStore, hAndM))));
        assertThat(westfield.getRestaurants(), is(equalTo(asSet(wahaca, busabaEathai))));
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedBetweenFieldAndCollection() throws Exception {
        Chairman jacquesRogge = chairmen.save(new Chairman("Jacques Rogge"));
        BoardMember boardMember1 = boardMembers.save(new BoardMember("Denis Oswald"));
        BoardMember boardMember2 = boardMembers.save(new BoardMember("René Fasel"));
        BoardMember boardMember3 = boardMembers.save(new BoardMember("Frank Fredericks"));

        Organisation ioc = new Organisation("THE INTERNATIONAL OLYMPIC COMMITTEE");
        ioc.setChairman(jacquesRogge);
        ioc.add(boardMember1, boardMember2, boardMember3);
        organisations.save(ioc);

        assertThat(getRelationshipNames(template, ioc), is(equalTo(asSet("executive"))));
        ioc = organisations.findOne(ioc.getId());
        assertThat(ioc.getChairman(), is(equalTo(jacquesRogge)));
        assertThat(ioc.getBoardMembers(), is(equalTo(asSet(boardMember1, boardMember2, boardMember3))));
    }

    /**
     * We cannot distinguish the relationships, so one overwrites the other. It is not even deterministic
     */
    @Test
    public void shouldNotDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedButEndNodeTypesIdenticalBetweenFields() throws Exception {
        Friend sam = friends.save(new Friend("Samwise Gamgee"));
        Friend pippin = friends.save(new Friend("Peregrin Took"));
        Friend frodo = new Friend("Frodo Baggins");
        frodo.isFriendsWith(pippin);
        frodo.isBestFriendsWith(sam);

        friends.save(frodo);

        assertThat(getNumberOfRelationships(template, frodo), is(equalTo(1)));
    }

    /**
     * "Interesting" semantics - but we have no way of disallowing it
     */
    @Test
    public void shouldNotDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedButEndNodeTypesIdenticalBetweenCollections() throws Exception {
        Course probabilityTheory = courses.save(new Course("Probability Theory"));
        Course geometry = courses.save(new Course("Geometry"));
        Course statistics = courses.save(new Course("Statistics"));
        Course calculus = courses.save(new Course("Calculus"));

        Student student = new Student("Ferris Bueller");
        student.likes(probabilityTheory, geometry);
        student.isBoredWith(statistics, calculus);
        students.save(student);

        assertThat(getRelationshipNames(template, student), is(equalTo(asSet("attends"))));
        student = students.findOne(student.getId());
        assertThat(student.getFavouriteCourses(), is(either(equalTo(asSet(probabilityTheory, geometry))).or(equalTo(asSet(statistics, calculus)))));
        assertThat(student.getDislikedCourses(), is(either(equalTo(asSet(probabilityTheory, geometry))).or(equalTo(asSet(statistics, calculus)))));
    }

    /**
     * So, because you let your relationship types clash, and
     * didn't want to distinguish by target type, you get either
     * only green apples or an exception
     */
    @Test
    public void shouldNotDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedButEndNodeTypesIdenticalBetweenFieldAndCollection() throws Exception {
        Apple redApple1 = apples.save(new Apple("red"));
        Apple redApple2 = apples.save(new Apple("red"));
        Apple greenApple = apples.save(new Apple("green"));

        FruitBowl fruitBowl = new FruitBowl();
        fruitBowl.addRedApples(redApple1, redApple2);
        fruitBowl.setGreenApple(greenApple);
        try {
            fruitbowls.save(fruitBowl);

            assertThat(getRelationshipNames(template, fruitBowl), is(equalTo(asSet("contains"))));

            fruitBowl = fruitbowls.findOne(fruitBowl.getId());
            assertThat(fruitBowl.getRedApples(), is(equalTo(asSet(greenApple))));
            assertThat(fruitBowl.getGreenApple(), is(equalTo(greenApple)));
        } catch (InvalidDataAccessApiUsageException e) {
            assertThat(e.getCause().getMessage(), is(equalTo("Cannot obtain single field value for field 'greenApple'")));
        }
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedOnFieldsSharingSuperType() throws Exception {
        MainHandWeapon mainHandWeapon = mainHandWeapons.save(new MainHandWeapon("Warglaive of Azzinoth"));
        OffHandWeapon offHandWeapon = offHandWeapons.save(new OffHandWeapon("Warglaive of Azzinoth"));
        WorldOfWarcraftCharacter illidanStormrage = new WorldOfWarcraftCharacter("Illidan Stormrage");
        illidanStormrage.wields(mainHandWeapon);
        illidanStormrage.wields(offHandWeapon);

        worldOfWarcraftCharacters.save(illidanStormrage);

        assertThat(getRelationshipNames(template, illidanStormrage), is(equalTo(asSet("wields"))));
        illidanStormrage = worldOfWarcraftCharacters.findOne(illidanStormrage.getId());
        assertThat(illidanStormrage.getWeapons(), is(equalTo((Iterable) asSet(mainHandWeapon, offHandWeapon))));
        assertThat(illidanStormrage.getMainHandWeapon(), is(equalTo(mainHandWeapon)));
        assertThat(illidanStormrage.getOffHandWeapon(), is(equalTo(offHandWeapon)));
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedOnCollectionsSharingSuperType() throws Exception {
        Herbivore deer = animals.save(new Herbivore("Deer"));
        Herbivore giraffe = animals.save(new Herbivore("Giraffe"));
        Carnivore lion = animals.save(new Carnivore("Lion"));
        Carnivore tiger = animals.save(new Carnivore("Tiger"));

        Zoo zsl = new Zoo("ZSL London Zoo");
        zsl.exhibits(deer);
        zsl.exhibits(giraffe);
        zsl.exhibits(lion);
        zsl.exhibits(tiger);
        this.zsl.save(zsl);

        assertThat(getRelationshipNames(template, zsl), is(equalTo(asSet("exhibit"))));
        zsl = this.zsl.findOne(zsl.getId());
        assertThat(zsl.getAllAnimals(), is(equalTo((Iterable) asSet(deer, giraffe, lion, tiger))));
        assertThat(zsl.getHerbivores(), is(equalTo(asSet(deer, giraffe))));
        assertThat(zsl.getCarnivores(), is(equalTo(asSet(lion, tiger))));
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedOnFieldAndCollectionSharingSuperType() throws Exception {
        Square redSquare = squares.save(new Square(RED));
        Rectangle yellowRectangle = rectangles.save(new Rectangle(YELLOW));
        Rectangle blueRectangle = rectangles.save(new Rectangle(BLUE));

        Mondrian mondrian = new Mondrian("Composition with Yellow, Blue and Red");
        mondrian.includes(redSquare);
        mondrian.includes(yellowRectangle, blueRectangle);
        mondrians.save(mondrian);

        assertThat(getRelationshipNames(template, mondrian), is(equalTo(asSet("includes"))));
        mondrian = mondrians.findOne(mondrian.getId());
        assertThat(mondrian.getQuadrilaterals(), is(equalTo((Iterable) asSet(redSquare, yellowRectangle, blueRectangle))));
        assertThat(mondrian.getSquare(), is(equalTo(redSquare)));
        assertThat(mondrian.getRectangles(), is(equalTo(asSet(yellowRectangle, blueRectangle))));
    }

    @Test
    public void shouldNotDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedButEndNodeTypeSubstitutableBetweenFields() throws Exception {
        MacBook macbook = macBookRepository.save(new MacBook());
        RetinaMacBook retinaMacbook = retinaMacBooks.save(new RetinaMacBook());
        AppleStore appleStore = new AppleStore();
        appleStore.suppliesMacBook(macbook);
        appleStore.suppliesRetinaMacBook(retinaMacbook);

        try {
            appleStores.save(appleStore);

            assertThat(getRelationshipNames(template, appleStore), is(equalTo(asSet("supplies"))));
            appleStore = appleStores.findOne(appleStore.getId());
            assertThat(single(appleStore.getSupplies()), is(instanceOf(MacBook.class)));
        } catch (InvalidDataAccessApiUsageException e) {
            assertThat(e.getCause().getMessage(), is(equalTo("Cannot obtain single field value for field 'macBook'")));
        }
    }

    @Test
    public void shouldNotDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedButEndNodeTypeSubstitutableBetweenCollections() {
        Experience college = experiences.save(new Experience("Reed College (calligraphy mainly)"));
        Experience sabbatical = experiences.save(new Experience("Neem Karoli ashram, found enlightenment"));
        Job atari = jobs.save(new Job("Technician at Atari"));
        Job apple = jobs.save(new Job("Co-founder, Chairman and CEO, Apple Inc."));
        CareerProfile steveJobs = new CareerProfile("Steven Paul Jobs");
        steveJobs.addExperience(college, sabbatical);
        steveJobs.addJobs(atari, apple);

        linkedin.save(steveJobs);

        assertThat(getRelationshipNames(template, steveJobs), is(equalTo(asSet("experience"))));
        steveJobs = linkedin.findOne(steveJobs.getId());
        assertThat(steveJobs.getExperience(), is(either(equalTo(asSet(college, sabbatical))).or(equalTo(asSet(college, sabbatical, atari, apple)))));
        assertThat(steveJobs.getJobs(), is(either(equalTo(Collections.<Job>emptySet())).or(equalTo(asSet(atari, apple)))));
    }

    @Test
    public void shouldDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedAndEndNodeTypeForFieldInheritsFromEndNodeTypeForCollection() throws Exception {
        Human sam = humans.save(new Human("Samantha Carter"));
        Human daniel = humans.save(new Human("Daniel Jackson"));
        Human tealc = humans.save(new Human("Tealc"));
        SuperHuman jack = superHumans.save(new SuperHuman("Jack O'Neill"));

        Team sg1 = new Team("SG1");
        sg1.setLeader(jack);
        sg1.add(sam, daniel, tealc);
        teams.save(sg1);

        assertThat(getRelationshipNames(template, sg1), is(equalTo(asSet("member"))));
        assertThat(sg1.getLeader(), is(equalTo(jack)));
        assertThat(sg1.getMembers(), is(equalTo(asSet(sam, daniel, tealc))));
    }

    @Test
    public void shouldNotDifferentiateClashingRelationshipTypesWhenTargetTypeEnforcedButEndNodeTypeForCollectionInheritsFromEndNodeTypeForField() throws Exception {
        Superhero thor = superheroes.save(new Superhero("Thor"));
        Superhero ironMan = superheroes.save(new Superhero("Iron Man"));
        Superhero hulk = superheroes.save(new Superhero("Hulk"));
        Hero nickFury = heroes.save(new Hero("Nick Fury"));

        Initiative avengers = new Initiative("Avengers");
        avengers.setLeader(nickFury);
        avengers.add(thor, ironMan, hulk);
        initiatives.save(avengers);

        assertThat(getRelationshipNames(template, avengers), is(equalTo(asSet("hero"))));
        avengers = initiatives.findOne(avengers.getId());
        assertThat(avengers.getLeader(), is(equalTo(nickFury)));
        assertThat(avengers.getMembers(), is(equalTo(asSet(thor, ironMan, hulk))));
    }
}
