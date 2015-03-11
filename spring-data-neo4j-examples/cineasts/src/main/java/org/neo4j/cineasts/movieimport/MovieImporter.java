package org.neo4j.cineasts.movieimport;

import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.Collections;
import java.util.Map;

/**
 * @author mh
 * @since 04.10.11
 */
public class MovieImporter {

    private final MovieDbImportService importer;

    public MovieImporter(MovieDbImportService importer) {
        this.importer = importer;
    }

    public static void main(String[] args) {
        final FileSystemXmlApplicationContext ctx = new FileSystemXmlApplicationContext("src/main/webapp/WEB-INF/applicationContext.xml");
        try {
            final MovieDbImportService importer = ctx.getBean(MovieDbImportService.class);
            final MovieImporter movieImporter = new MovieImporter(importer);
            movieImporter.runImport(getMovieIdsToImport(args));
        } finally {
            ctx.close();
        }
    }

    private static Map<Integer, Integer> getMovieIdsToImport(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: MovieImporter 1 10000\nWorking Directory should be the cineasts directory with the json files in data/json.");
        }
        if (args.length == 1) {
            return Collections.singletonMap(Integer.valueOf(args[0]), Integer.valueOf(args[0]));
        }
        return Collections.singletonMap(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
    }

    private void runImport(Map<Integer, Integer> movieIdsToImport) {
        final long start = System.currentTimeMillis();
        final Map<Integer, String> result = importer.importMovies(movieIdsToImport);
        final long time = System.currentTimeMillis() - start;
        for (Map.Entry<Integer, String> movie : result.entrySet()) {
            System.out.println(movie.getKey() + "\t" + movie.getValue());
        }
        System.out.println("Imported movies took " + time + " ms.");
    }
}
