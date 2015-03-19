/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.jsr303.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.jsr303.domain.Adult;
import org.springframework.data.neo4j.integration.jsr303.service.AdultService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.ValidationException;

/**
 * @author Vince Bickers
 */
@Controller
public class AdultController {

    @Autowired
    private AdultService service;

    @RequestMapping(value = "/adults", method = RequestMethod.POST, consumes = "application/json")
    @ResponseBody
    public Adult create (@Valid @RequestBody Adult entity, BindingResult bindingResult) {
        // in practice we'd do a bit more than this...
        if (bindingResult.hasErrors()) {
            throw new ValidationException("oops");
        }
        return service.save(entity);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseBody
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public void handleException(ValidationException ve) { }

}
