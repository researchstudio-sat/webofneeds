- [1. Java SDK version 8 or newer](#1-java-sdk-version-8-or-newer)
- [2. Eclipse for Java Enterprise Developers (4.7 Oxygen or newer)](#2-eclipse-for-java-enterprise-developers-47-oxygen-or-newer)
- [3. Install Maven (3.3 or newer)](#3-install-maven-33-or-newer)
- [4. Git Clone to Your Eclipse Workspace](#4-git-clone-to-your-eclipse-workspace)
- [5. Import the Maven-project in Eclipse](#5-import-the-maven-project-in-eclipse)
- [6. Deactivate "autobuild"](#6-deactivate-%22autobuild%22)
- [7. Use the provided code style](#7-use-the-provided-code-style)
- [8. Set maven profile `skip-tests`](#8-set-maven-profile-skip-tests)
- [9. Maven install](#9-maven-install)
- [10. On Windows: <PROJECT_ROOT>/webofneeds/won-owner-webapp/src/main/webapp/node/npm -g windows-build-tools](#10-on-windows-projectrootwebofneedswon-owner-webappsrcmainwebappnodenpm--g-windows-build-tools)
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

For Windows, there's [Git SCM](https://git-scm.com/download/win) that also installs the "git bash" command-line-terminal (which is in fact Cygwin and generally works like a Linux bash). On Linux git should already be installed or is most certainly available via your package manager.

You can then check out the repository from a command-line terminal in the following two ways:

- Using SSH (only needs you to unlock your SSH key when pulling/pushing; but requires that you've added your public key to your Github account):
  ```
  cd <PATH_TO_YOUR_ECLIPSE_WORKSPACE>
  git clone git@github.com:researchstudio-sat/webofneeds.git
  ```
- Using HTTPS:
  ```
  cd <PATH_TO_YOUR_ECLIPSE_WORKSPACE>
  git clone https://github.com/researchstudio-sat/webofneeds.git
  ```
- Alternatively, Eclipse includes a graphical git client, that you can use to check out the repository.

## 5. Import the Maven-project in Eclipse

File >> Import >> Existing Maven Project >> select the (folder with) the pom.xml `webofneeds/pom.xml`

Troubleshooting: If you don't have the "Existing Maven Project"-option, make sure you have the addons mentioned in the [eclipse section above](#2-eclipse-java-ee).

## 6. Deactivate "autobuild"

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

## 10. On Windows: <PROJECT_ROOT>/webofneeds/won-owner-webapp/src/main/webapp/node/npm -g windows-build-tools

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

1. We use the Apache Portable Runtime (APR) in the security modules of the Web of Needs. Normally, APR is included in the Tomcat by default, but it could happen (e.g. when you use the Tomcat for Windows installer) that it's not included. To be sure that you have APR, check your `<TOMCAT_FOLDER>/bin`. A DLL file called `tcnative-1.dll` MUST be there (if you're on Windows). On Linux it exist occur globally e.g. in `/usr/local/apr/lib/libtcnative-1.a` **NOTE:** After all the configurations below are done, if you have problems starting the tomcat (cannot be started, or started with the default keys, or complains about key format), most probably the APR library is not set up. Check Tomcat documenentation for OS specific setup, e.g. http://tomcat.apache.org/tomcat-9.0-doc/apr.html. For Mac OS users this is helpful: http://mrhaki.blogspot.co.at/2011/01/add-apr-based-native-library-for-tomcat.html (Mac Ports have to be installed)

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
