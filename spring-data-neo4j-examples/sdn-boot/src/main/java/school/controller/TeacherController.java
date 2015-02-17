package school.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import school.domain.Teacher;
import school.service.Service;
import school.service.TeacherService;

import javax.servlet.http.HttpServletResponse;

@RestController
public class TeacherController extends Controller<Teacher> {

    @Autowired
    private TeacherService teacherService;


    @Override
    public Service<Teacher> getService() {
        return teacherService;
    }

    @RequestMapping(value = "/teachers", method= RequestMethod.GET)
    public Iterable<Teacher> list(final HttpServletResponse response) {
        setHeaders(response);
        return super.list();
    }

    @RequestMapping(value = "/teachers", method = RequestMethod.POST, consumes = "application/json")
    public  Teacher create (@RequestBody Teacher entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.create(entity);
    }

    @RequestMapping(value="/teachers/{id}", method = RequestMethod.GET)
    public Teacher find(@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        return super.find(id);
    }

    @RequestMapping(value="/teachers/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/teachers/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public  Teacher update (@PathVariable Long id, @RequestBody Teacher entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }

}
