# Setting up Frontend Development Environment


### Setting up Frontend Development Environment for Windows (AngularJS)

#### Installing Node

1. Goto <http://nodejs.org/download/> and download the NodeJS version you need.<br>
2. install NodeJS<br>
3. make sure that NodeJS is correctly installed by opening a Windows Command Console and typing `node -v` then enter. If it didn't work, check whether or not your node-directory are in your system-path-variable<br>

#### Installing Windows Build Tools on Windows

Run
```
mvn install
```
once. It will fail, that's ok.

Now install node in the won-owner-webapp module. You'll need to modify *that* node installation, but you need Administrator privileges to do so.

You'll need to run `npm install -g windows-build-tools` (otherwise building `node-sass` and `rdf-canonize` might fail later with a message about not finding VCBuild.exe. the command will install a python-environment and dotnet)

Open a windows power shell with admin permissions and run these commmand (possibly adapting the path for your settings):

```
$env:PATH = "C:\DATA\DEV\workspace\webofneeds\webofneeds\won-owner-webapp\src\main\webapp\node;" + $env:PATH
npm install -g windows-build-tools
```

If your Windows User does not have admin permissions, installing `windows-build-tools` with an admin user will unfortunately install them only for that user. (Further info at https://github.com/researchstudio-sat/webofneeds/pull/1743) The Visual Studio build tools are installed correctly however, so you only need to fix your Python installation:

1. Go to https://www.python.org/downloads/ and download and install Python 2.7 (Python 3 will not work!)
2. Locate your Python installation, e.g., `c:\Python27\python.exe`
3. With the **same user that will execute the maven build**, run `npm config set python c:\\Python27\\python.exe` (replace with your actual location, of course, and note the double backslashes).

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

### Adding js dependencies
For example, for adding a dependecy to `rdf-formats-common`, do this:

`node_modules\.bin\jspm install github:rdf-ext/rdf-formats-common`

### Updating N3.js

As their npm package assumes usage in node, updating it is a bit extra effort. See <../webofneeds/won-owner-webapp/src/main/webapp/scripts/N3/how-to-update.md> (it should be right next to the built scripts).
