package org.neo4j.cineasts.controller;


import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Handles and retrieves the login or denied page depending on the URI template
 */
@Controller
public class UserController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    MovieRepository movieRepository;

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public String profile(Model model) {
        final User user = userRepository.getUserFromSession();
        model.addAttribute("user", user);
        if (user!=null) {
            model.addAttribute("recommendations", movieRepository.getRecommendations(user));
        }
        return "/user/index";
    }

    @RequestMapping(value = "/user/{login}/friends", method = RequestMethod.POST)
    public String addFriend(Model model, @PathVariable("login") String login) {
        userRepository.addFriend(login, userRepository.getUserFromSession());
		return "forward:/user/"+login;
    }

    @RequestMapping(value = "/user/{login}")
    public String publicProfile(Model model, @PathVariable("login") String login) {
        User profiled = userRepository.findByLogin(login);
        User user = userRepository.getUserFromSession();

        return publicProfile(model, profiled, user);
    }

    private String publicProfile(Model model, User profiled, User user) {
        if (profiled.equals(user)) return profile(model);

        model.addAttribute("profiled", profiled);
        model.addAttribute("user", user);
        model.addAttribute("isFriend", areFriends(profiled, user));
        return "/user/public";
    }

    private boolean areFriends(User user, User loggedIn) {
        return user!=null && user.isFriend(loggedIn);
    }
}