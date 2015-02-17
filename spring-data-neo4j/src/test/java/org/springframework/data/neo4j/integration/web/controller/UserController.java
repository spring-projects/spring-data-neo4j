package org.springframework.data.neo4j.integration.web.controller;

import org.springframework.data.neo4j.integration.web.domain.User;
import org.springframework.data.neo4j.integration.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/user/{name}/friends")
    @ResponseBody
    public String listFriends(@PathVariable String name, HttpSession session) {
        System.out.println("Session: " + session);
        User user = userService.getUserByName(name);

        if (user == null) {
            return "No such user!";
        }

        StringBuilder result = new StringBuilder();
        for (User friend : userService.getNetwork(user)) {
            result.append(friend.getName()).append(" ");
        }

        return result.toString().trim();
    }
}
