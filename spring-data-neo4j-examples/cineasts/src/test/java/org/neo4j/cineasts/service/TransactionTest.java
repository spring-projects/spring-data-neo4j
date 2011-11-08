package org.neo4j.cineasts.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 08.11.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@DirtiesContext
public class TransactionTest {

    @Autowired
    UserRepository userRepository;
    @Autowired
    MovieRepository movieRepository;

    @Test
    @Ignore
    public void testBefriendUsers() {
        final User me = userRepository.save(new User("me", "me", "me"));
        final User you = userRepository.save(new User("you", "you", "you"));
        userRepository.addFriend("you", userRepository.getUserFromSession());
        final User loaded = userRepository.findOne(me.getId());
        assertEquals(1,loaded.getFriends().size());
    }

    @Test
    public void testRateMovie() {
        final User me = userRepository.save(new User("me", "me", "me"));
        final Movie movie = movieRepository.save(new Movie("1","Movie"));
        userRepository.rate(movie, me, 5, "cool");
        final User loaded = userRepository.findOne(me.getId());
        assertEquals(1,loaded.getRatings().size());
    }
}
