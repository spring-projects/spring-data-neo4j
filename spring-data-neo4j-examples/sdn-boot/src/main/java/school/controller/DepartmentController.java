package school.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import school.domain.Department;
import school.service.DepartmentService;
import school.service.Service;

import javax.servlet.http.HttpServletResponse;

@RestController
public class DepartmentController extends Controller<Department> {

    @Autowired
    private DepartmentService departmentService;

    @Override
    public Service<Department> getService() {
        return departmentService;
    }
    @RequestMapping(value = "/departments", method= RequestMethod.GET)
    public Iterable<Department> list(final HttpServletResponse response) {
        setHeaders(response);
        return super.list();
    }

    @RequestMapping(value = "/departments", method = RequestMethod.POST, consumes = "application/json")
    public  Department create (@RequestBody Department entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.create(entity);
    }

    @RequestMapping(value="/departments/{id}", method = RequestMethod.GET)
    public Department find(@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        return super.find(id);
    }

    @RequestMapping(value="/departments/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/departments/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public  Department update (@PathVariable Long id, @RequestBody Department entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }

}
