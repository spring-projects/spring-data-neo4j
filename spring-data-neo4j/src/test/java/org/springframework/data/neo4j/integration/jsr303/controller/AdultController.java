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
