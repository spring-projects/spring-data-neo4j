package org.neo4j.cineasts.service;

import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.cineasts.domain.*;
import org.neo4j.cineasts.movieimport.MovieDbImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 04.03.11
 */
@Service
public class DatabasePopulator {

    @Autowired
    Neo4jTemplate template;
    @Autowired
    CineastsRepository repository;

    @Autowired
    MovieDbImportService importService;
    private final static Logger log = LoggerFactory.getLogger(DatabasePopulator.class);

    @Transactional
    public List<Movie> populateDatabase() {
        User me = new User("micha", "Micha", "password", User.Roles.ROLE_ADMIN,User.Roles.ROLE_USER).persist();
        User ollie = new User("ollie", "Olliver", "password",User.Roles.ROLE_USER).persist();
        me.addFriend(ollie);
        List<Integer> ids = asList(19995 , 194, 600, 601, 602, 603); //, 604, 605, 606, 607, 608, 609, 13, 20526, 11, 1893, 1892, 1894, 168, 193, 200, 157, 152, 201, 154, 12155, 58, 285, 118, 22, 392, 5255, 568, 9800, 497, 101, 120, 121, 122);
        List<Movie> result=new ArrayList<Movie>(ids.size());
        for (Integer id : ids) {
            result.add(importService.importMovie(String.valueOf(id)));
        }

        //me.rate(repository.getMovie("13"),5,"Inspiring");
        me.rate(repository.getMovie("603"),5,"Best of the series");
        return result;
    }

    @Transactional
    public void cleanDb() {
        new Neo4jDatabaseCleaner((AbstractGraphDatabase) template.getGraphDatabaseService()).cleanDb();
    }
}
