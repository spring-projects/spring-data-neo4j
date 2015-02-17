package school.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import school.domain.School;
import school.service.ImportService;
import school.service.SchoolService;
import school.service.Service;

import javax.servlet.http.HttpServletResponse;

@RestController
public class SchoolController extends Controller<School> {

    @Autowired
    private SchoolService schoolService;

    @Autowired
    ImportService importService;

    @RequestMapping("/reload")
    public Iterable<School> reload() {
        importService.reload();
        return list();
    }

    @RequestMapping(value = "/schools", method= RequestMethod.GET)
    public Iterable<School> list(final HttpServletResponse response) {
        setHeaders(response);
        return super.list();
    }

    @RequestMapping(value = "/schools", method = RequestMethod.POST)
    public  School create (@RequestBody School entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.create(entity);
    }

    @RequestMapping(value="/schools/{id}", method = RequestMethod.GET)
    public School find(@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        return super.find(id);
    }

    @RequestMapping(value="/schools/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/schools/{id}", method = RequestMethod.PUT)
    public  School update (@PathVariable Long id, @RequestBody School entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }

    @Override
    public Service<School> getService() {
        return schoolService;
    }


}
