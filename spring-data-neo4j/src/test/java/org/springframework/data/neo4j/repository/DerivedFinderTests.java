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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.neo4j.graphdb.Direction.INCOMING;
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

interface RecipeRepository extends GraphRepository<Recipe> {
    Set<Recipe> findById(long id);

    Set<Recipe> findByIngredientId(long id);

    Set<Recipe> findByAuthor(String author);

    Set<Recipe> findByIngredient(Ingredient ingredient);

    Set<Recipe> findBySecret(Ingredient ingredient);

    Set<Recipe> findByIngredientAndAuthor(Ingredient ingredient, String author);

    Set<Recipe> findByAuthorAndTitle(String author, String title);

    Set<Recipe> findByIngredientAndCookBookTitle(Ingredient ingredient, String cookBookTitle);

    Set<Recipe> findByIngredientAndCookBook(Ingredient ingredient, CookBook cookBook);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DerivedFinderTests {

    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new ImpermanentGraphDatabase();
        }

    }

    @Autowired
    private GraphDatabaseService graphDatabaseService;

    @Autowired
    private Neo4jTemplate template;

    @Autowired
    private RecipeRepository recipeRepository;

    private Ingredient fish, spice, oliveOil, pear;

    private CookBook nakedChef, baking101;

    private Recipe focaccia;

    @Before
    public void setUp() throws Exception {
        Neo4jHelper.cleanDb(graphDatabaseService, true);

        CRUDRepository<Ingredient> ingredientRepository = template.repositoryFor(Ingredient.class);

        Transaction transaction = graphDatabaseService.beginTx();
        try {
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
            transaction.success();
        } finally {
            transaction.finish();
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
    public void shouldFindUsingEntityAndPropertyTraversal() throws Exception {
        Set<Recipe> recipes = recipeRepository.findByIngredientAndCookBookTitle(oliveOil, "Naked Chef");

        assertThat(single(recipes).title, is(equalTo("pesto")));
    }
}
