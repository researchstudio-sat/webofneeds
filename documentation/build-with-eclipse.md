# Build WON in Eclipse:

### Eclipse:

1.  Download **Eclipse Oxygen: Java EE**:
    * either get the portable version here: https://www.eclipse.org/downloads/eclipse-packages/ » Eclipse IDE **for Java EE Developers** » 64bit. 
    * or use this direct link to the currently latest zip: https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/oxygen/1a/eclipse-jee-oxygen-1a-win32-x86_64.zip
2.  Install/unzip eclipse to a folder (e.g. `C:\DATA\DEV\...`)
3.  Start Eclipse
4.  Install Spring: Help >> Eclipse Marketplace >> Search and install `Spring Tool Suite`
5.  Restart Eclipse
6.  Add `-clean -Xms512m -Xmx1024m` to the `.exe` shortcut
7.  Clone project with git (e.g. to `C:\DATA\DEV\workspace`). It’s easier not to do this in eclipse, but with the git bash or gui)
8.  Import project in eclipse: File >> Import >> Existing Maven Project >> point to `pom.xml`
9. Deactivate "autobuild": Window >> Preferences >> General >> Workspace >> uncheck "Build automatically"
10. Ideykeyscheme: https://code.google.com/archive/p/ideakeyscheme/
    *  Add jar file to `eclipse\plugins` folder. 
    *  Restart Eclipse. 
    *  Open Window → Preferences → General → Keys and select the scheme "Intellij Idea".
11. Change "spaces for tabs" settings: Window >> Preferences >> General >> Editors >> Text Editors >> Check "Insert spaces for tabs"
12. Set maven profiles: right-click the webofneeds project in the package explorer >> Maven >> Select Maven Profiles. check 'skip-tests'

### Tomcat integration:

1.  Create Server: File >> New >> Other >> Server
2.  Choose Tomcat 
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
    *  "Open launch configuration" >> (x)= Arguments >> VM arguments >> add 
            `-Djava.library.path="<TOMCAT_FOLDER>\bin" -XX:PermSize=512m -XX:MaxPermSize=512m -DWON_CONFIG_DIR="<PROJECT_FOLDER>\webofneeds\conf.local" -Dlogback.configurationFile="<PROJECT_FOLDER>\webofneeds\conf.local\logback.xml"` (Note: Change `<PROJECT_FOLDER>` to the webofneeds project location and the `<TOMCAT_FOLDER>` to the installation directory of your tomcat.)
    *  Server Locations: "Use Tomcat installation (takes control of tomcat installation)"
    *  Server Options: Publish module… + Modules auto reload…
    *  Publishing: Never publish automatically
    *  Timeouts: i.e. 180 + 30
    *  Ports: The ports should be shown for HTTP + SSL
8.  Follow instructions on https://github.com/researchstudio-sat/webofneeds/blob/5dc0db3747c201a87d94621453b8b898a34e7fc4/documentation/installation-cryptographic-keys-and-certificates.md and make sure that you have the `tcnative-1.dll` **in the tomcat bin folder!**, and that you correctly point to it with the `-Djava.library.path` varialbe (Step 7). Otherwise you will get Invalid KeystoreFormat Exceptions at server startup and an info message which says "The APR based Apache Tomcat Native library which allows optimal performance in production environments was not found on the java.library.path => following path where to put the .dll" 
9. Add the bouncy castle libraries `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` to "Open launch configuration" >> "Classpath" as "Add External JARs..." and to the tomcat lib folder
10.  Start server
11.  Run the gulpfile outside eclipse: `npm run build` in `wepapp`, refresh the `won-owner-webapp` in eclipse (F5), click on the server –> "Publish"
