package school.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import school.domain.Subject;
import school.service.Service;
import school.service.SubjectService;

import javax.servlet.http.HttpServletResponse;

@RestController
public class SubjectController extends Controller<Subject> {

    @Autowired
    private SubjectService subjectService;

    @Override
    public Service<Subject> getService() {
        return subjectService;
    }

    @RequestMapping(value = "/subjects", method= RequestMethod.GET)
    public Iterable<Subject> list(final HttpServletResponse response) {
        setHeaders(response);
        return super.list();
    }

    @RequestMapping(value = "/subjects", method = RequestMethod.POST, consumes = "application/json")
    public  Subject create (@RequestBody Subject entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.create(entity);
    }

    @RequestMapping(value="/subjects/{id}", method = RequestMethod.GET)
    public Subject find(@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        return super.find(id);
    }

    @RequestMapping(value="/subjects/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/subjects/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public  Subject update (@PathVariable Long id, @RequestBody Subject entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }
}
