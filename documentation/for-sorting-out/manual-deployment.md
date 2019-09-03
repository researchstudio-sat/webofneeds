# Manual Deployment

After the build of the webofneeds project is run you can deploy the owner and wonnode components on a tomcat server.

### Deployment to Tomcat etc
The modules
* `won-owner-webapp`
* `won-node-webapp`
are configured to deploy to suitable containers in with the cargo maven plugin: `mvn cargo:redeploy -P <serverprofile>`.

### Server

The `won-owner-webapp` and `won-node-webapp` have plugin configurations that allow you to deploy your app to a Tomcat 7 container using [maven cargo] (http://cargo.codehaus.org/Maven2+plugin). First, you will have to configure a profile (in the main `.pom` file or your settings file located at `{HOME-DIR}/.m2/settings.xml`) that will look like this (replace values in [] according to your tomcat configuration):

    <profiles>
    ...
    <profile>
    	<id>server.[name]</id>
    	<properties>
    		<server.id>[serverHostName]</server.id>
    		<tomcat.url>http://${server.id}:8080/manager/text</tomcat.url>
    		<tomcat.user>[tomcat-user]</tomcat.user>
    		<tomcat.password>[tomcat-password]</tomcat.password>
    	</properties>
    </profile>
    ...
    </profiles>

Second, all webofneeds applications expect an environment variable `WON_CONFIG_DIR` pointing to a folder containing configuration files. Copy the contents of [/webofneeds/conf] (https://github.com/researchstudio-sat/webofneeds/tree/master/webofneeds/conf) to some location on the target host. In the files node.properties and owner.properties you will have to replace ´localhost´ with your server's dns name. For installing the system on a developer machine for testing purposes, just copy the contents of `/webofneeds/conf` to `/webofneeds/conf.local` - that folder is marked in the `.gitignore` file and will not be added to git.

Third, you will need to set up a tomcat user and give him `manager-script` permission. For details please check the Tomcat 7 documentation and [cargo Tomcat 7 documentation](http://cargo.codehaus.org/Tomcat+7.x)

Now you can deploy your webapps with just one command: `mvn clean install cargo:redeploy -P skip-dependencies,skip-tests,<serverprofile>`

Note: if you're using the ubuntu/debian tomcat7 package: you will have to add `tomcat-websocket.jar` and `websocket-api.jar` to the tomcat `lib` directory manually.

Now you have to do this:

1. **Copy the dependencies** to your tomcat's **shared libraries folder** (see Tomcat [documentation](http://tomcat.apache.org/tomcat-6.0-doc/appdev/deployment.html#Shared_Library_Files))
2. Deploy the war files to the tomcat server (you may use `mvn cargo:redeploy -P <serverprofile>`)

#### Speeding up server startup
At startup, the jasper compiler seems to scan all jar files that are not explicitly excluded for tag library descriptors. We don't really need this, as we don't use JSPs. Therefore, all our common libs should be excluded from scanning. A list of these files can be found in
```
/toolconfig/tomcat/tomcat-jasper-scan-exclude-jars.txt
```
Find the `catalina.properties` file for your deployment and add the list to the values for config property

`tomcat.util.scan.DefaultJarScanner.jarsToSkip`
and
`org.apache.catalina.startup.TldConfig.jarsToSkip`
