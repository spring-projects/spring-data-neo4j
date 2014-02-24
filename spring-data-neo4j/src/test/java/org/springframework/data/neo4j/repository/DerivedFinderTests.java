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
package org.springframework.data.neo4j.repository;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;
import static org.neo4j.helpers.collection.IteratorUtil.single;
import static org.springframework.data.neo4j.SetHelper.asSet;

@NodeEntity
class Ingredient {
    @GraphId
    Long id;

    String name;

    Ingredient() {
    }

    Ingredient(String name) {
        this.name = name;
    }
}
@NodeEntity
class Dish {
    @GraphId
    Long id;

    @Indexed(unique = true,numeric = true) int number;

    Dish() {
    }

    Dish(int number) {
        this.number = number;
    }
}

@NodeEntity
class CookBook {
    @GraphId
    Long id;

    String title;

    CookBook() {
    }

    CookBook(String title) {
        this.title = title;
    }
}

@NodeEntity
class Recipe {
    @GraphId
    Long id;

    String author, title;

    @Fetch
    Ingredient ingredient;

    CookBook cookBook;

    @RelatedTo(direction = INCOMING)
    @Fetch
    Ingredient secret;

    @RelatedTo(direction = INCOMING, type ="secret")
    Set<Ingredient> secrets;

    Recipe() {
    }

    Recipe(String author, String title, Ingredient ingredient, Ingredient secret, CookBook cookBook) {
        this.author = author;
        this.title = title;
        this.ingredient = ingredient;
        this.secret = secret;
        this.cookBook = cookBook;
    }
}

interface DishRepository extends GraphRepository<Dish> {
    Dish findByNumber(int number);
}

interface RecipeRepository extends GraphRepository<Recipe> {
    Set<Recipe> findById(long id);

    Set<Recipe> findByIngredientId(long id);

    Set<Recipe> findByAuthor(String author);

    Set<Recipe> findByIngredient(Ingredient ingredient);

    Iterable<Recipe> findByIngredient(Ingredient ingredient, Sort sort);

    Iterable<Recipe> findByIngredientOrderByAuthorAsc(Ingredient ingredient);

    Iterable<Recipe> findByIngredientOrderByAuthorDesc(Ingredient ingredient);

    Set<Recipe> findBySecret(Ingredient ingredient);

    Set<Recipe> findByIngredientAndAuthor(Ingredient ingredient, String author);

    Set<Recipe> findByAuthorAndTitle(String author, String title);

    Set<Recipe> findByIngredientAndCookBookTitle(Ingredient ingredient, String cookBookTitle);

    Set<Recipe> findByIngredientAndCookBook(Ingredient ingredient, CookBook cookBook);
    Set<Recipe> findBySecrets(Ingredient ingredient);

    Long countByAuthor(String author);

    Long countByIngredientAndAuthor(Ingredient ingredient, String author);

    Long countByIngredient(Ingredient ingredient);

    Integer countByCookBookTitle(String title);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DerivedFinderTests {

    private Dish dish;
    private Transaction transaction;

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        TestConfig() throws ClassNotFoundException {
            setBasePackage("org.springframework.data.neo4j.repository","org.springframework.data.neo4j.model");
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }

    }

    @Autowired
    private GraphDatabaseService graphDatabaseService;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private DishRepository dishRepository;

    private Ingredient fish, spice, oliveOil, pear, chocolate;

    private CookBook nakedChef, baking101;

    private Recipe focaccia, chocolateFudgeCake, whiteChocolateSquares;

    @Before
    public void setUp() throws Exception {
        Neo4jHelper.cleanDb(graphDatabaseService, false);

        CRUDRepository<Ingredient> ingredientRepository = template.repositoryFor(Ingredient.class);

        Transaction tx = graphDatabaseService.beginTx();
        try {
            chocolate = ingredientRepository.save(new Ingredient("chocolate"));
            fish = ingredientRepository.save(new Ingredient("fish"));
            spice = ingredientRepository.save(new Ingredient("spice x"));
            oliveOil = ingredientRepository.save(new Ingredient("olive oil"));
            pear = ingredientRepository.save(new Ingredient("pear"));
            nakedChef = template.repositoryFor(CookBook.class).save(new CookBook("Naked Chef"));
            baking101 = template.repositoryFor(CookBook.class).save(new CookBook("baking101"));
            recipeRepository.save(new Recipe("Hugh", "Bouillabaisse", fish, null, null));
            recipeRepository.save(new Recipe("Hugh", "pear frangipane", pear, null, baking101));
            recipeRepository.save(new Recipe("The Colonel", "fried chicken", null, spice, null));
            recipeRepository.save(new Recipe("Jamie", "pesto", oliveOil, null, nakedChef));
            focaccia = recipeRepository.save(new Recipe("Hugh", "focaccia", oliveOil, null, baking101));

            chocolateFudgeCake = recipeRepository.save(new Recipe("Nigella", "Chocolate Fudge cake", chocolate, null, null));
            whiteChocolateSquares = recipeRepository.save(new Recipe("Heston", "White Chocolate squares", chocolate, null, null));

            dish = dishRepository.save(new Dish(100));
            tx.success();
        } finally {
            tx.close();
        }
        tx = graphDatabaseService.beginTx();
        try {
            for (Node node : graphDatabaseService.getAllNodes()) {
                System.out.println("node = " + node);
            }
            tx.success();
        } finally {
            tx.close();
        }

        transaction = graphDatabaseService.beginTx();
    }

    @After
    public void tearDown() throws Exception {
        if (transaction!=null) {
            transaction.success();
            transaction.close();
            transaction = null;
        }
    }

    @Test
    public void shouldFindUsingSingleProperty() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByAuthor("Hugh");

        assertThat(getIngredientNames(recipes), is(equalTo(asSet("fish", "pear", "olive oil"))));
    }

    private Set<String> getIngredientNames(Set<Recipe> recipes) {
        HashSet<String> ingredientNames = new HashSet<String>();

        for (Recipe recipe : recipes) {
            ingredientNames.add(recipe.ingredient.name);
        }

        return ingredientNames;
    }

    @Test
    public void shouldFindUsingMultipleProperties() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByAuthorAndTitle("Hugh", "pear frangipane");

        assertThat(single(recipes).ingredient.name, is(equalTo("pear")));
    }

    @Test
    public void shouldFindUsingEntity() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByIngredient(fish);

        assertThat(single(recipes).author, is(equalTo("Hugh")));
    }

    @Test
    public void shouldFindUsingEntityId() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByIngredientId(fish.id);

        assertThat(single(recipes).author, is(equalTo("Hugh")));
    }

    @Test
    public void shouldFindUsingOwnGraphId() throws Exception {
        Set<Recipe> recipes = recipeRepository.findById(focaccia.id);

        assertThat(single(recipes).id, is(equalTo(focaccia.id)));
    }

    @Test
    public void shouldFindUsingMultipleEntities() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByIngredientAndCookBook(pear, baking101);

        assertThat(single(recipes).title, is(equalTo("pear frangipane")));
    }

    @Test
    public void shouldFindByRelationshipPropertyEntities() throws Exception {
        Set<Recipe> recipes = recipeRepository.findBySecrets(spice);

        assertThat(single(recipes).author, is(equalTo("The Colonel")));
    }

    @Test
    public void shouldFindUsingEntityWithIncomingRelationship() throws Exception {
        Set<Recipe> recipes = recipeRepository.findBySecret(spice);

        assertThat(single(recipes).author, is(equalTo("The Colonel")));
    }

    @Test
    public void shouldFindUsingEntityAndProperty() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByIngredientAndAuthor(oliveOil, "Jamie");

        assertThat(single(recipes).author, is(equalTo("Jamie")));
    }

    @Test
    public void shouldCountCorrectlyUsingProperty() throws Exception {
        Long actualCountVal = recipeRepository.countByAuthor("Hugh");
        assertThat(actualCountVal, is(equalTo(3L)));
    }

    @Test
    public void shouldCountCorrectlyUsingEntityAndProperty() throws Exception {
        Long actualCountVal = recipeRepository.countByIngredientAndAuthor(oliveOil,"Hugh");
        assertThat(actualCountVal, is(equalTo(1L)));
    }

    @Test
    public void shouldCountCorrectlyUsingEntity() throws Exception {
        Long actualCountVal = recipeRepository.countByIngredient(chocolate);
        assertThat(actualCountVal, is(equalTo(2L)));
    }

    @Test
    public void shouldCountCorrectlyUsingPropertyTraversal() throws Exception {
        Integer actualCountVal = recipeRepository.countByCookBookTitle("Naked Chef");
        assertThat(actualCountVal, is(equalTo(1)));
    }


    @Test
    public void shouldFindUsingEntityAndPropertyTraversal() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByIngredientAndCookBookTitle(oliveOil, "Naked Chef");

        for (Node node : graphDatabaseService.getAllNodes()) {
            System.out.println("in test = " + node);
        }

        assertThat(single(recipes).title, is(equalTo("pesto")));
    }
    @Test
    public void shouldFindUsingIndexedNumericValue() throws Exception {
        Dish foundDish = dishRepository.findByNumber(100);

        assertThat(foundDish.number, is(equalTo(dish.number)));
    }

    @Test
    public void shouldFindCorrectlyOrderedUsingMethodSignatureAscending() {
        Iterable<Recipe> recipes = recipeRepository.findByIngredientOrderByAuthorAsc(chocolate);
        List<Recipe> recs = (List<Recipe>)asCollection(recipes);
        assertThat( recs.size(), equalTo(2));
        assertEquals("Heston" , recs.get(0).author);
        assertEquals("Nigella" , recs.get(1).author);
    }

    @Test
    public void shouldFindCorrectlyOrderedUsingMethodSignatureDescending() {
        Iterable<Recipe> recipes = recipeRepository.findByIngredientOrderByAuthorDesc(chocolate);
        List<Recipe> recs = (List<Recipe>)asCollection(recipes);
        assertThat( recs.size(), equalTo(2));
        assertEquals("Nigella" , recs.get(0).author);
        assertEquals("Heston" , recs.get(1).author);
    }

    @Test
    public void shouldFindCorrectlyOrderedUsingSortParam() {
        Sort.Order order = new Sort.Order(Sort.Direction.DESC,"author");
        Sort sort = new Sort(order);
        Iterable<Recipe> recipes = recipeRepository.findByIngredient(chocolate, sort);
        List<Recipe> recs = (List<Recipe>)asCollection(recipes);
        assertThat( recs.size(), equalTo(2));
        assertEquals("Nigella" , recs.get(0).author);
        assertEquals("Heston" , recs.get(1).author);
    }
}
