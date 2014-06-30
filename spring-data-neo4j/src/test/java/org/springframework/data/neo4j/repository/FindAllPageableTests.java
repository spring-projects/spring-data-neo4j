package org.springframework.data.neo4j.repository;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.PersonRepository;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test suite to check ISSUE-177
 * 
 * @author lorenzo
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/repository/GraphRepositoryTests-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class FindAllPageableTests {

	@Autowired
    private Neo4jTemplate neo4jTemplate;
    
	@Autowired
    private PersonRepository personRepository;
    
	@BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }

    @Test
    @Transactional
    public void shouldPaginateCorrectly() {
    	for (int i = 0; i < 30; i++) {
    		String name = "person_" + StringUtils.leftPad("" + i, 2, "0");
			personRepository.save(new Person(name, i));
    	}

    	Iterable<Person> thirtyPersons = personRepository.findAll();
    	Assert.assertEquals(30, IteratorUtil.count(thirtyPersons));
    	
    	// 7 record per page
    	// -----------------------------------------------------
    	Sort sort = new Sort(Sort.Direction.ASC, "name");
    	
    	// Page 1
    	// -----------------------------------------------------    	
    	PageRequest pageRequest = new PageRequest(0, 7, sort);        
    	Page<Person> page = personRepository.findAll(pageRequest);
		Assert.assertEquals(7, page.getNumberOfElements());
		
		List<Person> personsPaginated = page.getContent();
		Assert.assertEquals("person_00", personsPaginated.get(0).getName());
		Assert.assertEquals("person_01", personsPaginated.get(1).getName());
		Assert.assertEquals("person_02", personsPaginated.get(2).getName());
		Assert.assertEquals("person_03", personsPaginated.get(3).getName());
		Assert.assertEquals("person_04", personsPaginated.get(4).getName());
		Assert.assertEquals("person_05", personsPaginated.get(5).getName());
		Assert.assertEquals("person_06", personsPaginated.get(6).getName());
		
		System.out.println("Page 1:");
		System.out.println(page.getTotalElements());
		System.out.println(page.getTotalPages());
		
		// Page 2
		// -----------------------------------------------------    	
		pageRequest = new PageRequest(1, 7, sort);        
		page = personRepository.findAll(pageRequest);
    	Assert.assertEquals(7, page.getNumberOfElements());
    	personsPaginated = page.getContent();
		Assert.assertEquals("person_07", personsPaginated.get(0).getName());
		Assert.assertEquals("person_08", personsPaginated.get(1).getName());
		Assert.assertEquals("person_09", personsPaginated.get(2).getName());
		Assert.assertEquals("person_10", personsPaginated.get(3).getName());
		Assert.assertEquals("person_11", personsPaginated.get(4).getName());
		Assert.assertEquals("person_12", personsPaginated.get(5).getName());
		Assert.assertEquals("person_13", personsPaginated.get(6).getName());
		
		System.out.println("Page 2:");
		System.out.println(page.getTotalElements());
		System.out.println(page.getTotalPages());
		
		// Page 3
		// -----------------------------------------------------    	
    	pageRequest = new PageRequest(2, 7, sort);        
    	page = personRepository.findAll(pageRequest);
    	Assert.assertEquals(7, page.getNumberOfElements());
    	personsPaginated = page.getContent();
		Assert.assertEquals("person_14", personsPaginated.get(0).getName());
		Assert.assertEquals("person_15", personsPaginated.get(1).getName());
		Assert.assertEquals("person_16", personsPaginated.get(2).getName());
		Assert.assertEquals("person_17", personsPaginated.get(3).getName());
		Assert.assertEquals("person_18", personsPaginated.get(4).getName());
		Assert.assertEquals("person_19", personsPaginated.get(5).getName());
		Assert.assertEquals("person_20", personsPaginated.get(6).getName());

		System.out.println("Page 3:");
		System.out.println(page.getTotalElements());
		System.out.println(page.getTotalPages());
		
		// Page 4
		// -----------------------------------------------------    	
    	pageRequest = new PageRequest(3, 7, sort);        
    	page = personRepository.findAll(pageRequest);
    	Assert.assertEquals(7, page.getNumberOfElements());
    	personsPaginated = page.getContent();
		Assert.assertEquals("person_21", personsPaginated.get(0).getName());
		Assert.assertEquals("person_22", personsPaginated.get(1).getName());
		Assert.assertEquals("person_23", personsPaginated.get(2).getName());
		Assert.assertEquals("person_24", personsPaginated.get(3).getName());
		Assert.assertEquals("person_25", personsPaginated.get(4).getName());
		Assert.assertEquals("person_26", personsPaginated.get(5).getName());
		Assert.assertEquals("person_27", personsPaginated.get(6).getName());

		System.out.println("Page 4:");
		System.out.println(page.getTotalElements());
		System.out.println(page.getTotalPages());
		
		// Page 5
		// -----------------------------------------------------    	
		pageRequest = new PageRequest(4, 7, sort);        
    	page = personRepository.findAll(pageRequest);
    	Assert.assertEquals(2, page.getNumberOfElements());
    	personsPaginated = page.getContent();
		Assert.assertEquals("person_28", personsPaginated.get(0).getName());
		Assert.assertEquals("person_29", personsPaginated.get(1).getName());
		
		System.out.println("Page 5:");
		System.out.println(page.getTotalElements());
		System.out.println(page.getTotalPages());
    }
}
