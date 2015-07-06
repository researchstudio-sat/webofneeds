webofneeds
==========

The central idea of the Web of needs is this: the WWW looks like a marketplace; 
everyone knows what there is for sale, only a few know what is needed. We want to change that.

The central point is the 'need', which we represent as a linked data resource. It names communication
protocol endpoints that can be used to contact its owner and provide information about interesting other
'needs'.

Web of needs is built of 
- won-node: the server component hosting the needs
- won-owner: a simple application allowing for enduser need management  
- and a matching-service: currently implemented as a crawler that contacts needs (via the protocol endpoints) whenever 
   it finds a good match.

Requirements for getting started:
- jdk 1.6 or later
- maven 2 or later
- tomcat 6 or later as application-server

To prevent reinventing the wheel every single day, 
we come back to existing (old) technology for the matching services:
- siren                   -> http://siren.sindice.com/ 
- triplestore(virtuoso)   -> http://virtuoso.openlinksw.com/
- solr                    -> http://lucene.apache.org/solr/
- ldspider                -> https://code.google.com/p/ldspider/

---

won-node
- all needs and offers will be saved there

won-owner
- needs are managed by the won-owner

matching-service
- the matching-service crawls the won-nodes and looks for need and offer entries with corresponding values.


See also: http://events.linkeddata.org/ldow2013/papers/ldow2013-paper-13.pdf

# Building with IntelliJ

1. Conf-Folder
    1. Copy the `conf` folder to `conf.local`
    1. Adapt the properties files to your setup
1. Make sure you have the following external dependencies installed:
    * Maven 3.0.5 (+configured in your IntelliJ). 
    * Tomcat 7.0.57 (+configured in your IntelliJ). 
    * For the owner-app (a more detailed guide can be found [here](https://www.jetbrains.com/idea/help/using-gulp-task-runner.html#d588211e148))
        * Node.js (should be in your $PATH)
        * Bower (should be in your $PATH)
        * Git (should be in your $PATH)
        * Gulp (should be in your $PATH)
        * Nodejs-plugin for IntelliJ 
1. Import into IntelliJ via the Maven task
1. Create Gulp-configuration according to: <https://www.jetbrains.com/idea/help/using-gulp-task-runner.html#d588211e148>, pointing to the owner-app's gulpfile.js [1]
1. Create tomcat-configurations, e.g.:
    * After-launch: `http://localhost:8080/won/`
    * VM-Options: `-XX:MaxPermSize=250m -Dlogback.configurationFile=C:\WoN\webofneeds\webofneeds\conf.local\logback.xml -DWON_CONFIG_DIR=C:\WoN\webofneeds\webofneeds\conf.local -Dsolr.solr.home=C:\WoN\webofneeds\webofneeds\won-matcher-solr\target\won-matcher-solr-0.1-SNAPSHOT\siren\solr`
    * HTTP port: 8080
    * JMX port: 1099
    * Deployment
        * `won-node-webapp:war exploded` as `/won`
        * `won-owner-webapp:war exploded` as `/owner`
        * `apache-solr-3.5.0.war` as `/siren`
1. Make your tomcat deploy-configuration depend on the gulp configuration (don't forget to insert it *before* the packing of the `.war`-files) [1]

[1]: If you don't want to develop at the SCSS, you can skip these steps. Maven install will run gulp. If you do want to develop at the SCSS: Sadly there doesn't seem to be a way to run the gulp configuration when just reloading resources. During development you either need to run gulp manually (there's a handy `watch`-task in gulp) or restart the server every time to see the changes in the scss.

A **major build speedup** can be achieved by running the maven install task with the `copy-*-dependencies` options enabled and then copying the war files from the most generic `target`-folder to `$TOMCAT/shared/lib` and afterwards always using the `skip-dependencies` option for maven install.

