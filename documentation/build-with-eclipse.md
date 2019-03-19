# Build WoN in Eclipse:

### Eclipse:

1.  Download **Eclipse Oxygen: Java EE**:
    * either get the portable version here: https://www.eclipse.org/downloads/eclipse-packages/ » Eclipse IDE **for Enterprise Java Developers** » 64bit. 
    * or use this direct link to the currently latest zip: https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/oxygen/1a/eclipse-jee-oxygen-1a-win32-x86_64.zip
2.  Install/unzip eclipse to a folder (e.g. `C:\DATA\DEV\...`)
6.  Add `-clean -Xms512m -Xmx1024m` to the `.exe` shortcut
7.  Clone project with git (e.g. to `C:\DATA\DEV\workspace`). It’s easier not to do this in eclipse, but with the git bash or gui)
8.  Import project in eclipse: File >> Import >> Existing Maven Project >> point to `pom.xml`
9. Deactivate "autobuild": Window >> Preferences >> General >> Workspace >> uncheck "Build automatically"
10. Ideykeyscheme: https://code.google.com/archive/p/ideakeyscheme/
    *  Add jar file to `eclipse\plugins` folder. 
    *  Restart Eclipse. 
    *  Open Window >> Preferences >> General >> Keys and select the scheme "Intellij Idea".
11. Change "spaces for tabs" settings: 
    * General: Window >> Preferences >> General >> Editors >> Text Editors >> Check `Insert spaces for tabs`
    * JavaScript: Window >> Preferences >> Javascript >> Code Style >> Formatter >> Edit >> Indentation >> Tab policy >> choose `spaces only`
        * Set `Indentation size` + `Tab size`: `4`
        * Rename the formatter settings profile to save it
12. Set maven profiles: right-click the webofneeds project in the package explorer >> Maven >> Select Maven Profiles. check `skip-tests`
13. If you develop on Windows you will need to setup `node`s `windows-build-tools` (see [this guide](./installation-setting-up-frontend-development-environment.md#installing-windows-build-tools-on-windows))

### Tomcat integration:

1.  Create Server: File >> New >> Other >> Server
2.  Choose Tomcat, then press "next" (not "finish")
3.  Make sure you use a Java 8 JDK or JRE, not java 9, or tomcat will not start up and throw a JAXB-related exception.
4.  Add node and owner and click finish 
      1. If you do not have the options to add the owner and node application to the tomcat (also accessible via Server >> [your tomcat server] >> Add and Remove), something went wrong.
            1. Maybe you did not install eclipse for Java EE. Check Help >> About Eclipse. If it does not say 'Eclipse Java EE IDE for Web Developers.', the easiest is to download and install Eclipse for Java EE.
            2. Maybe the import of the webofneeds maven project somehow did not work properly. Delete all imported projects (without deleting the sources), then import again (File >> Import... >> Maven >> Existing Maven Projects )
5.  Add Server view: Window >> Show View >> Server
6.  Change server.xml: In Project Explorer >> Server >> "Your Server" >> open `server.xml` and add
```xml
        <Service name="Catalina">
        ...
        <Connector 
                SSLCertificateFile="<PATH TO SERVER CERTS>/won-server-certs/t-cert.pem" 
                SSLCertificateKeyFile="<PATH TO SERVER CERTS>/won-server-certs/t-key.pem" 
                SSLEnabled="true" 
                SSLPassword="changeit" 
                SSLVerifyClient="optionalNoCA" 
                SSLVerifyDepth="2" 
                acceptCount="100" 
                clientAuth="false" 
                compressableMimeType="                                      
                                        text/html,                                                               
                                        text/xml,                                      
                                        text/plain,                                      
                                        text/css,                                      
                                        text/javascript,                                      
                                        application/javascript,                                                                                                                 
                                        application/x-font-ttf,                                      
                                        image/svg+xml,                                                                           
                                        text/turtle,                                                                           
                                        application/rdf+xml,                                                                           
                                        application/x-turtle,                                                                           
                                        text/rdf+n3,                                                                           
                                        application/json,                                                                            
                                        application/trig,                                                                            
                                        application/ld+json,                                                                            
                                        application/n-quads" 
                compression="on" 
                disableUploadTimeout="true" 
                enableLookups="true" 
                keystoreFile="<PATH TO CLIENT CERTS>\client-certs\owner-keys.jks" 
                keystorePass="temp" 
                maxPostSize="5242880000" 
                maxSpareThreads="75" 
                maxThreads="200" 
                minSpareThreads="5" 
                port="8443" 
                scheme="https" 
                secure="true" 
                sslProtocol="TLS"
                />
        ...
        </Service>
  ```
  
**NOTE: replace with your own certificate path for server and client certificate locations if necessary**
        
7.  Edit server configuration: DoubleClick the server in the "Server View" and select:
	*  Open launch configuration >> (x)= Arguments >> VM arguments
		* append the following, replacing the placeholders `<PROJECT_FOLDER>` to the webofneeds project location and the `<TOMCAT_FOLDER>` to the installation directory of your tomcat: `-Djava.library.path="<TOMCAT_FOLDER>\bin" -XX:PermSize=512m -XX:MaxPermSize=512m -DWON_CONFIG_DIR="<PROJECT_FOLDER>\webofneeds\conf.local" -Dlogback.configurationFile="<PROJECT_FOLDER>\webofneeds\conf.local\logback.xml"` 
	*  Add the JSTL jar to your tomcat's classpath libs (or tomcat cannot be run in `Serve modules without publishing` mode, see `Server Options` below):
		*  Open Launch Configuration >> Classpath >> User Entries >> Add External JARs ... 
		* Navigate to your maven repository (default location: [user home]\.m2\repository; if it's not there, look into [user home]\.m2\settings.xml)
		* find javax\servlet\jstl\1.2\jstl-1.2.jar
		* if you don't find it
			* build the whole project with `mvn install -Dmaven.test.skip=true`
			* try again      
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
8.  Follow instructions on https://github.com/researchstudio-sat/webofneeds/blob/5dc0db3747c201a87d94621453b8b898a34e7fc4/documentation/installation-cryptographic-keys-and-certificates.md and make sure that you have the `tcnative-1.dll` **in your tomcat's `bin/`-folder!**, and that you correctly point to it with the `-Djava.library.path` variable (Step 7). Otherwise you will get `InvalidKeystoreFormatException`s at server startup and an info message which says `The APR based Apache Tomcat Native library which allows optimal performance in production environments was not found on the java.library.path` => following path where to put the .dll 
9.  Install the bouncycaslte security provider: Locate the JRE you are using with eclipse (`Window -> Preferences -> Java -> Installed JREs`). 
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
10.  Start server
11.  Run the gulpfile outside eclipse: `npm run build` in `wepapp`, refresh the `won-owner-webapp` in eclipse (F5), click on the server –> "Publish"
