package org.springframework.data.neo4j.fieldaccess;

import javax.validation.ValidationException;
import javax.validation.Validator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.PersonRepository;
import org.springframework.data.neo4j.model.FriendshipRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Test validation on node and relationship properties.
 * 
 * @author Timmy Storms
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ValidatingPropertyFieldAccessorListenerFactoryTest {

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = PersonRepository.class, considerNestedRepositories = true)
	static class Config extends Neo4jConfiguration {
        Config() throws ClassNotFoundException {
            setBasePackage(Person.class.getPackage().getName());
        }

        @Bean
		public GraphDatabaseService graphDatabaseService() {
			return new TestGraphDatabaseFactory().newImpermanentDatabase();
		}
        
        @Bean
        public Validator validator() {
            return new LocalValidatorFactoryBean(); 
        }
        
	}
	
	@Autowired
	private PersonRepository personRepo;
	
	@Autowired
	private FriendshipRepository friendshipRepo;
	
	@Test(expected=ValidationException.class)
	public void testNodePropertyValidation() {
	    personRepo.save(new Person("Li", 102));
	}
	
	@Test(expected=ValidationException.class)
    public void testRelationshipPropertyValidation() {
        final Person john = personRepo.save(new Person("John", 50));
        final Person jack = personRepo.save(new Person("Jack", 45));
        final Friendship friendship = john.knows(jack);
        friendship.setYears(100);
        friendshipRepo.save(friendship);
    }

}
