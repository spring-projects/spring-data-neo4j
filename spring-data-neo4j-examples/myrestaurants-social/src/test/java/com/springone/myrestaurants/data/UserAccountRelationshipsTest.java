package com.springone.myrestaurants.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.springone.myrestaurants.domain.Recommendation;
import com.springone.myrestaurants.domain.Restaurant;
import com.springone.myrestaurants.domain.UserAccount;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class UserAccountRelationshipsTest extends AbstractTestWithUserAccount {

    @Autowired
    UserAccountRepository userAccountRepo;
    
    @Autowired
    RestaurantRepository restaurantRepository;

    @Transactional
    @Test
    public void listFriends() {
    	UserAccount user = userAccountRepo.findUserAccount(userId);
    	UserAccount newUser = new UserAccount();
    	newUser.setFirstName("John");
    	newUser.setLastName("Doe");
    	newUser.setBirthDate(new Date());
    	newUser.setNickname("Bubba");
    	newUser.setUserName("jdoe");
    	em.persist(newUser);
        newUser.persist();
    	Assert.assertEquals("Should have zero friends", 0, newUser.getFriends().size());    	
    	newUser.getFriends().add(user);
    	Assert.assertEquals("Should have a list of friends", 1, newUser.getFriends().size());
    }
    
    @Transactional
    @Test
    public void testAddAndRemoveFriends() {
    	UserAccount user = userAccountRepo.findUserAccount(userId);
    	UserAccount newUser = new UserAccount();
    	newUser.setFirstName("John");
    	newUser.setLastName("Doe");
    	newUser.setBirthDate(new Date());
    	newUser.setNickname("Bubba");
    	newUser.setUserName("jdoe");
    	userAccountRepo.persist(newUser);
    	em.flush();
        newUser.persist();
    	user.getFriends().add(newUser);
    	UserAccount updatedUser = userAccountRepo.findUserAccount(userId);
    	Assert.assertNotNull("should have found something" ,updatedUser);
    	Assert.assertEquals("user should now have correct number of friends", 1, updatedUser.getFriends().size());
    	user.getFriends().remove(newUser);
    	Assert.assertEquals("user should now have correct number of friends", 0, updatedUser.getFriends().size());
    }

    @Transactional
    @Test
    public void testAddRecommendation() {
    	UserAccount user = userAccountRepo.findUserAccount(userId);
    	Restaurant rest = restaurantRepository.findRestaurant(22L);
    	user.rate(rest, 3, "Pretty Good");
    	em.flush();
    	UserAccount updatedUser = userAccountRepo.findUserAccount(userId);
    	Assert.assertNotNull("should have found something" ,updatedUser);
    	List<Recommendation> recommendations = new ArrayList<Recommendation>();
    	for (Recommendation r : updatedUser.getRecommendations()) {
    		recommendations.add(r);
    	}
    	Assert.assertEquals("user should now have correct number of recommendations", 1, recommendations.size());
    	Recommendation r = recommendations.get(0);
    	Assert.assertEquals("recommendation should have correct rating", 3, r.getStars());
    	Assert.assertEquals("recommendation should have correct comment", "Pretty Good", r.getComment());
    	Assert.assertEquals("recommendation should have correct restaurant id", new Long(22), r.getRestaurant().getId());
    	Assert.assertEquals("recommendation should have correct restaurant name", "Subway Sandwiches & Salads", r.getRestaurant().getName());
    }

}
