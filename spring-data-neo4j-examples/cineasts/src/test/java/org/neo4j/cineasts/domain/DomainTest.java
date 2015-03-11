package org.neo4j.cineasts.domain;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.PersistenceContext;
import org.neo4j.cineasts.repository.ActorRepository;
import org.neo4j.cineasts.repository.DirectorRepository;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@ContextConfiguration(classes = {PersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DomainTest extends WrappingServerIntegrationTest{

    @Autowired
    ActorRepository actorRepository;
    @Autowired
    DirectorRepository directorRepository;
    @Autowired
    MovieRepository movieRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    protected int neoServerPort() {
        return PersistenceContext.NEO4J_PORT;
    }

    @Test
    public void shouldAllowActorCreation() {
        Actor tomHanks = new Actor("1", "Tom Hanks");
        tomHanks = actorRepository.save(tomHanks);

        Actor foundTomHanks = actorRepository.findByProperty("name", tomHanks.getName()).iterator().next();
        assertEquals(tomHanks.getName(), foundTomHanks.getName());
        assertEquals(tomHanks.getId(), foundTomHanks.getId());

    }

    @Test
    public void shouldAllowDirectorCreation() {
        Director robert = new Director("1", "Robert Zemeckis");
        robert = directorRepository.save(robert);

        Director foundRobert = directorRepository.findByProperty("name", robert.getName()).iterator().next();
        assertEquals(robert.getId(), foundRobert.getId());
        assertEquals(robert.getName(), foundRobert.getName());

    }

    @Test
    public void shouldAllowMovieCreation() {
        Movie forrest = new Movie("1", "Forrest Gump");
        forrest = movieRepository.save(forrest);

        Movie foundForrest = movieRepository.findByProperty("title", forrest.getTitle()).iterator().next();
        assertEquals(forrest.getId(), foundForrest.getId());
        assertEquals(forrest.getTitle(), foundForrest.getTitle());
    }

    @Test
    public void shouldAllowDirectorToDirectMovie() {
        Movie forrest = new Movie("1", "Forrest Gump");
        forrest = movieRepository.save(forrest);

        Director robert = new Director("1", "Robert Zemeckis");
        robert.directed(forrest);
        robert = directorRepository.save(robert);

        Director foundRobert = directorRepository.findByProperty("name", robert.getName()).iterator().next();
        assertEquals(robert.getId(), foundRobert.getId());
        assertEquals(robert.getName(), foundRobert.getName());
        assertEquals(forrest, robert.getDirectedMovies().iterator().next());

        Movie foundForrest = movieRepository.findByProperty("title", forrest.getTitle()).iterator().next();
        assertEquals(1, foundForrest.getDirectors().size());
        assertEquals(foundRobert, foundForrest.getDirectors().iterator().next());

    }

    @Test
    public void shouldAllowActorToActInMovie() {
        Movie forrest = new Movie("1", "Forrest Gump");
        forrest = movieRepository.save(forrest);

        Actor tomHanks = new Actor("1", "Tom Hanks");
        tomHanks = actorRepository.save(tomHanks);

        tomHanks.playedIn(forrest, "Forrest Gump");
        tomHanks = actorRepository.save(tomHanks);

        Actor foundTomHanks = actorRepository.findByProperty("name", tomHanks.getName()).iterator().next();
        assertEquals(tomHanks.getName(), foundTomHanks.getName());
        assertEquals(tomHanks.getId(), foundTomHanks.getId());
        assertEquals("Forrest Gump", foundTomHanks.getRoles().iterator().next().getName());
    }

    @Test
    public void userCanRateMovie() {
        Movie forrest = new Movie("1", "Forrest Gump");
        //forrest =  movieRepository.save(forrest);

        User micha = new User("micha", "Micha", "password");
        micha = userRepository.save(micha);

        Rating awesome = micha.rate(forrest, 5, "Awesome");
        micha = userRepository.save(micha);


        User foundMicha = userRepository.findByProperty("login", "micha").iterator().next();
        assertEquals(1, foundMicha.getRatings().size());

        Movie foundForrest = movieRepository.findByProperty("title", forrest.getTitle()).iterator().next();
        assertEquals(1, foundForrest.getRatings().size());

        Rating rating = foundForrest.getRatings().iterator().next();
        assertEquals(awesome, rating);
        assertEquals("Awesome", rating.getComment());
        assertEquals(5, rating.getStars());
        assertEquals(5, foundForrest.getStars(), 0);
    }


    @Test
    @Ignore
    public void movieCanBeRatedByUser() {
        Movie forrest = new Movie("1", "Forrest Gump");

        User micha = new User("micha", "Micha", "password");

        Rating awesome = new Rating(micha, forrest, 5, "Awesome");

        forrest.addRating(awesome);
        movieRepository.save(forrest);

        User foundMicha = userRepository.findByProperty("login", "micha").iterator().next();
        //TODO debug this   (the startNode/EndNode issue)
        /*
        org.neo4j.ogm.session.result.ResultProcessingException: "errors":[{"code":"Neo.DatabaseError.Statement.ExecutionFailure","message":null,"stackTrace":"java.lang.NullPointerException\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.builders.GetGraphElements$.getElements(GetGraphElements.scala:45)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.builders.GetGraphElements$.getOptionalElements(GetGraphElements.scala:28)\n\tat org.neo4j.cypher.internal.compiler.v2_1.commands.EntityProducerFactory$$anonfun$3$$anonfun$applyOrElse$4.apply(EntityProducerFactory.scala:82)\n\tat org.neo4j.cypher.internal.compiler.v2_1.commands.EntityProducerFactory$$anonfun$3$$anonfun$applyOrElse$4.apply(EntityProducerFactory.scala:80)\n\tat org.neo4j.cypher.internal.compiler.v2_1.commands.EntityProducerFactory$$anon$1.apply(EntityProducerFactory.scala:36)\n\tat org.neo4j.cypher.internal.compiler.v2_1.commands.EntityProducerFactory$$anon$1.apply(EntityProducerFactory.scala:35)\n\tat org.neo4j.cypher.internal.compiler.v2_1.pipes.matching.MonoDirectionalTraversalMatcher.findMatchingPaths(MonodirectionalTraversalMatcher.scala:46)\n\tat org.neo4j.cypher.internal.compiler.v2_1.pipes.TraversalMatchPipe$$anonfun$internalCreateResults$1.apply(TraversalMatchPipe.scala:36)\n\tat org.neo4j.cypher.internal.compiler.v2_1.pipes.TraversalMatchPipe$$anonfun$internalCreateResults$1.apply(TraversalMatchPipe.scala:33)\n\tat scala.collection.Iterator$$anon$13.hasNext(Iterator.scala:371)\n\tat scala.collection.Iterator$$anon$11.hasNext(Iterator.scala:327)\n\tat scala.collection.Iterator$class.foreach(Iterator.scala:727)\n\tat scala.collection.AbstractIterator.foreach(Iterator.scala:1157)\n\tat org.neo4j.cypher.internal.compiler.v2_1.pipes.EagerAggregationPipe.internalCreateResults(EagerAggregationPipe.scala:78)\n\tat org.neo4j.cypher.internal.compiler.v2_1.pipes.PipeWithSource.createResults(Pipe.scala:105)\n\tat org.neo4j.cypher.internal.compiler.v2_1.pipes.PipeWithSource.createResults(Pipe.scala:102)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionPlanBuilder$$anonfun$getExecutionPlanFunction$1$$anonfun$apply$2.apply(ExecutionPlanBuilder.scala:120)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionPlanBuilder$$anonfun$getExecutionPlanFunction$1$$anonfun$apply$2.apply(ExecutionPlanBuilder.scala:119)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionWorkflowBuilder.runWithQueryState(ExecutionPlanBuilder.scala:168)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionPlanBuilder$$anonfun$getExecutionPlanFunction$1.apply(ExecutionPlanBuilder.scala:118)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionPlanBuilder$$anonfun$getExecutionPlanFunction$1.apply(ExecutionPlanBuilder.scala:103)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionPlanBuilder$$anon$1.execute(ExecutionPlanBuilder.scala:68)\n\tat org.neo4j.cypher.internal.compiler.v2_1.executionplan.ExecutionPlanBuilder$$anon$1.execute(ExecutionPlanBuilder.scala:67)\n\tat org.neo4j.cypher.internal.ExecutionPlanWrapperForV2_1.execute(CypherCompiler.scala:159)\n\tat org.neo4j.cypher.ExecutionEngine.execute(ExecutionEngine.scala:76)\n\tat org.neo4j.cypher.ExecutionEngine.execute(ExecutionEngine.scala:71)\n\tat org.neo4j.cypher.javacompat.ExecutionEngine.execute(ExecutionEngine.java:84)\n\tat org.neo4j.server.rest.transactional.TransactionHandle.executeStatements(TransactionHandle.java:277)\n\tat org.neo4j.server.rest.transactional.TransactionHandle.commit(TransactionHandle.java:139)\n\tat org.neo4j.server.rest.web.TransactionalService$2.write(TransactionalService.java:202)\n\tat com.sun.jersey.core.impl.provider.entity.StreamingOutputProvider.writeTo(StreamingOutputProvider.java:71)\n\tat com.sun.jersey.core.impl.provider.entity.StreamingOutputProvider.writeTo(StreamingOutputProvider.java:57)\n\tat com.sun.jersey.spi.container.ContainerResponse.write(ContainerResponse.java:306)\n\tat com.sun.jersey.server.impl.application.WebApplicationImpl._handleRequest(WebApplicationImpl.java:1437)\n\tat com.sun.jersey.server.impl.application.WebApplicationImpl.handleRequest(WebApplicationImpl.java:1349)\n\tat com.sun.jersey.server.impl.application.WebApplicationImpl.handleRequest(WebApplicationImpl.java:1339)\n\tat com.sun.jersey.spi.container.servlet.WebComponent.service(WebComponent.java:416)\n\tat com.sun.jersey.spi.container.servlet.ServletContainer.service(ServletContainer.java:537)\n\tat com.sun.jersey.spi.container.servlet.ServletContainer.service(ServletContainer.java:699)\n\tat javax.servlet.http.HttpServlet.service(HttpServlet.java:848)\n\tat org.eclipse.jetty.servlet.ServletHolder.handle(ServletHolder.java:698)\n\tat org.eclipse.jetty.servlet.ServletHandler.doHandle(ServletHandler.java:505)\n\tat org.eclipse.jetty.server.session.SessionHandler.doHandle(SessionHandler.java:211)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doHandle(ContextHandler.java:1096)\n\tat org.eclipse.jetty.servlet.ServletHandler.doScope(ServletHandler.java:432)\n\tat org.eclipse.jetty.server.session.SessionHandler.doScope(SessionHandler.java:175)\n\tat org.eclipse.jetty.server.handler.ContextHandler.doScope(ContextHandler.java:1030)\n\tat org.eclipse.jetty.server.handler.ScopedHandler.handle(ScopedHandler.java:136)\n\tat org.eclipse.jetty.server.handler.HandlerList.handle(HandlerList.java:52)\n\tat org.eclipse.jetty.server.handler.HandlerWrapper.handle(HandlerWrapper.java:97)\n\tat org.eclipse.jetty.server.Server.handle(Server.java:445)\n\tat org.eclipse.jetty.server.HttpChannel.handle(HttpChannel.java:268)\n\tat org.eclipse.jetty.server.HttpConnection.onFillable(HttpConnection.java:229)\n\tat org.eclipse.jetty.io.AbstractConnection$ReadCallback.run(AbstractConnection.java:358)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:601)\n\tat org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:532)\n\tat java.lang.Thread.run(Thread.java:724)\n"}]}
	at org.neo4j.ogm.session.response.JsonResponse.parseErrors(JsonResponse.java:113)
         */
        assertEquals(1, foundMicha.getRatings().size());

        Movie foundForrest = movieRepository.findByProperty("id", "1").iterator().next();
        assertEquals(1, foundForrest.getRatings().size());

        Rating rating = foundForrest.getRatings().iterator().next();
        assertEquals(awesome, rating);
        assertEquals("Awesome", rating.getComment());
        assertEquals(5, rating.getStars());
        assertEquals(5, foundForrest.getStars(), 0);
    }

    @Test
    public void testBefriendUsers() {
        final User me = userRepository.register("me", "me", "me");
        final User you = userRepository.save(new User("you", "you", "you"));
        userRepository.addFriend("you", userRepository.getUserFromSession());
        final User loaded = userRepository.findByProperty("login", "me").iterator().next();
        assertEquals(1, loaded.getFriends().size());
    }

    @Test
    public void shouldBeAbleToSaveUserWithSecurityRoles() {
        User micha = new User("micha", "Micha", "password", User.SecurityRole.ROLE_ADMIN, User.SecurityRole.ROLE_USER);
        userRepository.save(micha);

        User foundMicha = userRepository.findByProperty("login","micha").iterator().next();
        assertEquals(micha.getName(),foundMicha.getName());
    }

    @Test
    @Ignore
    public void ratingForAMovieByAUserCanBeRetrieved() {
        Movie forrest = new Movie("1", "Forrest Gump");

        User micha = new User("micha", "Micha", "password");
        micha = userRepository.save(micha);

        Rating awesome = micha.rate(forrest, 5, "Awesome");
        micha = userRepository.save(micha);

        Movie foundForrest = movieRepository.findByProperty("id", "1").iterator().next();
        Rating foundAwesome = userRepository.findUsersRatingForMovie(foundForrest, micha);
        //TODO Infinite recursion
        assertNotNull(foundAwesome);
        assertEquals(foundAwesome, awesome);
    }

    @Test
    public void shouldBeAbleToSaveMovieWithTwoDirectors() {
        Movie matrix = new Movie("3", "The Matrix");
        matrix = movieRepository.save(matrix);

        Director andy = new Director("1", "Andy Wachowski");
        andy.directed(matrix);
        directorRepository.save(andy);

        Director lana = new Director("2", "Lana Wachowski");
        lana.directed(matrix);
        directorRepository.save(lana);

        Movie foundMatrix = movieRepository.findByProperty("id", "3").iterator().next();
        assertEquals(2, foundMatrix.getDirectors().size());
    }

    @Test
    public void shouldBeAbleToSaveMovieWithManyActors() {
        Movie matrix = new Movie("3", "The Matrix");
        matrix = movieRepository.save(matrix);

        //TODO save the actor after his role, don't save the movie, no roles saved on the movie
        Actor keanu = new Actor("6384","Keanu Reeves");
        actorRepository.save(keanu);
        keanu.playedIn(matrix,"Neo");
        matrix = movieRepository.save(matrix);

        Actor laurence = new Actor("2975","Laurence Fishburne");
        actorRepository.save(laurence);
        laurence.playedIn(matrix, "Morpheus");
        matrix = movieRepository.save(matrix);

        Actor carrie = new Actor("530", "Carrie-Ann Moss");
        actorRepository.save(carrie);
        carrie.playedIn(matrix, "Trinity");
        matrix = movieRepository.save(matrix);

        Actor foundKeanu = actorRepository.findByProperty("id","6384").iterator().next();
        assertEquals(1,foundKeanu.getRoles().size());

        Movie foundMatrix = movieRepository.findByProperty("id", "3").iterator().next();
        assertEquals(3, foundMatrix.getRoles().size());

    }

    @Test
    @Ignore
    public void personShouldBeAbleToBothActInAndDirectMovies() {   //TODO M>1
       /* Movie unforgiven = new Movie("4","Unforgiven");
        unforgiven = movieRepository.save(unforgiven);

        Actor clint = new Actor("5","Clint Eastwood");
        clint = actorRepository.save(clint);
        clint.playedIn(unforgiven,"Bill Munny");
        unforgiven=movieRepository.save(unforgiven);

        Person clintPerson = personRepository.findByProperty("id","5").iterator().next();
        Director clintDirector = new Director(clintPerson);
        clintDirector = directorRepository.save(clintDirector);
        unforgiven.addDirector(clintDirector);
        movieRepository.save(unforgiven);

        Movie foundUnforgiven = movieRepository.findByProperty("id","4").iterator().next();
        assertEquals(1,foundUnforgiven.getDirectors().size());
        assertEquals(1,foundUnforgiven.getRoles().size());
        assertEquals("5",foundUnforgiven.getDirectors().iterator().next().getId());
        assertEquals("5",foundUnforgiven.getRoles().iterator().next().getActor().getId());

        Person p = personRepository.findByProperty("id","5").iterator().next();
        assertNotNull(p);
        Actor actor =  actorRepository.findByProperty("id","5").iterator().next();
        assertNotNull(actor);
        Director d = directorRepository.findByProperty("id","5").iterator().next();
        assertNotNull(d);*/

    }

    @Test
    public void shouldBeAbleToGetEmptyRecommendationsForNewUser() {
        User micha = new User("micha", "Micha", "password", User.SecurityRole.ROLE_ADMIN, User.SecurityRole.ROLE_USER);
        userRepository.save(micha);

        List<Movie> recs = movieRepository.getRecommendations("micha");
        assertEquals(0,recs.size());
    }

    @Test
    @Ignore
    public void twoUsersCanRateSameMovie() {
        Movie forrest = new Movie("1", "Forrest Gump");

        User micha = new User("micha", "Micha", "password");
        micha = userRepository.save(micha);

        User luanne = new User("luanne","Luanne","password");
        luanne = userRepository.save(luanne);

        Rating awesome = micha.rate(forrest, 5, "Awesome");
        micha = userRepository.save(micha);


        User foundMicha = userRepository.findByProperty("login", "micha").iterator().next();
        assertEquals(1, foundMicha.getRatings().size());

        Movie foundForrest = movieRepository.findByProperty("title", forrest.getTitle()).iterator().next();
        assertEquals(1, foundForrest.getRatings().size());

        Rating okay = luanne.rate(forrest,3,"Okay");
        luanne = userRepository.save(luanne);

        User foundLuanne = userRepository.findByProperty("login", "luanne").iterator().next();
        assertEquals(1, foundLuanne.getRatings().size());
        foundMicha = userRepository.findByProperty("login", "micha").iterator().next();
        assertEquals(1, foundMicha.getRatings().size());

        foundForrest = movieRepository.findByProperty("title", forrest.getTitle()).iterator().next();
        assertEquals(2, foundForrest.getRatings().size());

      /*  Rating rating = foundForrest.getRatings().iterator().next();
        assertEquals(awesome, rating);
        assertEquals("Awesome", rating.getComment());
        assertEquals(5, rating.getStars());
        assertEquals(5, foundForrest.getStars(), 0);*/
    }

    @Test
    @Ignore
    public void shouldLoadActorsForAPersistedMovie() {
        new ExecutionEngine(getDatabase()).execute(
                "CREATE " +
                        "(dh:Movie {id:'600', title:'Die Hard'}), " +
                        "(bw:Person:Actor {name: 'Bruce Willis'}), " +
                        "(bw)-[:ACTS_IN {name:'Bruce'}]->(dh)");

        Movie dieHard = IteratorUtil.firstOrNull(movieRepository.findByProperty("title","Die Hard"));
        assertNotNull(dieHard);
        assertEquals(1,dieHard.getRoles().size());
    }

}
