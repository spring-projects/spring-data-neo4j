package org.neo4j.cineasts.controller;

import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.Person;
import org.neo4j.cineasts.domain.Rating;
import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.service.CineastsUserDetailsService;
import org.neo4j.cineasts.service.DatabasePopulator;
import org.neo4j.cineasts.service.CineastsRepository;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * @author mh
 * @since 04.03.11
 */
@Controller
public class MovieController {

    private CineastsRepository moviesRepository;
    private CineastsUserDetailsService userDetailsService;
    private DatabasePopulator populator;
    private static final Logger log = LoggerFactory.getLogger(MovieController.class);

    @Autowired
    public MovieController(CineastsRepository moviesRepository, DatabasePopulator populator, CineastsUserDetailsService userDetailsService) {
        this.moviesRepository = moviesRepository;
        this.populator = populator;
        this.userDetailsService = userDetailsService;
    }

    // for web service (JSON) clients

    /**
     * Only matches 'GET /moviies/{id}}' requests for JSON content; a 404 is sent otherwise.
     * TODO send a 406 if an unsupported representation, such as XML, is requested.  See SPR-7353.
     */
    @RequestMapping(value = "/movies/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public
    @ResponseBody
    Movie getMovie(@PathVariable String id) {
        return moviesRepository.getMovie(id);
    }


    @RequestMapping(value = "/movies/{movieId}", method = RequestMethod.GET, headers = "Accept=text/html")
    public String singleMovieView(final Model model, @PathVariable String movieId) {
        User user = addUser(model);
        Movie movie = moviesRepository.getMovie(movieId);
        model.addAttribute("id", movieId);
        if (movie != null) {
            model.addAttribute("movie", movie);
            final int stars = movie.getStars();
            model.addAttribute("stars", stars);
            Rating rating = null;
            if (user!=null) rating = movie.getRelationshipTo(user, Rating.class, "RATED");
            if (rating == null) rating = new Rating().rate(stars,null);
            model.addAttribute("userRating",rating);
        }
        return "/movies/show";
    }

    @RequestMapping(value = "/movies/{movieId}", method = RequestMethod.POST, headers = "Accept=text/html")
    public String updateMovie(Model model, @PathVariable String movieId, @RequestParam(value = "rated",required = false) Integer stars, @RequestParam(value = "comment",required = false) String comment) {
        Movie movie = moviesRepository.getMovie(movieId);
        User user = userDetailsService.getUserFromSession();
        moviesRepository.rateMovie(movie,user, stars==null ? -1 : stars,comment!=null ? comment.trim() : null);
        return singleMovieView(model,movieId);
    }

    private User addUser(Model model) {
        User user = userDetailsService.getUserFromSession();
        model.addAttribute("user", user);
        return user;
    }

    @RequestMapping(value = "/movies", method = RequestMethod.GET, headers = "Accept=text/html")
    public String findMovies(Model model, @RequestParam("q") String query) {
        List<Movie> movies = moviesRepository.findMovies(query, 20);
        model.addAttribute("movies", movies);
        model.addAttribute("query", query);
        addUser(model);
        return "/movies/list";
    }

    @RequestMapping(value = "/actors/{id}", method = RequestMethod.GET, headers = "Accept=text/html")
    public String singleActorView(Model model, @PathVariable String id) {
        Person person = moviesRepository.getPerson(id);
        model.addAttribute("actor", person);
        model.addAttribute("id", id);
        model.addAttribute("roles",  IteratorUtil.asCollection(person.getRoles()));
        addUser(model);
        return "/actors/show";
    }

    //@RequestMapping(value = "/admin/populate", method = RequestMethod.GET)
    @RequestMapping(value = "/populate", method = RequestMethod.GET)
    public String populateDatabase(Model model) {
        Collection<Movie> movies=populator.populateDatabase();
        model.addAttribute("movies",movies);
        addUser(model);
        return "/movies/list";
    }

    @RequestMapping(value = "/admin/clean", method = RequestMethod.GET)
    public String clean(Model model) {
        populator.cleanDb();
        return "movies/list";
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(Model model) {
        addUser(model);
        return "index";
    }
}
