# Build WON in Eclipse:

### Eclipse:

1.	Download Eclipse Oxygen: Java EE
2.	https://www.eclipse.org/downloads/download.php?file=/oomph/epp/oxygen/R/eclipse-inst-win64.exe
3.	Place the Eclipse main folder in “C:\DATA\DEV\repo”
4.	Start Eclipse
5.	Install Spring: Help -> Eclipse Marketplace –> Search and install “Spring Tool Suite”
6.	Restart Eclipse
7.	Add “-clean -Xms512m -Xmx1024m” to the .exe short cut
8.	Clone project with git to “C:\DATA\DEV\workspace” (it’s easier not to do this in eclipse, but with the git bash or gui
9.	Import project in eclipse: File -> Import -> Existing Maven Project -> point to pom.xml
10.	Deactivate “autobuild”: Window -> Preferences -> General -> Workspace -> uncheck “Build automatically”
11.	Ideykeyscheme: https://code.google.com/archive/p/ideakeyscheme/
	a.	Add jar file to eclipse/plugins folder. 
	b.	Restart Eclipse. 
	c.	Open Window → Preferences → General → Keys and select the scheme "Intellij Idea".
12.	Change “spaces for tabs” settings: Window -> Preferences -> General -> Editors -> Text Editors -> Check “Insert spaces for tabs”

### Tomcat integration:

1.	Create Server: File –> New -> Other -> Server
2.	Choose Tomcat
3.	Add node + owner
4.	Add Server view: Window -> Show View –> Server
5.	Change server.xml: In Project Explorer -> Server -> Your Server ->paste the new server-xml (need to store it somewhere for us)
6.	Edit server configuration: DoubleClick the server in the “Server View” and select:
	a.	Server Locations: “Use Tomcat installation (takes control of tomcat installation)
	b.	Server Options: Check: Publish module… + Modules auto reload…
	c.	Publishing: Check: Never Publish automatically
	d.	Timeouts: Set higher: i.e. 180 + 30
	e.	Ports: The ports should be shown for HTTP + SSL
7.	Follow instructions on https://github.com/researchstudio-sat/webofneeds/blob/5dc0db3747c201a87d94621453b8b898a34e7fc4/documentation/installation-cryptographic-keys-and-certificates.md and make sure to copy the “tcnative-1.dll” into the Java jdk folder and the tomcat! Same with the “bcpkix-jdk15on-1.52.jar” and “bcprov-jdk15on-1.52.jar”
8.	Start server:
9.	Run the gulpfile outside eclipse, refresh the “won-owner-webapp” in eclipse (F5), click on the server –> “Publish”
