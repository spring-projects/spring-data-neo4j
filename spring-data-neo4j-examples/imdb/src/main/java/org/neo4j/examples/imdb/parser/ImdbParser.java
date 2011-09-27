package org.neo4j.examples.imdb.parser;

import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * A <code>ImdbParser</code> can parse the movie and actor/actress lists from
 * the imdb text data (http://www.imdb.com/interfaces). It uses an
 * {@link ImdbReader} forwarding the parsed information.
 */
public class ImdbParser {
    private static final String MOVIES_MARKER = "MOVIES LIST";
    private static final int MOVIES_SKIPS = 2;
    private static final String ACTRESSES_MARKER = "THE ACTRESSES LIST";
    private static final int ACTRESS_SKIPS = 4;
    private static final String ACTOR_MARKER = "THE ACTORS LIST";
    private static final int ACTOR_SKIPS = 4;
    private static final int BUFFER_SIZE = 200;
    private final ImdbReader reader;

    /**
     * Create a new Imdb parser.
     *
     * @param reader reader this parser will use to forward events to
     */
    public ImdbParser(final ImdbReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Null ImdbReader");
        }
        this.reader = reader;
    }

    /**
     * Parsers a tab-separated movie list file, each line containing a movie
     * title and the year the movie was released. The file can be .gz or .zip
     * compressed, and must then have the corresponding file extension.
     *
     * @param file name of movie list file
     * @throws IOException if unable to open the movie list file
     */
    public String parseMovies(final String file) throws IOException {
        final List<MovieData> buffer = new LinkedList<MovieData>();
        if (file == null) {
            throw new IllegalArgumentException("Null movie file");
        }
        BufferedReader fileReader = getFileReader(file, MOVIES_MARKER,
                MOVIES_SKIPS);
        String line = fileReader.readLine();
        int movieCount = 0;
        while (line != null) {
            // get rid of blank lines and TV shows
            if ("".equals(line) || line.indexOf("(TV)") != -1) {
                line = fileReader.readLine();
                continue;
            }
            final int yearSep = line.indexOf('\t');
            if (yearSep > 0) {
                final String title = line.substring(0, yearSep).trim();
                String yearString = line.substring(yearSep).trim();
                if (yearString.length() > 4) {
                    yearString = yearString.substring(0, 4);
                }
                if (yearString.length() == 0 || yearString.charAt(0) == '?'
                        || title.contains("{") || title.startsWith("\"")) {
                    line = fileReader.readLine();
                    continue;
                }
                final int year = Integer.parseInt(yearString);
                buffer.add(new MovieData(title, year));
                movieCount++;
                if (movieCount % BUFFER_SIZE == 0) {
                    reader.newMovies(buffer);
                    buffer.clear();
                }
            }
            line = fileReader.readLine();
        }
        reader.newMovies(buffer);
        return (movieCount + " movies parsed and injected.");
    }

    /**
     * Parsers a tab-separated actors list file. A line begins with actor name
     * then followed by a tab and a movie title the actor acted in. Additional
     * movies the current actor acted in are found on the following line that
     * starts with a tab followed by the movie title.
     *
     * @param actorFile   name of actor list file
     * @param actressFile TODO
     * @throws IOException if unable to open actor list file
     */
    public String parseActors(final String actorFile, final String actressFile)
            throws IOException {
        if (actorFile == null) {
            throw new IllegalArgumentException("Null actor file");
        }
        if (actressFile == null) {
            throw new IllegalArgumentException("Null actress file");
        }
        String result = "";
        BufferedReader fileReader = getFileReader(actorFile, ACTOR_MARKER,
                ACTOR_SKIPS);
        result += "Actors: " + parseActorItems(fileReader) + "\n";
        fileReader.close();
        fileReader = getFileReader(actressFile, ACTRESSES_MARKER,
                ACTRESS_SKIPS);
        result += "Actresses: " + parseActorItems(fileReader);
        return result;
    }

    private String parseActorItems(BufferedReader fileReader)
            throws IOException {
        String line = fileReader.readLine();
        String currentActor = null;
        final List<ActorData> buffer = new LinkedList<ActorData>();
        final List<RoleData> movies = new ArrayList<RoleData>();
        int movieCount = 0;
        int actorCount = 0;
        while (line != null) {
            // get rid of blank lines
            if ("".equals(line)) {
                line = fileReader.readLine();
                continue;
            }
            int actorSep = line.indexOf('\t');
            if (actorSep >= 0) {
                String actor = line.substring(0, actorSep).trim();
                if (!"".equals(actor)) {
                    if (movies.size() > 0) {
                        buffer.add(new ActorData(currentActor, movies
                                .toArray(new RoleData[movies.size()])));
                        actorCount++;
                        movies.clear();
                    }
                    currentActor = actor;
                }
                String title = line.substring(actorSep).trim();
                if (title.length() == 0 || title.contains("{")
                        || title.startsWith("\"") || title.contains("????")) {
                    line = fileReader.readLine();
                    continue;
                }
                int characterStart = title.indexOf('[');
                int characterEnd = title.indexOf(']');
                String character = null;
                if (characterStart > 0 && characterEnd > characterStart) {
                    character = title.substring(characterStart + 1,
                            characterEnd);
                }
                int creditStart = title.indexOf('<');
                // int creditEnd = title.indexOf( '>' );
                // String credit = null;
                // if ( creditStart > 0 && creditEnd > creditStart )
                // {
                // credit = title.substring( creditStart + 1, creditEnd );
                // }
                if (characterStart > 0) {
                    title = title.substring(0, characterStart).trim();
                } else if (creditStart > 0) {
                    title = title.substring(0, creditStart).trim();
                }
                int spaces = title.indexOf("  ");
                if (spaces > 0) {
                    if (title.charAt(spaces - 1) == ')'
                            && title.charAt(spaces + 2) == '(') {
                        title = title.substring(0, spaces).trim();
                    }
                }
                movies.add(new RoleData(title, character));
                movieCount++;
                if (movieCount % BUFFER_SIZE == 0) {
                    reader.newActors(buffer);
                    buffer.clear();
                }
            }
            line = fileReader.readLine();
        }
        reader.newActors(buffer);
        return (actorCount + " added including " + movieCount + " characters parsed and injected.");
    }

    /**
     * Get file reader that corresponds to file extension.
     *
     * @param file      the file name
     * @param pattern   TODO
     * @param skipLines TODO
     * @return a file reader that uncompresses data if needed
     * @throws IOException
     * @throws FileNotFoundException
     */
    private BufferedReader getFileReader(final String file, String pattern,
                                         int skipLines) throws IOException, FileNotFoundException {
        BufferedReader fileReader;
        // support compressed files
        if (file.endsWith(".gz")) {
            fileReader = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new ClassPathResource(file).getInputStream())));
        } else if (file.endsWith(".zip")) {
            fileReader = new BufferedReader(new InputStreamReader(
                    new ZipInputStream(new ClassPathResource(file).getInputStream())));
        } else {
            fileReader = new BufferedReader(new FileReader(file));
        }

        String line = "";
        while (!pattern.equals(line)) {
            line = fileReader.readLine();
        }
        for (int i = 0; i < skipLines; i++) {
            line = fileReader.readLine();
        }

        return fileReader;
    }

}
