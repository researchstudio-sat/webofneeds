# Build WON in Eclipse:

### Eclipse:

1.  Download Eclipse Oxygen: Java EE:
    * either get the portable version here: https://www.eclipse.org/downloads/eclipse-packages/ » Eclipse IDE for Java EE Developers » 64bit. 
    * or use this direct link to the currently latest zip: https://www.eclipse.org/downloads/download.php?file=/technology/epp/downloads/release/oxygen/1a/eclipse-jee-oxygen-1a-win32-x86_64.zip
3.  Place the Eclipse main folder in `C:\DATA\DEV\...` 
4.  Start Eclipse
5.  Install Spring: Help -> Eclipse Marketplace –> Search and install `Spring Tool Suite`
6.  Restart Eclipse
7.  Add `-clean -Xms512m -Xmx1024m` to the `.exe` shortcut
8.  Clone project with git to `C:\DATA\DEV\workspace` (it’s easier not to do this in eclipse, but with the git bash or gui)
9.  Import project in eclipse: File -> Import -> Existing Maven Project -> point to `pom.xml`
10. Deactivate "autobuild": Window -> Preferences -> General -> Workspace -> uncheck "Build automatically"
11. Ideykeyscheme: https://code.google.com/archive/p/ideakeyscheme/
    *  Add jar file to eclipse/plugins folder. 
    *  Restart Eclipse. 
    *  Open Window → Preferences → General → Keys and select the scheme "Intellij Idea".
12. Change "spaces for tabs" settings: Window -> Preferences -> General -> Editors -> Text Editors -> Check "Insert spaces for tabs"

### Tomcat integration:

1.  Create Server: File –> New -> Other -> Server
2.  Choose Tomcat
3.  Add node + owner
4.  Add Server view: Window -> Show View –> Server
5.  Change server.xml: In Project Explorer -> Server -> "Your Server" -> open `server.xml` and add

        <Service name="Catalina">
        ...
        <Connector 
                SSLCertificateFile="C:/DATA/DEV/workspace/certs/won-server-certs/t-cert.pem" 
                SSLCertificateKeyFile="C:/DATA/DEV/workspace/certs/won-server-certs/t-key.pem" 
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
                keystoreFile="C:\DATA\DEV\workspace\certs\client-certs\owner-keys.jks" 
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
        
6.  Edit server configuration: DoubleClick the server in the "Server View" and select:
    *  Server Locations: "Use Tomcat installation (takes control of tomcat installation)"
    *  Server Options: Check: Publish module… + Modules auto reload…
    *  Publishing: Check: Never Publish automatically
    *  Timeouts: Set higher: i.e. 180 + 30
    *  Ports: The ports should be shown for HTTP + SSL
7.  Follow instructions on https://github.com/researchstudio-sat/webofneeds/blob/5dc0db3747c201a87d94621453b8b898a34e7fc4/documentation/installation-cryptographic-keys-and-certificates.md and make sure to copy the `tcnative-1.dll` into the Java jdk folder and the tomcat! Same with the `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar`
8.  Start server:
9.  Run the gulpfile outside eclipse, refresh the `won-owner-webapp` in eclipse (F5), click on the server –> "Publish"
