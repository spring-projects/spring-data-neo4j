package school.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import school.domain.Student;
import school.service.Service;
import school.service.StudentService;

import javax.servlet.http.HttpServletResponse;

@RestController
public class StudentController extends Controller<Student> {

    @Autowired
    private StudentService studentService;

    @Override
    public Service<Student> getService() {
        return studentService;
    }

    @RequestMapping(value = "/students", method= RequestMethod.GET)
    public Iterable<Student> list(final HttpServletResponse response) {
        setHeaders(response);
        return super.list();
    }

    @RequestMapping(value = "/students", method = RequestMethod.POST, consumes = "application/json")
    public  Student create (@RequestBody Student entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.create(entity);
    }

    @RequestMapping(value="/students/{id}", method = RequestMethod.GET)
    public Student find(@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        return super.find(id);
    }

    @RequestMapping(value="/students/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/students/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public  Student update (@PathVariable Long id, @RequestBody Student entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }
}
