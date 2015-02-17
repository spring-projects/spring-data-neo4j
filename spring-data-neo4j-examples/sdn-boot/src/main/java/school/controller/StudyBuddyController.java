package school.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import school.domain.StudyBuddy;
import school.service.Service;
import school.service.StudyBuddyService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class StudyBuddyController extends Controller<StudyBuddy> {

    @Autowired
    private StudyBuddyService studyBuddyService;

    @Override
    public Service<StudyBuddy> getService() {
        return studyBuddyService;
    }

    @RequestMapping("/popularStudyBuddies")
    public Iterable<Map<String,Object>> popularStudyBuddies() {
        return studyBuddyService.getStudyBuddiesByPopularity();
    }

    @RequestMapping(value = "/studyBuddies", method= RequestMethod.GET)
    public Iterable<StudyBuddy> list(final HttpServletResponse response) {
        setHeaders(response);
        return super.list();
    }

    @RequestMapping(value = "/studyBuddies", method = RequestMethod.POST, consumes = "application/json")
    public  StudyBuddy create (@RequestBody StudyBuddy entity, final HttpServletResponse response) {
        setHeaders(response);
        return getService().createOrUpdate(entity);
    }

    @RequestMapping(value="/studyBuddies/{id}", method = RequestMethod.GET)
    public StudyBuddy find(@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        return super.find(id);
    }

    @RequestMapping(value="/studyBuddies/{id}", method = RequestMethod.DELETE)
    public void delete (@PathVariable Long id, final HttpServletResponse response) {
        setHeaders(response);
        super.delete(id);
    }

    @RequestMapping(value="/studyBuddies/{id}", method = RequestMethod.PUT, consumes = "application/json")
    public  StudyBuddy update (@PathVariable Long id, @RequestBody StudyBuddy entity, final HttpServletResponse response) {
        setHeaders(response);
        return super.update(id, entity);
    }
}
