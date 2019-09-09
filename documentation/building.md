## Overview

1. Install the [Java SDK](https://openjdk.java.net/) version 8 or newer
2. Get [**Eclipse for Java Enterprise Developers**](https://www.eclipse.org/downloads/packages/) (Eclipse 4.7 Oxygen or newer)
3. Get [Maven](https://maven.apache.org/) (3.3 or newer) and make sure it's on the system `$PATH`.
4. Git clone the project to your eclipse workspace
5. Import the Maven-project in Eclipse: File >> Import >> Existing Maven Project >> select the (folder with) the pom.xml `webofneeds/pom.xml`
6. Deactivate "autobuild": Window >> Preferences >> General >> Workspace >> Build >> uncheck "Build automatically"
7. Use the provided code style: Window >> Preferences >> Java >> Code Style >> Formatter
   1. Click `Import`
   2. Select the file `webofneeds/won-buildtools/src/main/resources/eclipse/formatter.xml`
   3. Click `Apply and Close`
8. Set maven profiles: right-click the webofneeds project in the Project Explorer Tab on the left side >> Maven >> Select Maven Profiles. Check `skip-tests`.
9. If you develop on Windows you will need to setup `node`s `windows-build-tools` for `npm` instance that the owner-application installs.
10. Generate cryptographic keys.
11. Download/Install the latest [Tomcat 9 server](https://tomcat.apache.org/download-90.cgi)
12. Add Tomcat to Eclipse

Right-click "webofneeds" in project explorer >> Run As >> Maven Install

## In More Detail

### 1. Java SDK

Sources:

- Your package manager
- Oracle Java: <https://www.oracle.com/technetwork/java/javase/downloads/index.html>
- Open JDK: <https://openjdk.java.net/>

Things to mind:

- Make sure your `$JAVA_HOME` environment variable points to the folder it's installed in.
- Make sure the Java-Binaries are on the system-path/`$PATH`
  - On Linux-distributions using `alternatives`, check `alternatives --list | grep java` and make sure you have the right SDK selected. You can change the configuration via `sudo alternatives --config <alternative-name>`

### 2. Eclipse Java EE

Sources:

- <https://www.eclipse.org/downloads/packages/> >> "Eclipse for Java Enterprise Developers"
- Your package manager. Check if there's a Java EE version.

If you installed from your package manager, ensure the following addons are also installed:

- Eclipse Java EE Developer Tools
- Eclipse Web Developer Tools
- m2e Maven Integration

You can check in Help >> Install New Software >> "What's already installed". If anything isn't you can select "Work With: All Available Sites" and then search, select and install the missing addons.

### 3. Maven

Sources:

- <https://maven.apache.org/>
- your package manager

If it's installed you should be able to run `mvn -version` on the command-line.

### 4. Git Clone to Your Eclipse Workspace

For Windows, there's [Git SCM](https://git-scm.com/download/win) that also installs the "git bash" command-line-terminal (which is in fact Cygwin and generally works like a Linux bash). On Linux git should already be installed or is most certainly available via your package manager.

You can then check out the repository from a command-line terminal in the following two ways:

1. Using SSH (only needs you to unlock your SSH key when pulling/pushing; but requires that you've added your public key to your Github account):

```
cd <PATH_TO_YOUR_ECLIPSE_WORKSPACE>
git clone git@github.com:researchstudio-sat/webofneeds.git
```

2. Using HTTPS:

```
cd <PATH_TO_YOUR_ECLIPSE_WORKSPACE>
git clone https://github.com/researchstudio-sat/webofneeds.git
```

Alternatively, Eclipse includes a graphical git client, that you can use to check out the repository.

### 5. Import the Maven-project in Eclipse

Troubleshooting: If you don't have the "Existing Maven Project"-option, make sure you have the addons mentioned in the [eclipse section above](#2-eclipse-java-ee).

## 9. Windows Build Tools

Run `mvn install`. When it reaches the owner-application, the maven-frontend plugin will install it's own version of `npm`. Using that run the following with admin permissions:

```
<PROJECT_ROOT>/webofneeds/won-owner-webapp/src/main/webapp/node/npm install -g windows-build-tools
```

If your Windows User does not have admin permissions, installing `windows-build-tools` will unfortunately install them only for that user. (Further info at https://github.com/researchstudio-sat/webofneeds/pull/1743) The Visual Studio build tools are installed correctly however, so you only need to fix your Python installation:

1. Go to https://www.python.org/downloads/ and download and install Python 2.7 (Python 3 will not work!)
2. Locate your Python installation, e.g., `c:\Python27\python.exe`
3. With the **same user that will execute the maven build**, run `npm config set python c:\\Python27\\python.exe` (replace with your actual location, of course, and note the double backslashes).

## 10. Generate cryptographic keys

**NOTE:** The following guide assumes you want to run all services on the same machine. If you deploy these on different machines, use the respective IPs and Paths as suited/desired.

1. We use the Apache Portable Runtime (APR) in the security modules of the Web of Needs. Normally, APR is included in the Tomcat by default, but it could happen (e.g. when you use the Tomcat for Windows installer) that it's not included. To be sure that you have APR, check your `<TOMCAT_FOLDER>/bin`. A DLL file called `tcnative-1.dll` MUST be there (if you're on Windows). On Linux it exist occur globally e.g. in `/usr/local/apr/lib/libtcnative-1.a` **NOTE:** After all the configurations below are done, if you have problems starting the tomcat (cannot be started, or started with the default keys, or complains about key format), most probably the APR library is not set up. Check Tomcat documenentation for OS specific setup, e.g. http://tomcat.apache.org/tomcat-9.0-doc/apr.html. For Mac OS users this is helpful: http://mrhaki.blogspot.co.at/2011/01/add-apr-based-native-library-for-tomcat.html (Mac Ports have to be installed)

2. Copy the webofneeds conf-folder: `cp -r conf conf.local` if you haven't done that already.

3. Change all instances of `localhost` in the configurations to your your ip (or computer name or domain) if you are not going to run node owner or matcher locally.

4. In `conf.local`, edit `matcher-service.properties`, `node.properties` and `owner.properties` and change all instances of keystore/truststore locations to point to a folder where you will store generated keys and certificates (in ), e.g. I used `/home/username/WoNKeystore/`). Do not change the file name, just the path.

5. Change the accompanying passwords to something at least 6-letter long.

6. Copy the SSL connector statement given below to `<TOMCAT_FOLDER>/conf/server.xml` as a child of the `<Service name="Catalina">`-node and change the password and key-folders there to values used in previous steps as well (e.g. `"changeit"`, `/home/username/WoNKeystore/t-cert.pem`, `/home/username/WoNKeystore/t-key.pem`). If you already have a `Connector`-statement, modify it accordingly. You can also access the conf in Eclipse if you've already added Tomcat there via Project Explorer >> Server >> "Your Server" >> open `server.xml`.

**Note:** the keystore mentioned here is the one you generated earlier in the _Preparation_ step.

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

7. In the console navigate to the folder for the keystore created in previous steps (e.g. `/home/username/WoNKeystore/`), adapt(!) and run the following lines:

**NOTE:** the openssl commands can be executed in windows using cygwin or the git bash

**NOTE:** If you're getting the error message `Subject does not start with '/'.`, change last parameter to `-subj "//CN=myhost.mydomain.com"`

```sh
openssl req -x509 -newkey rsa:2048 -keyout t-key.pem -out t-cert.pem  -passout pass:changeit -days 365 -subj "/CN=myhost.mydomain.com"

openssl pkcs12 -export -out sometmpfile_deletme -passout pass:changeit -inkey t-key.pem -passin pass:changeit -in t-cert.pem

"$JAVA_HOME/bin/keytool(.exe)" -importkeystore -srckeystore sometmpfile_deletme -srcstoretype pkcs12 -destkeystore t-keystore.jks -deststoretype JKS -srcstorepass changeit  -deststorepass changeit

rm sometmpfile_deletme
```

8. The other key stores, and the trust stores are created and filled in automatically when the application is run (in the locations defined in step 4 with the passwords defined in step 5).

9. For Oracle Java 8: Depending on your java-setup it might not be able to generate keys of a relevant length. In that case, you need to download and install the [Java Cryptography Extension](https://www.oracle.com/java/technologies/jce8-downloads.html). There's a readme in the zip detailing its setup. At the time of this writing, this consists of copying the two jars into `$JAVA_HOME/(jre/)lib/security`. If you don't do this or the jars are in the wrong folder, you'll get an exception like `java.security.InvalidKeyException: Illegal key size` when trying to run the app. Check out step 3 [here](https://www.baeldung.com/java-bouncy-castle) for a more detailed guide. In OpenJDK the unlimted cryptography policy should be enabled by default.

10. Install the bouncycastle security provider and the trust manager: Locate the JRE you are using with eclipse (`Window -> Preferences -> Java -> Installed JREs`). 

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

11. Restart Eclipse if you're using it for the deploy.

**NOTE:** If you're re-deploying the project and want to use different digital certificates than previously used (e.g. by generate new ones according to step 8, or use your server certificates certified by a CA): in addition to replacing corresponding server certificate files and the broker's key store, you have to also delete (or empty) all the trust-store files used by the applications. Also, if you have deleted or replaced the owner keystore, and your deployment uses something other than the in-memory database, you have to empty this database as well -- the stored data related to owner registration at node has to be deleted.

**NOTE:** If you're running docker containers:

Deploy sripts for building and running web of needs as docker containsers (see `webofneeds/webofneeds/won-docker/deploy*.sh`) include building and running server/broker certificate generation container `gencert`. It generates self-signed certificates for the server with the specified name (or IP) protected with the specified password if there are no certificates already present in the mounted volume. The generated key and certificate are in pem format (for Tomcat server) and java keystore format (for broker). If you already have server certificate for your server, and they are in the required format, you don't need to run the `gencert`. If you do want to generate and use self-signed certificate for your web of needs deployment, run it with changed parameters (server name and password), so that they correspond to your server name and to your desired password. Make sure that the same volume containing the certificate is mounted to the wonnode or owner container it is intended for. E.g. the location with the certificate of `server.example.at` should be mounted when owner application is run as docker container if the owner is being deployed at `server.example.com`. When using other than default file pathes and passwords for certificates, make sure that the same values are used in application properies files and in `server.xml`.

**NOTE:** Inspecting keystores using `keytool`: owner/node keystores are saved in bouncycastle's UBER format. `keytool -list -v -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider -storetype UBER -storepass <YOUR_KEYSTORE_PASSWORD> -keystore t-keystore.jks`

## 12. Add Tomcat to Eclipse

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
