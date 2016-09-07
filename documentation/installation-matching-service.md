# Setting up the existing matching services
##### Components
* [siren](http://siren.sindice.com/) which is based on [solr](http://lucene.apache.org/solr/)
* [ldspider](https://code.google.com/p/ldspider/), or rather, [our fork](https://github.com/researchstudio-sat/ldspider4won)

#### Installing SIREn

SIREn was developed to allow for using Solr's indexing capabilities for RDF content. For detailed information about SIREn please have a look at:

* http://rdelbru.github.io/SIREn/
* https://github.com/rdelbru/SIREn/wiki/Solr-Getting-Started-and-Example

We've prepared a siren/solr application that contains our specific configuration. It can be found in the project folder under `webofneeds/won-matcher-solr`.

1) In that folder, type `mvn install -Pcopy-module-dependencies`

This builds our code and assembles the web application.

2) Copy the module dependency jar files from folder `webofneeds/won-matcher-solr/target/copiedDependencies` to the shared libs folder of your tomcat server. You can specify that folder in `[Catalina-conf]/catalina.properties` under the property `shared.loader` (create a folder if you don't have it yet).

It's not a good idea to pack it as a war file and deploy it in tomcat becaus it also contains a 'data' folder which holds the index and which would be deleted upon re-deploying. Rather, tomcat should be configured to load a servlet context from the folder that is created by maven (or rather, a copy of it in a suitable place).

3) Create a file `[Catalina-conf]/Catalina/localhost/siren.xml` with the following content:

    <Context docBase="{webofneeds-project-folder}/webofneeds/won-matcher-solr/target/won-matcher-solr-0.1-SNAPSHOT/siren/apache-solr-3.5.0.war" debug="0" crossContext="true" >
        <Environment name="solr/home" type="java.lang.String" value="{webofneeds-project-folder}/webofneeds/won-matcher-solr/target/won-matcher-solr-0.1-SNAPSHOT/siren/solr" override="true" />
    </Context>

4) Make sure you set the [WON_CONFIG_DIR](https://github.com/researchstudio-sat/webofneeds/blob/devel-interim/webofneeds/conf/README.txt) variable.

5) Start your tomcat server



#### Running ldspider
LDspider is a linked data crawler. It is used in our system to crawl the won-nodes and to feed siren/solr with the information found on these servers. Siren matches the needs and sends 'hint' messages to the won nodes.

We had to adapt ldspider for our needs. Clone our fork:
`git clone git@github.com:researchstudio-sat/ldspider4won.git`
and build the project using ant
`ant dist`
this will create an `ldspider-trunk.jar` file in the `dist` folder.

Back in the webofneeds project, the project folder `webofneeds/ldspider-scripts` contains a list of shellscripts you can use to run ldspider.

Copy the ldspider-trunk.jar file and the ldspider-scripts folders' contents together and run
`./loop-ldspider.sh` after putting your won node's URI (<server:port>/won/resource/need) into the seed.txt file.

Ldspider honors the 'Expires' HTTP header and keeps track of the expiry dates of URIs it has already downloaded. With the ldspider-*.sh scripts, these data are stored in a 'data' folder in the execution directory of ldspider. To re-download everything, just delete the 'data' folder.

For more information on how to use ldspider on the command line, see:
https://code.google.com/p/ldspider/wiki/GettingStartedCommandLine

