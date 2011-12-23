Spring Data Neo4j Todos
=======================

A simple todo list using Spring Data Neo4j, configured for easy deployment to Heroku.

Get Ready
---------

To prepare for deploying to Heroku, you must first become a Hero:

* Create an account on [Heroku](http://heroku.com)
* Install the `heroku` command-line client
* See the [Heroku Quickstart](http://devcenter.heroku.com/articles/quickstart) for details

Feel ready? OK, then get a copy of this project and checkout the heroku-deploy branch:

```bash
git clone git://github.com/SpringSource/spring-data-neo4j.git
cp -r spring-data-neo4j/spring-data-neo4j-examples/todos .
cd todos
```

Local build and run
-------------------

* Run a local Neo4j server (using default config)

```bash
export NEO4J_REST_URL=http://localhost:7474/db/data
export NEO4J_LOGIN=""
export NEO4J_PASSWORD=""
```
* Build, then run locally

```bash
mvn package
sh target/bin/webapp
```

* check that it is working (in  another terminal, but same project directory)

```bash
./bin/todo mk "tweet high praises to @neo4j"
./bin/todo list
```

Satisfied? Then `ctrl-c` to kill the application.


Deploy on Heroku
----------------

Initialize a local git repository:

```bash
git init .
git add .
git commit -m "the start of my own todo application"
```

Create a new provisioning stack on Heroku for the app. Heroku will notice that you've got
a git repository, adding itself as a remote.

```bash
heroku create --stack cedar`
heroku addons:add neo4j
git push heroku master
```

Check to make sure it is running with `heroku ps`, which should show a `web.1` process with an "up" state.

Finally try it out using the `-r` flag to indicate remote access.

```bash
./bin/todo -r mk "tweet thanks for the good work @mesirii @akollegger"
./bin/todo -r list
```

To see the Neo4j graph you just created through Heroku, use `heroku config` to reveal the NEO4J_URL 
which can take you to Neo4j's Webadmin. Have fun!

For details about preparing your own Spring Data Neo4j application for deployment to Heroku,
see the Heroku chapter in "Good Relationships: The Spring Data Neo4j Guide Book".

Don't forget to decommission the Heroku application when you're done with it. 
Use `heroku destroy` to free up the instance. 

CLI Tool
--------

A simplistic `todo` script in the `bin` directory can directly create, delete and list todos.

    Usage: todo [-r] [ list | mk | rm ]

      todo -r              - access the remote (Heroku) todo application
      todo list            - list current todos
      todo mk "a new todo" - to create a todo
      todo rm 1            - to remove the todo with id 1


