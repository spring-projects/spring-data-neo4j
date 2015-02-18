package school;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.IOException;
import java.util.Arrays;

import javax.annotation.PostConstruct;

// todo: replace these three with @SpringBootApplication

@Configuration
@EnableAutoConfiguration
@ComponentScan

@EnableNeo4jRepositories(basePackages = "school.repository", queryLookupStrategy = QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND)
@EnableTransactionManagement
public class Application extends Neo4jConfiguration{

    private final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    private Environment env;

    @Override
    @Bean
    public Neo4jServer neo4jServer() {
        log.info("Initialising server connection");
        return new RemoteServer("http://localhost:7474");
        //return new InProcessServer();
    }

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        log.info("Initialising Session Factory");
        return new SessionFactory("school.domain");
    }

    @Override
    @Bean
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Session getSession() throws Exception {
        log.info("Initialising session-scoped Session Bean");
        return super.getSession();
    }

    /**
     * Initializes registrar.
     * <p/>
     * Spring profiles can be configured with a program arguments --spring.profiles.active=your-active-profile
     * <p/>
     */
    @PostConstruct
    public void initApplication() throws IOException {
        if (env.getActiveProfiles().length == 0) {
            log.warn("No Spring profile configured, running with default configuration");
        } else {
            log.info("Running with Spring profile(s) : {}", Arrays.toString(env.getActiveProfiles()));
        }
    }

    /**
     * Main method, used to run the application.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
//        app.setShowBanner(false);
//        SimpleCommandLinePropertySource source = new SimpleCommandLinePropertySource(args);
//        // Check if the selected profile has been set as argument.
//        // if not the development profile will be added
//        addDefaultProfile(app, source);
        app.run(args);
    }

    /**
     * Set a default profile if it has not been set
     */
    private static void addDefaultProfile(SpringApplication app, SimpleCommandLinePropertySource source) {
        if (!source.containsProperty("spring.profiles.active")) {
            app.setAdditionalProfiles(Constants.SPRING_PROFILE_DEVELOPMENT);
        }
    }

}
