# Outline

The following document walks you through the steps required to set up for development on all Web of Needs components, which are Java Spring applications deployed to a Tomcat server via Eclipse. Maven is used for dependency managment and building.

- [Outline](#outline)
- [Building](#building)
  - [1. Java SDK version 8 or newer](#1-java-sdk-version-8-or-newer)
  - [2. Eclipse for Java Enterprise Developers (4.7 Oxygen or newer)](#2-eclipse-for-java-enterprise-developers-47-oxygen-or-newer)
  - [3. Install Maven (3.3 or newer)](#3-install-maven-33-or-newer)
  - [4. Git Clone to Your Eclipse Workspace](#4-git-clone-to-your-eclipse-workspace)
  - [5. Import the Maven-project in Eclipse](#5-import-the-maven-project-in-eclipse)
  - [6. Deactivate eclipse autobuild](#6-deactivate-eclipse-autobuild)
  - [7. Use the provided code style](#7-use-the-provided-code-style)
  - [8. Set maven profile `skip-tests`](#8-set-maven-profile-skip-tests)
  - [9. Maven install](#9-maven-install)
  - [10. On Windows: build-tools for project-local npm](#10-on-windows-build-tools-for-project-local-npm)
  - [11. Copy and Adjust Configurations](#11-copy-and-adjust-configurations)
  - [12. Download/Install the latest Tomcat 9 server](#12-downloadinstall-the-latest-tomcat-9-server)
  - [13. Add Tomcat to Eclipse](#13-add-tomcat-to-eclipse)
  - [14. Update Connector-Statement in Tomcat's server.xml](#14-update-connector-statement-in-tomcats-serverxml)
  - [15. Edit the Server Configuration in Eclipse](#15-edit-the-server-configuration-in-eclipse)
    - [VM arguments](#vm-arguments)
    - [Add the JSTL jar to classpath](#add-the-jstl-jar-to-classpath)
    - [Add the required libs to the classpath](#add-the-required-libs-to-the-classpath)
    - [Server Overview Options](#server-overview-options)
    - [Options to speed up Startup](#options-to-speed-up-startup)
  - [16. Generate cryptographic keys](#16-generate-cryptographic-keys)
  - [17. Install the bouncycastle security provider and the trust manager](#17-install-the-bouncycastle-security-provider-and-the-trust-manager)
  - [18. Start server](#18-start-server)
- [Troubleshooting](#troubleshooting)
  - [Tomcat complains about Missing Keystore](#tomcat-complains-about-missing-keystore)
  - [Maven build fails with NoClassDefFoundException: java/sql/SQLException](#maven-build-fails-with-noclassdeffoundexception-javasqlsqlexception)
  - [Out of memory error](#out-of-memory-error)
  - [icu4j: Invalid byte tag in constant pool](#icu4j-invalid-byte-tag-in-constant-pool)
  - [won.protocol.exception.RDFStorageException: Could not create File!](#wonprotocolexceptionrdfstorageexception-could-not-create-file)
  - [Port Bind Problem - Failed to initialize end point associated with ProtocolHandler](#port-bind-problem---failed-to-initialize-end-point-associated-with-protocolhandler)
  - [maven dies in won-owner-webapp during 'clean' task](#maven-dies-in-won-owner-webapp-during-clean-task)
  - [java.security.NoSuchProviderException: no such provider: BC](#javasecuritynosuchproviderexception-no-such-provider-bc)
  - [Exception in Owner-Webapp log: PKIX path building failed](#exception-in-owner-webapp-log-pkix-path-building-failed)

# Building

## 1. Java SDK version 8 or newer

Sources:

- Your package manager
- Oracle Java: <https://www.oracle.com/technetwork/java/javase/downloads/index.html>
- Open JDK: <https://openjdk.java.net/>

Things to mind:

- Make sure your `$JAVA_HOME` environment variable points to the folder it's installed in.
- Make sure the Java-Binaries are on the system-path/`$PATH`
  - On Linux-distributions using `alternatives`, check `alternatives --list | grep java` and make sure you have the right SDK selected. You can change the configuration via `sudo alternatives --config <alternative-name>`

## 2. Eclipse for Java Enterprise Developers (4.7 Oxygen or newer)

Sources:

- <https://www.eclipse.org/downloads/packages/> >> "Eclipse for Java Enterprise Developers"
- Your package manager. Check if there's a Java EE version.

If you installed from your package manager, ensure the following addons are also installed:

- Eclipse Java EE Developer Tools
- Eclipse Web Developer Tools
- m2e Maven Integration

You can check in Help >> Install New Software >> "What's already installed". If anything isn't you can select "Work With: All Available Sites" and then search, select and install the missing addons.

## 3. Install Maven (3.3 or newer)

Sources:

- <https://maven.apache.org/>
- your package manager

If it's installed you should be able to run `mvn -version` on the command-line.

Make sure it's on the system `$PATH`.

## 4. Git Clone to Your Eclipse Workspace

Using your terminal (we recommend [Git SCM](https://git-scm.com/download/win) if you're on Windows) run:

```
git clone https://github.com/researchstudio-sat/webofneeds.git
```

## 5. Import the Maven-project in Eclipse

File >> Import >> Existing Maven Project >> select the (folder with) the pom.xml `webofneeds/pom.xml`

Troubleshooting: If you don't have the "Existing Maven Project"-option, make sure you have the addons mentioned in the [eclipse section above](#2-eclipse-java-ee).

## 6. Deactivate eclipse autobuild

Window >> Preferences >> General >> Workspace >> Build >> uncheck "Build automatically"

## 7. Use the provided code style

Window >> Preferences >> Java >> Code Style >> Formatter

1. Click `Import`
2. Select the file `webofneeds/won-buildtools/src/main/resources/eclipse/formatter.xml`
3. Click `Apply and Close`

## 8. Set maven profile `skip-tests`

Right-click the webofneeds project in the Project Explorer Tab on the left side >> Maven >> Select Maven Profiles. Check `skip-tests`.

## 9. Maven install

Either run `mvn install -P skip-tests` in the command-line or right-click "webofneeds" in project explorer >> Run As >> Maven Install

Further information on what you can do with the maven-configuration can be found in [./maven.md](./maven.md).

## 10. On Windows: build-tools for project-local npm

When you the maven-install reaches the owner-application, the maven-frontend plugin will install it's own version of `npm`. Using that run the following with admin permissions:

```
<PROJECT_ROOT>/webofneeds/won-owner-webapp/src/main/webapp/node/npm install -g windows-build-tools
```

If your Windows User does not have admin permissions, installing `windows-build-tools` will unfortunately install them only for that user. (Further info at https://github.com/researchstudio-sat/webofneeds/pull/1743) The Visual Studio build tools are installed correctly however, so you only need to fix your Python installation:

1. Go to https://www.python.org/downloads/ and download and install Python 2.7 (Python 3 will not work!)
2. Locate your Python installation, e.g., `c:\Python27\python.exe`
3. With the **same user that will execute the maven build**, run `npm config set python c:\\Python27\\python.exe` (replace with your actual location, of course, and note the double backslashes).

## 11. Copy and Adjust Configurations

1. Copy the webofneeds conf-folder: `cp -r conf conf.local` if you haven't done that already.

2. Change all instances of `localhost` in the configurations to your your IP (or computer name or domain) if you are not going to run node owner or matcher locally.

3. In `conf.local`, edit `matcher-service.properties`, `node.properties` and `owner.properties` and change all instances of keystore/truststore locations to point to a folder where you will store generated keys and certificates (in ), e.g. I used `/home/username/WoNKeystore/`). Do not change the file name, just the path.

4. Change the accompanying passwords to something at least 6-letter long.

## 12. Download/Install the latest Tomcat 9 server

sources:

- https://tomcat.apache.org/download-90.cgi
- your system's package manager

## 13. Add Tomcat to Eclipse

1. On Linux: You might need to add your user to the "tomcat" group (e.g. via `sudo usermod -aG tomcat myusername`). Check the permissions on your tomcat-installation/config-directory (e.g. `/usr/share/tomcat/`). Otherwise the integration process might throw an error that it couldn't read the configs.
1. Add Server view: Window >> Show View >> Server. When the server appears there, double clicking it will allow to open the config-GUI.
1. Create Server in Eclipse: File >> New >> Other >> Server
1. Choose Tomcat 9, then press "next" (not "finish")
1. Make sure you use a Java 8 JDK or JRE, not java 9, or tomcat will not start up and throw a JAXB-related exception. <!-- TODO rly? Testing this with OpenJDK 12 -->
1. Add node and owner and click finish
   1. If you do not have the options to add the owner and node application to the tomcat (also accessible via Server >> [your tomcat server] >> Add and Remove), something went wrong.
      1. Make sure to run maven install at least once
         - right-click "webofneeds" in the project explorer >> Run As >> Maven install
         - or in the command-line: `mvn install -P skip-tests`
      1. Make sure the following project facets are active for the won-owner-webapp and won-node-webapp-projects (right-click project >> Properties >> search: "facets" >> activate facets):
         - Dynamic Web Module
         - Java
         - JavaScript
      1. Do an eclipse build (Project >> Build All)
      1. Maybe the import of the webofneeds maven project somehow did not work properly. Delete all imported projects (without deleting the sources), then import again (File >> Import... >> Maven >> Existing Maven Projects )
      1. Maybe you did not install eclipse for Java EE. Check Help >> About Eclipse. If it does not say 'Eclipse Java EE IDE for Web Developers.', the easiest is to download and install Eclipse for Java EE.

## 14. Update Connector-Statement in Tomcat's server.xml

Copy the SSL connector statement given below to `<TOMCAT_FOLDER>/conf/server.xml` as a child of the `<Service name="Catalina">`-node and change the password and key-folders there to values used in previous steps as well (e.g. `"changeit"`, `/home/username/WoNKeystore/t-cert.pem`, `/home/username/WoNKeystore/t-key.pem`). If you already have a `Connector`-statement, modify it accordingly. You can also access the conf in Eclipse if you've already added Tomcat there via Project Explorer >> Server >> "Your Server" >> open `server.xml`.

```xml
<Service name="Catalina">
 ...
 <Connector
   SSLEnabled="true"
   compressibleMimeType="text/html, text/xml, text/plain, text/css, text/javascript, application/javascript, application/x-font-ttf, image/svg+xml, text/turtle, application/rdf+xml, application/x-turtle, text/rdf+n3, application/json, application/trig, application/ld+json, application/n-quads"
   compression="on"
   disableUploadTimeout="true"
   enableLookups="true"
   maxThreads="200"
   minSpareThreads="5"
   port="8443"
   protocol="org.apache.coyote.http11.Http11Nio2Protocol"
   scheme="https"
   secure="true">
     <SSLHostConfig
      certificateVerification="optionalNoCA"
      certificateVerificationDepth="2"
      trustManagerClassName="won.utils.tls.AcceptAllCertsTrustManager"
      protocols="all">
      <Certificate
        certificateKeystoreFile="/home/username/WoNKeystore/t-keystore.jks"
        certificateKeystorePassword="changeit"/>
   </SSLHostConfig>
 </Connector>
```

## 15. Edit the Server Configuration in Eclipse

DoubleClick the server in the "Server View" and select:

### VM arguments

Open launch configuration >> (x)= Arguments >> VM arguments \* append the following, replacing the placeholders `<PROJECT_FOLDER>` to the webofneeds project location and the `<TOMCAT_FOLDER>` to the installation directory of your tomcat:

```
-DWON_CONFIG_DIR="<PROJECT_FOLDER>\webofneeds\conf.local" -Dlogback.configurationFile="<PROJECT_FOLDER>\webofneeds\conf.local\logback.xml"
```

### Add the JSTL jar to classpath

Add the JSTL jar to your tomcat's classpath libs (or tomcat cannot be run in `Serve modules without publishing` mode, see `Server Options` below):

- Open Launch Configuration >> Classpath >> User Entries >> Add External JARs ...
- Navigate to your maven repository (default location: `$HOME/.m2/repository`; if it's not there, look into `$HOME/.m2/settings.xml`)
- find javax\servlet\jstl\1.2\jstl-1.2.jar
- if you don't find it
- build the whole project again via `mvn install -Dmaven.test.skip=true` or eclipse's maven integration (right-click on "webofneeds" >> Run As >> Maven Install).
- try again

### Add the required libs to the classpath

Like above, add all jars in `webofneeds/target/required-libs`. This should include `won-utils-tls-x.y-jar`.

### Server Overview Options

In the Server overview (reachable via double-clicking the server in the servers-tab at the bottom):

- select "Server Locations >> Use workspace metadata".
- Server Options
  - [x] Serve modules without publishing \_(allows for instant effect of changes)\*
  - [x] Publish module contexts to separate XML files
  - [x] Modules auto reload by default
  - [ ] Enable security
  - [ ] Enable tomcat logging
- Publishing: Never publish automatically
- Timeouts: i.e. 180 + 30
- Ports: The ports should be shown for HTTP + SSL

### Options to speed up Startup

Suppress unnecessary tag library (TLD) scans to speed up startup:

- In the eclipse navigator view, open Servers >> Tomcat 9.0 \_(your server config)\*
- edit `catalina.properties`
- replace the value of the multi-line(!) property `tomcat.util.scan.StandardJarScanFilter.jarsToSkip` such that the line reads, `tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar`
- replace the value of the multi-line(!) property `tomcat.util.scan.StandardJarScanFilter.jarsToScan` such that the line reads, `tomcat.util.scan.StandardJarScanFilter.jarsToScan=jstl-1.2.jar`

Allow both webapps (owner-webapp and node-webapp) to start simultaneously:

- In the eclipse navigator view, open Servers >> Tomcat 8.0 \_(your server config)\*
- edit `server.xml`
- find the xml element `<Host appBase="webapps" ...` and add the xml attribute `startStopThreads="2"`

## 16. Generate cryptographic keys

**NOTE:** The following guide assumes you want to run all services on the same machine. If you deploy these on different machines, use the respective IPs and Paths as suited/desired.

1. We use the Apache Portable Runtime (APR) in the security modules of the Web of Needs. Normally, APR is included in the Tomcat by default, but it could happen (e.g. when you use the Tomcat for Windows installer) that it's not included. To be sure that you have APR, check your `<TOMCAT_FOLDER>/bin`. A DLL file called `tcnative-1.dll` MUST be there (if you're on Windows). On Linux it exist occur globally e.g. in `/usr/local/apr/lib/libtcnative-1.a`

   - At least on Ubuntu 18 you might need to compile it for yourself, see the [Ubuntu-installation notes](./ubuntu-installation-notes#building-tcnative)
   - **NOTE:** After all the configurations below are done, if you have problems starting the tomcat (cannot be started, or started with the default keys, or complains about key format), most probably the APR library is not set up. Check Tomcat documenentation for OS specific setup, e.g. http://tomcat.apache.org/tomcat-9.0-doc/apr.html. For Mac OS users this is helpful: http://mrhaki.blogspot.co.at/2011/01/add-apr-based-native-library-for-tomcat.html (Mac Ports have to be installed)

2. In the console navigate to the folder for the keystore created in previous steps (e.g. `/home/username/WoNKeystore/`), adapt(!) and run the following lines:

   ```sh
   openssl req -x509 -newkey rsa:2048 -keyout t-key.pem -out t-cert.pem  -passout pass:changeit -days 365 -subj "/CN=myhost.mydomain.com"

   openssl pkcs12 -export -out sometmpfile_deletme -passout pass:changeit -inkey t-key.pem -passin pass:changeit -in t-cert.pem

   "$JAVA_HOME/bin/keytool(.exe)" -importkeystore -srckeystore sometmpfile_deletme -srcstoretype pkcs12 -destkeystore t-keystore.jks -deststoretype JKS -srcstorepass changeit  -deststorepass changeit

   rm sometmpfile_deletme
   ```

   **NOTE:** the openssl commands can be executed in windows using cygwin or the git bash  
   **NOTE:** If you're getting the error message `Subject does not start with '/'.`, change last parameter to `-subj "//CN=myhost.mydomain.com"`

3. The other key stores, and the trust stores are created and filled in automatically when the application is run (in the locations defined in step 4 with the passwords defined in step 5).

4. For Oracle Java 8: Depending on your java-setup it might not be able to generate keys of a relevant length. In that case, you need to download and install the [Java Cryptography Extension](https://www.oracle.com/java/technologies/jce8-downloads.html). There's a readme in the zip detailing its setup. At the time of this writing, this consists of copying the two jars into `$JAVA_HOME/(jre/)lib/security`. If you don't do this or the jars are in the wrong folder, you'll get an exception like `java.security.InvalidKeyException: Illegal key size` when trying to run the app. Check out step 3 [here](https://www.baeldung.com/java-bouncy-castle) for a more detailed guide. In OpenJDK the unlimted cryptography policy should be enabled by default.

**NOTE:** If you're re-deploying the project and want to use different digital certificates than previously used (e.g. by generate new ones according to step 8, or use your server certificates certified by a CA): in addition to replacing corresponding server certificate files and the broker's key store, you have to also delete (or empty) all the trust-store files used by the applications. Also, if you have deleted or replaced the owner keystore, and your deployment uses something other than the in-memory database, you have to empty this database as well -- the stored data related to owner registration at node has to be deleted.

**NOTE:** If you're running docker containers:

Deploy sripts for building and running web of needs as docker containsers (see `webofneeds/webofneeds/won-docker/deploy*.sh`) include building and running server/broker certificate generation container `gencert`. It generates self-signed certificates for the server with the specified name (or IP) protected with the specified password if there are no certificates already present in the mounted volume. The generated key and certificate are in pem format (for Tomcat server) and java keystore format (for broker). If you already have server certificate for your server, and they are in the required format, you don't need to run the `gencert`. If you do want to generate and use self-signed certificate for your web of needs deployment, run it with changed parameters (server name and password), so that they correspond to your server name and to your desired password. Make sure that the same volume containing the certificate is mounted to the wonnode or owner container it is intended for. E.g. the location with the certificate of `server.example.at` should be mounted when owner application is run as docker container if the owner is being deployed at `server.example.com`. When using other than default file pathes and passwords for certificates, make sure that the same values are used in application properies files and in `server.xml`.

**NOTE:** Inspecting keystores using `keytool`: owner/node keystores are saved in bouncycastle's UBER format. `keytool -list -v -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider -storetype UBER -storepass <YOUR_KEYSTORE_PASSWORD> -keystore t-keystore.jks`

## 17. Install the bouncycastle security provider and the trust manager

Locate the JRE you are using with eclipse (`Window -> Preferences -> Java -> Installed JREs`).

- Navigate to the `[JRE]/lib/security` folder
- look for the `java.security` file in your JDK's folder structure (for Oracle Java it should be in `$JAVA_HOME/lib/security`, in OpenJDK-12 it was in `$JAVA_HOME/conf/security`)
- edit the file `java.security` <!--TODO for openjdk it's in [JDK]/conf/security/-->
- find the `List of providers and their preference orders`, which looks like this:

  ```
  security.provider.1=sun.security.provider.Sun
  security.provider.2=sun.security.rsa.SunRsaSign
  security.provider.3=sun.security.ec.SunEC
  ...
  ```

- add this line (replace `11` with the last number in the list plus one)
  ```
  security.provider.11=org.bouncycastle.jce.provider.BouncyCastleProvider
  ```

<!--
TODO dunno if we need the fully qualified package name. in openjdk12 only the class names are used
TODO I've also copied the jars to tomcat's lib folder (at no avail tho)
-->

- For Java 8: the contents of `[won-checkout-dir]/webofneeds/webofneeds/target/required-libs/` (which will be there after the first build) to the `[JRE]/lib/ext/` folder
- For Java >=9: add the contents of `[won-checkout-dir]/webofneeds/webofneeds/target/required-libs/` to the front of your class-path (support for `lib/ext` has been dropped with java 9). If you're using Eclipse for the build, you can do so via "[Run Button] >> Run Configurations >> [tomcat run config name] >> Classpath >> select 'Bootstrap Entries' >> Add JARs"

(if you miss this step, you'll see BC exceptions when running the owner/node)

## 18. Start server

Select Server in the "Servers"-Tab at the bottom and click play (or right-click the server and press "play" in the context-menu)

# Troubleshooting

## Tomcat complains about Missing Keystore

```
SEVERE [main] org.apache.tomcat.util.net.jsse.JSSESocketFactory.getStore Failed to load keystore type JKS with path C:\Users\[username]/.keystore due to C:\Users\[username]\.keystore
 java.io.FileNotFoundException: C:\Users\[username]\.keystore
```

Reason: we use tomcat APR. This means that in `conf/server.xml`, the following line must be present:

```xml
<Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
```

and the apache tomcat native library needs to be installed. On Windows, this means you find the file `tcnative-1.dll` in tomcats `lib` folder, for more information, see <http://tomcat.apache.org/native-doc/>.

On linux you might need to build it for yourself, see "Ubuntu Installation Notes - Building TCNative](./ubuntu-installation-notes.md#building-tcnative)

## Maven build fails with NoClassDefFoundException: java/sql/SQLException

Turns out that you need to have a JDK installed. Download a recent JDK and tell eclipse to use that one by default: `Window >> Preferences >> Java >> Installed JREs`. For me, Jdk11 did not work, but Jdk8u202 did.

## Out of memory error

If you run your project and encounter a "Out of memory" error you should probably add this to your run configuration: `-XX:MaxPermSize=128M`. This should only be necessary if you use JDK 1.8.

## icu4j: Invalid byte tag in constant pool

If you get a compile error like below you can just ignore it. There is a corrupt `.class` file in the maven dependencies and will go away with future updates and everything is OK untill you use it, but you won't. This will only affect Chinese speaking users since it is the `LocaleElements_zh__PINYIN.class` file for Pinyin.

Another possibility is to find the icu4j `.jar` file and delete the `LocaleElements_zh__PINYIN.class` file. This is a quick and dirty hack, but it works.

```
Apr 16, 2013 2:07:59 PM org.apache.catalina.startup.ContextConfig processAnnotationsJar
SEVERE: Unable to process Jar entry [com/ibm/icu/impl/data/LocaleElements_zh__PINYIN.class] from Jar [jar:file:/C:/DATA/atus/Code/webofneeds/webofneeds/won-node-webapp/target/won/WEB-INF/lib/icu4j-2.6.1.jar!/] for annotations
org.apache.tomcat.util.bcel.classfile.ClassFormatException: Invalid byte tag in constant pool: 60
at org.apache.tomcat.util.bcel.classfile.Constant.readConstant(Constant.java:133)
at org.apache.tomcat.util.bcel.classfile.ConstantPool.<init>(ConstantPool.java:60)
```

More info about this error can be found at:

- http://stackoverflow.com/questions/6751920/tomcat-7-servlet-3-0-invalid-byte-tag-in-constant-pool
- http://maven.40175.n5.nabble.com/Problem-when-mvn-site-site-Generating-quot-Dependencies-quot-report-td113470.html
- http://jira.codehaus.org/browse/MPIR-142

## won.protocol.exception.RDFStorageException: Could not create File!

This means that Tomcat could not access the temp directory where it stores the `.ttl` files. This is either the `TMP` global variable or the `%tomcat_dir%/tmp` directory. The problem is that if you're on Windows and installed Tomcat in `C:/Program Files/`. Unless you are an admin you will not have access to any of these.

You can either grant your user access to one of these directories or change the directory in `won-node/src/main/resources.localhost/` by changing the `rdf.file.path`.

Check the error message for the actual directory in question. Expect something like

```
Caused by: java.io.FileNotFoundException: C:\Program Files\apache-tomcat-7.0.35\temp\1.ttl (Access denied)
```

## Port Bind Problem - Failed to initialize end point associated with ProtocolHandler

```
org.apache.coyote.AbstractProtocol.init Failed to initialize end point associated with ProtocolHandler ["http-apr-8443"]
```

This problem or other similar errors that can be referred as "port bind problems" are causd by setting the port that is used by WoN-node or WoN-webapp in the Tomcat Server Settings of IntelliJ. Leave the field "HTTP port" in the "Run/Debug Configurations" free (the default is 8080 and does not cause any problem).

## maven dies in won-owner-webapp during 'clean' task

stating that 'node' was not found.
solution: run `mvn install` before `mvn clean`

## java.security.NoSuchProviderException: no such provider: BC

Make sure you've gone through the steps in ["Install the bouncycastle security provider and the trust manager"](#17-install-the-bouncycastle-security-provider-and-the-trust-manager).

If this error still occurs, it could happen that Tomcat can't find or access the the bc `.jar` files during startup. Below is a collection of actions that may fix the problem and places to copy the `.jar` files into. You may want to try them both separately and combined to find a setup that works for you.

You should be able to find both `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` in your maven directory (default location is: `C:/Users/[user name]/.m2/repository/org/bouncycastle/`). If not, build the whole project with `mvn install -Dmaven.test.skip=true` and check again.

- in Eclipse, edit the server launch configuration properties (accessible via "Run As...") and add both `.jar` files as External JARs to User Entries (suggested in: [Building, Step "Add the JSTL jar to classpath"](https://github.com/researchstudio-sat/webofneeds/blob/master/documentation/building.md#add-the-jstl-jar-to-classpath))
- copy both `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` to `%tomcat_dir%/lib/` (suggested in: [Crytpographic Keys and Certificates](https://github.com/researchstudio-sat/webofneeds/blob/5dc0db3747c201a87d94621453b8b898a34e7fc4/documentation/installation-cryptographic-keys-and-certificates.md), Step 11)
- copy both `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` to `C:/Program Files/Java/[jre dir]/lib/ext/` and [install the bouncy castle security provider](http://www.bouncycastle.org/wiki/display/JA1/Provider+Installation) (suggested in: [issue#1393](https://github.com/researchstudio-sat/webofneeds/issues/1393))
- In the Tomcat server's `server.xml`, find the xml element `<Host appBase="webapps" ...` and add the xml attribute `startStopThreads="2"`
  - **NOTE:** This only has an effect if two or more webapps, e.g. a node and an owner, are started on the server.

## Exception in Owner-Webapp log: PKIX path building failed

One possible cause of this is that the [certificate renewal](/documentation/letsencrypt.md#certificate-renewal) updated the pem files but did not update the jks and pfx files. The consequence is that the node webapp uses the new key (as nginx loads the pem file) and the activemq server uses the old key (as it loads the jks file).

_To check if this is the problem:_ list the keys in the jks/pfx file (using `keytool -list -v -keystore t-keystore.pfx`) and compare them to the key information available for, eg. `https://{your-won-node}/won/resource` in the browser or some other http client. If the keys are the same, this is _not_ the problem.

_To fix this:_ overwrite the jks and pfx files by exporting the key from the pem file. This can be done by executing the `openssl`and `keytool` commands in the letsencrypt's container script [certificate-request-and-renew.sh](/webofneeds/won-docker/image/letsencrypt/certificate-request-and-renew.sh)
