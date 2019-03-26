# Build WoN in Eclipse:

### Eclipse:

1.  Download **Eclipse Oxygen: Java EE**:
    * either get the portable version here: https://www.eclipse.org/downloads/eclipse-packages/ » Eclipse IDE **for Enterprise Java Developers** » 64bit. 
    * or use this direct link to the currently latest zip: https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/oxygen/1a/eclipse-jee-oxygen-1a-win32-x86_64.zip
2.  Install/unzip eclipse to a folder (e.g. `C:\DATA\DEV\...`)
6.  Add `-clean -Xms512m -Xmx1024m` to the `.exe` shortcut
7.  Clone project with git (e.g. to `C:\DATA\DEV\workspace`). It’s easier not to do this in eclipse, but with the git bash or gui)
8. Import project in eclipse: File >> Import >> Existing Maven Project >> point to `pom.xml`
9. Deactivate "autobuild": Window >> Preferences >> General >> Workspace >> uncheck "Build automatically"
10. Change "spaces for tabs" settings: 
    * General: Window >> Preferences >> General >> Editors >> Text Editors >> Check `Insert spaces for tabs`
    * JavaScript: Window >> Preferences >> Javascript >> Code Style >> Formatter >> Edit >> Indentation >> Tab policy >> choose `spaces only`
        * Set `Indentation size` + `Tab size`: `4`
        * Rename the formatter settings profile to save it
11. Set maven profiles: right-click the webofneeds project in the package explorer >> Maven >> Select Maven Profiles. check `skip-tests`
12. If you develop on Windows you will need to setup `node`s `windows-build-tools` (see [this guide](./installation-setting-up-frontend-development-environment.md#installing-windows-build-tools-on-windows))

### Preparation: generate cryptographic keys
Follow the [instructions for generating your keys](https://github.com/researchstudio-sat/webofneeds/blob/master/documentation/installation-cryptographic-keys-and-certificates.md). The result will be a `t-keystore.jks` file somewhere in your filesystem.

### Tomcat integration:
0.  Download/Install the latest Tomcat 9 server
1.  Create Server in Eclipse: File >> New >> Other >> Server
2.  Choose Tomcat 9, then press "next" (not "finish")
3.  Make sure you use a Java 8 JDK or JRE, not java 9, or tomcat will not start up and throw a JAXB-related exception.
4.  Add node and owner and click finish 
      1. If you do not have the options to add the owner and node application to the tomcat (also accessible via Server >> [your tomcat server] >> Add and Remove), something went wrong.
            1. Maybe you did not install eclipse for Java EE. Check Help >> About Eclipse. If it does not say 'Eclipse Java EE IDE for Web Developers.', the easiest is to download and install Eclipse for Java EE.
            2. Maybe the import of the webofneeds maven project somehow did not work properly. Delete all imported projects (without deleting the sources), then import again (File >> Import... >> Maven >> Existing Maven Projects )
5.  Add Server view: Window >> Show View >> Server
6.  Change server.xml: In Project Explorer >> Server >> "Your Server" >> open `server.xml` and add the following xml snippet.

**Note:** the keystore mentioned here is the one you generated earlier in the *Preparation* step.
```xml
        <Connector 
    port="8443"
    protocol="org.apache.coyote.http11.Http11Nio2Protocol"
    SSLEnabled="true"
    maxThreads="200"
    compressibleMimeType="text/html, text/xml, text/plain, text/css, text/javascript, application/javascript, application/x-font-ttf, image/svg+xml, text/turtle, application/rdf+xml, application/x-turtle, text/rdf+n3, application/json, application/trig, application/ld+json, application/n-quads"
    compression="on" 
    disableUploadTimeout="true" 
    enableLookups="true"
    maxPostSize="5242880000" 
    maxSpareThreads="75"
    minSpareThreads="5"  
    scheme="https"
    secure="true">
    <SSLHostConfig 
            certificateVerification="optionalNoCA"
            certificateVerificationDepth="2"
            trustManagerClassName="won.utils.tls.AcceptAllCertsTrustManager"
            protocols="all">
            <Certificate 
                         certificateKeystoreFile="c:/certs/t-keystore.jks"
                         certificateKeystorePassword="changeit"/>
        </SSLHostConfig>
        
  </Connector>
        ...
        </Service>
  ```

        
7.  Edit server configuration: DoubleClick the server in the "Server View" and select:
	*  Open launch configuration >> (x)= Arguments >> VM arguments
		* append the following, replacing the placeholders `<PROJECT_FOLDER>` to the webofneeds project location and the `<TOMCAT_FOLDER>` to the installation directory of your tomcat: `-XX:PermSize=512m -XX:MaxPermSize=512m -DWON_CONFIG_DIR="<PROJECT_FOLDER>\webofneeds\conf.local" -Dlogback.configurationFile="<PROJECT_FOLDER>\webofneeds\conf.local\logback.xml"` 
	*  Add the JSTL jar to your tomcat's classpath libs (or tomcat cannot be run in `Serve modules without publishing` mode, see `Server Options` below):
		*  Open Launch Configuration >> Classpath >> User Entries >> Add External JARs ... 
		* Navigate to your maven repository (default location: [user home]\.m2\repository; if it's not there, look into [user home]\.m2\settings.xml)
		* find javax\servlet\jstl\1.2\jstl-1.2.jar
		* if you don't find it
			* build the whole project with `mvn install -Dmaven.test.skip=true`
			* try again   
	* Like above, add another classpath entry: the `won-utils-tls-x.y-jar`, which is generated into `webofneeds/target/required-libs` by the maven build
	*  Server Locations: Use Workspace Metadata
	*  Server Options
		* [x] Serve modules without publishing *(allows for instant effect of changes)*
		* [x] Publish module contexts to separate XML files
		* [x] Modules auto reload by default
		* [ ] Enable security
		* [ ] Enable tomcat logging
	*  Publishing: Never publish automatically
	*  Timeouts: i.e. 180 + 30
	*  Ports: The ports should be shown for HTTP + SSL
	*  Suppress unnecessary tag library (TLD) scans: *(speeds up server startup)*
		*  In the eclipse navigator view, open Servers >> Tomcat 8.0 *(your server config)*
		*  edit `catalina.properties`
		*  replace the value of the multi-line(!) property `tomcat.util.scan.StandardJarScanFilter.jarsToSkip` such that the line reads, `tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar`
		*  replace the value of the multi-line(!) property  `tomcat.util.scan.StandardJarScanFilter.jarsToScan`such that the line reads, `tomcat.util.scan.StandardJarScanFilter.jarsToScan=jstl-1.2.jar`
	*  Allow both webapps (owner-webapp and node-webapp) to start simultaneously: *(speeds up server startup)* 
		*  In the eclipse navigator view, open Servers >> Tomcat 8.0 *(your server config)* 
		*  edit `server.xml` 
		*  find the xml element `<Host appBase="webapps" ...` and add the xml attribute `startStopThreads="2"` 

8.  Install the bouncycaslte security provider: Locate the JRE you are using with eclipse (`Window -> Preferences -> Java -> Installed JREs`). 
	* Navigate to the `[JRE]/lib/security` folder
	* edit the file `java.security`
	* find the `List of providers and their preference orders`, which looks like this:

	```
	security.provider.1=sun.security.provider.Sun
	security.provider.2=sun.security.rsa.SunRsaSign
	security.provider.3=sun.security.ec.SunEC
	...
	```

	* add this line (replace `11` with the last number in the list plus one)
	
	```
	security.provider.11=org.bouncycastle.jce.provider.BouncyCastleProvider
	```

	* copy `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` from `[won-checkout-dir]/webofneeds/webofneeds/target/required-libs/` (which will be there after the first build) to the `[JRE]/lib/ext/` folder
9.  Start server
10.  Run the gulpfile outside eclipse: `npm run build` in `webofneeds/won-owner-webapp/src/main/webapp`, refresh the `won-owner-webapp` in eclipse (F5), click on the server –> "Publish"
