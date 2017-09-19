# Setting up Frontend Development Environment


### Setting up Frontend Development Environment for Windows (AngularJS)

1. Goto <http://nodejs.org/download/> and download the NodeJS version you need.<br>
2. install NodeJS<br>
3. make sure that NodeJS is correctly installed by opening a Windows Command Console and typing `node -v` then enter. If it didn't work, check whether or not your node-directory are in your system-path-variable<br>
4. Windows users need to install msygit(http://msysgit.github.io/) with the right option. see the bower installation instruction for the right option.<br>
5. the maven install task will download and install bower and jspm for you that in turn will be used to install the owner-apps dependencies. However maven isn't yet configured to run `jspm install` as there's no easy integration via maven-frontend as with npm.

Tip: if you get the error "err Registry bower not found", you need to execute the following two commands:
```
cd webofneeds/won-owner-webapp/src/main/webapp/
./node_modules/.bin/jspm registry create bower jspm-bower-endpoint
```
If that fails with the follwing error:
```
err  Registry handler jspm-bower-endpoint not installed.
```
Then you first need to install jspm-bower-endpoint:
```
npm install jspm-bower-endpoint
```

Tip: if the owner webapp is missing a lot of js files (look into the browser's js console and network tools), jspm didn't download the js dependencies.
Go to `webofneeds/webofneeds/won-owner-webapp/src/main/webapp` and run `node_modules/jspm/jspm.js install`


<!--
### Building with IntelliJ

1. Conf-Folder
    1. Copy the `conf` folder to `conf.local`
    1. Adapt the properties files to your setup
1. Make sure you have the following external dependencies installed:
    * Maven 3.0.5 (+configured in your IntelliJ).
    * Tomcat 7.0.57 (+configured in your IntelliJ).
    * For the owner-app (a more detailed guide can be found [here](https://www.jetbrains.com/idea/help/using-gulp-task-runner.html#d588211e148))
        * Node.js (should be in your $PATH)[2]
        * Bower (should be in your $PATH)[2]
        * Git (should be in your $PATH)
        * Gulp (should be in your $PATH)[2]
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

[2]: Should be automatically installed during maven-install thanks to the [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin)

A **major build speedup** can be achieved by running the maven install task with the `copy-*-dependencies` options enabled and then copying the war files from the most generic `target`-folder to `$TOMCAT/shared/lib` and afterwards always using the `skip-dependencies` option for maven install.
-->

### Enabling GZip-compression

If you want to enable gzip-compression on you local tomcat (e.g. because you're tweaking with page-load optimisations), you can enable gzip-compression by adding the following to your tomcat's `server.xml`:


```xml
<Connector
    compression="on"
    compressableMimeType="text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/x-font-ttf"
   .../>
```
