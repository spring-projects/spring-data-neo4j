/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.web.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.web.domain.User;
import org.springframework.data.neo4j.web.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Michal Bachman
 * @author Mark ANgrish
 */
@Controller
public class UserController {

	@Autowired private UserService userService;

	@RequestMapping(value = "/user/{uuid}/friends")
	@ResponseBody
	@Transactional
	public String listFriends(@PathVariable UUID uuid, HttpSession session) {
		User user = userService.getUserByUuid(uuid);

		if (user == null) {
			return "No such user!";
		}

		StringBuilder result = new StringBuilder();
		for (User friend : userService.getNetwork(user)) {
			result.append(friend.getName()).append(" ");
		}

		return result.toString().trim();
	}

	@RequestMapping(value = "/user/{uuid}/immediateFriends")
	@ResponseBody
	@Transactional
	public String listImmediateFriends(@PathVariable UUID uuid, HttpSession session) {
		User user = userService.getUserByUuid(uuid);

		if (user == null) {
			return "No such user!";
		}

		List<String> friends = new ArrayList<>();
		for (User friend : user.getFriends()) {
			friends.add(friend.getName());
		}
		Collections.sort(friends);

		StringBuilder result = new StringBuilder();
		for (String friend : friends) {
			result.append(friend).append(" ");
		}
		return result.toString().trim();
	}
}
