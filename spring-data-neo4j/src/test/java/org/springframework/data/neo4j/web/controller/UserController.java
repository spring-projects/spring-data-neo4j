/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
