package org.neo4j.app.todos;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

@RequestMapping("/todos")
@Controller
public class TodoController {
	
    @Autowired TodoRepository todos;

    /**
     * POST /todos { "title": "text of the todo" }
     * 
     * Creates a new todo.
     * 
     * @param json
     * @return json containing the id of the newly created todo
     */
    @RequestMapping(method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<String> createFromJson(@RequestBody String json) {
        Todo createdTodo = todos.save(Todo.fromJsonToTodo(json));
        HttpHeaders headers= new HttpHeaders();
        headers.add("Content-Type", "application/text");
        return new ResponseEntity<String>("{\"id\":" + createdTodo.getId() + "}", headers, HttpStatus.CREATED);
    }
   
    /**
     * GET /todos/{id}
     * 
     * Gets the full json for a todo.
     * 
     * @param id
     * @return json representation of the requested todo, or HttpStatus.NOT_FOUND
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<String> showJson(@PathVariable("id") Long id) {
        Todo todo = todos.findOne(id);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text; charset=utf-8");
        if (todo == null) {
            return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>(todo.toJson(), headers, HttpStatus.OK);
    }
    
    /**
     * Get /todos/
     * 
     * Gets a json array of all todos.
     * 
     * @return json array representation of all todos
     */
    @RequestMapping(headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<String> listJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/text; charset=utf-8");
        return new ResponseEntity<String>(Todo.toJsonArray(todos.findAll()), headers, HttpStatus.OK);
    }

    /**
     * PUT /todos/ { "id": id, "title": "text of todo", idDone: true|false }
     * 
     * Updates an existing todo.
     * 
     * @param json full json representation of the todo to update
     * @return
     */
    @RequestMapping(method = RequestMethod.PUT, headers = "Accept=application/json")
    public ResponseEntity<String> updateFromJson(@RequestBody String json) {
        HttpHeaders headers= new HttpHeaders();
        headers.add("Content-Type", "application/text");
        if (todos.save(Todo.fromJsonToTodo(json)) == null) {
            return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<String>(headers, HttpStatus.OK);
    }
    
    /**
     * DELETE /todos/{id}
     * 
     * Deletes an existing todo. 
     * 
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, headers = "Accept=application/json")
    public ResponseEntity<String> deleteFromJson(@PathVariable("id") Long id) {
        Todo todo = todos.findOne(id);
        HttpHeaders headers= new HttpHeaders();
        headers.add("Content-Type", "application/text");
        if (todo == null) {
            return new ResponseEntity<String>(headers, HttpStatus.NOT_FOUND);
        }
        todos.delete(todo);
        return new ResponseEntity<String>(headers, HttpStatus.OK);
    }

    String encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        try {
            pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
        }
        catch (UnsupportedEncodingException uee) {}
        return pathSegment;
    }

}
