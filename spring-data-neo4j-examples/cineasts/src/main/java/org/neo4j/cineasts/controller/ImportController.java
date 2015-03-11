package org.neo4j.cineasts.controller;

import org.neo4j.cineasts.movieimport.MovieDbImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author mh
 * @since 04.03.11
 */
@Controller
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);
    private MovieDbImportService importService;

    @Autowired
    public ImportController(MovieDbImportService importService) {
        this.importService = importService;
    }

    @RequestMapping(value = "/import/{ids}", method = RequestMethod.GET)
    public String importMovie(@PathVariable String ids, Model model) {
        long start = System.currentTimeMillis();
        final Map<Integer, String> movies = importService.importMovies(extractRanges(ids));
        long duration = (System.currentTimeMillis() - start) / 1000;
        model.addAttribute("duration", duration);
        model.addAttribute("ids", ids);
        model.addAttribute("movies", movies.entrySet());
        return "import/result";
    }


    private Map<Integer, Integer> extractRanges(String ids) {
        Map<Integer, Integer> ranges = new LinkedHashMap<Integer, Integer>();
        StringBuilder errors = new StringBuilder();
        for (String token : ids.split(",")) {
            try {
                if (token.contains("-")) {
                    String[] range = token.split("-");
                    ranges.put(Integer.parseInt(range[0]), Integer.parseInt(range[1]));
                } else {
                    int id = Integer.parseInt(token);
                    ranges.put(id, id);
                }
            } catch (Exception e) {
                errors.append(token).append(": ").append(e.getMessage()).append("\n");
            }
        }
        if (errors.length() > 0) {
            throw new RuntimeException("Error parsing ids\n" + errors);
        }
        return ranges;
    }
}
