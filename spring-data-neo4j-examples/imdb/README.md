The IMDB Example Application for Neo4j written with Spring Datastore Graph
==========================================================================

![Web UI](http://github.com/neo4j-examples/spring-datastore-graph-imdb/raw/master/doc/images/IMDB1.png)


This application serves as a boilerplate to illustrate the use of annotations to persist objects in Spring to Neo4j, along the lines of 


	@NodeEntity
	public class Actor {
    	@GraphProperty(index = true)
    	private String name;

		@RelatedTo(type="ACTS_IN",elementClass = Movie.class)
		
		private Set<Movie> movies;
		static final String NAME_INDEX = "name";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Iterable<Movie> getMovies() {
			return movies;
		}
		
		public Role getRole(final Movie inMovie) {
			return (Role)getRelationshipTo((Movie)inMovie, Role.class, RelTypes.ACTS_IN.name());
		}
		
		@Override
		public String toString() {
			return "Actor '" + this.getName() + "'";
		}
	}
	
Which produces a graph similar to 

![Graph](http://wiki.neo4j.org/images/5/53/Imdb.screenshot.actor.png)



The documentation of the IMDB Example domain is found at [http://wiki.neo4j.org/content/IMDB_Example]


Run build and run the example:
	git clone git://github.com/neo4j-examples/spring-datastore-graph-imdb.git
	cd spring-datastore-graph-imdb
	mvn clean install jetty:run

browse to [http://localhost:8080/imdb/setup.html](http://localhost:8080/imdb/setup.html), insert the data and wait until it is completed (that may take some seconds). After that, you should be able to surf the data and look for actors and movies.

Stop the application with
	mvn jetty:stop

If you inspect the graph using Neoclipse,
node-icons are found in
src/test/resources/icons
