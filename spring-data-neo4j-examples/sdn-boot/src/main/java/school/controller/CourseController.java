package school.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import school.domain.Course;
import school.service.ClassRegisterService;
import school.service.Service;

@RestController
public class    CourseController extends Controller<Course> {

    @Autowired
    private ClassRegisterService classRegisterService;

    @RequestMapping(value = "/classes", method= RequestMethod.GET)
    public Iterable<Course> list(final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        return super.list();
    }
    @RequestMapping(value = "/classes", method = RequestMethod.POST, consumes = "application/json")
    public Course create (@RequestBody Course entity, final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        return super.create(entity);
    }

    @RequestMapping(value="/classes/{id}", method = RequestMethod.GET)
    public Course find(@PathVariable Long id, final HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        return super.find(id);
    }

    @RequestMapping(value="/classes/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/classes/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public Course update (@PathVariable Long id, @RequestBody Course entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }

    @Override
    public Service<Course> getService() {
        return classRegisterService;
    }


}
