/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.examples.jsr303.controller;

import javax.validation.Valid;
import javax.validation.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.jsr303.domain.Adult;
import org.springframework.data.neo4j.examples.jsr303.service.AdultService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Vince Bickers
 */
@Controller
public class AdultController {

	@Autowired private AdultService service;

	@RequestMapping(value = "/adults", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public Adult create(@Valid @RequestBody Adult entity, BindingResult bindingResult) {
		// in practice we'd do a bit more than this...
		if (bindingResult.hasErrors()) {
			throw new ValidationException("oops");
		}
		return service.save(entity);
	}

	@ExceptionHandler(ValidationException.class)
	@ResponseBody
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public void handleException(ValidationException ve) {}

}
