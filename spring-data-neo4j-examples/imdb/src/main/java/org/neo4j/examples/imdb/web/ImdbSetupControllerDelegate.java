package org.neo4j.examples.imdb.web;

import org.neo4j.examples.imdb.domain.ImdbService;
import org.neo4j.examples.imdb.parser.ImdbParser;
import org.neo4j.examples.imdb.parser.ImdbReader;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

public class ImdbSetupControllerDelegate implements SetupControllerDelegate {
    private static final String IMDB_DATADIR = "data/";
    @Autowired
    private ImdbReader imdbReader;
    @Autowired
    private ImdbService imdbService;

    public void getModel(final Object command, final Map<String, Object> model)
            throws ServletException {

        final ImdbParser parser = new ImdbParser(imdbReader);
        StringBuffer message = new StringBuffer(200);
        try {
            message.append(parser.parseMovies(IMDB_DATADIR + "movies.list.gz")).append('\n');
            message.append(parser.parseActors(IMDB_DATADIR + "actors.list.gz", IMDB_DATADIR + "actresses.list.gz")).append('\n');
            imdbService.setupReferenceRelationship();
        } catch (IOException e) {
            message.append("Something went wrong during the setup process:\n").append(e.getMessage());
        }
        model.put("setupMessage", message.toString());
    }
}
