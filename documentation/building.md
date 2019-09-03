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
10. Download/Install the latest [Tomcat 9 server](https://tomcat.apache.org/download-90.cgi)
11. Add Tomcat to Eclipse
    <!-- 11. Generate cryptographic keys. -->

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

## 10. Windows Build Tools

Run `mvn install`. When it reaches the owner-application, the maven-frontend plugin will install it's own version of `npm`. Using that run the following with admin permissions:

```
<PROJECT_ROOT>/webofneeds/won-owner-webapp/src/main/webapp/node/npm install -g windows-build-tools
```

If your Windows User does not have admin permissions, installing `windows-build-tools` will unfortunately install them only for that user. (Further info at https://github.com/researchstudio-sat/webofneeds/pull/1743) The Visual Studio build tools are installed correctly however, so you only need to fix your Python installation:

1. Go to https://www.python.org/downloads/ and download and install Python 2.7 (Python 3 will not work!)
2. Locate your Python installation, e.g., `c:\Python27\python.exe`
3. With the **same user that will execute the maven build**, run `npm config set python c:\\Python27\\python.exe` (replace with your actual location, of course, and note the double backslashes).

## 12. Add Tomcat to Eclipse

1. Create Server in Eclipse: File >> New >> Other >> Server
1. Choose Tomcat 9, then press "next" (not "finish")
1. Make sure you use a Java 8 JDK or JRE, not java 9, or tomcat will not start up and throw a JAXB-related exception.
1. Add node and owner and click finish
   1. If you do not have the options to add the owner and node application to the tomcat (also accessible via Server >> [your tomcat server] >> Add and Remove), something went wrong.
      1. Maybe you did not install eclipse for Java EE. Check Help >> About Eclipse. If it does not say 'Eclipse Java EE IDE for Web Developers.', the easiest is to download and install Eclipse for Java EE.
      2. Maybe the import of the webofneeds maven project somehow did not work properly. Delete all imported projects (without deleting the sources), then import again (File >> Import... >> Maven >> Existing Maven Projects )
1. Add Server view: Window >> Show View >> Server
1. Change server.xml: In Project Explorer >> Server >> "Your Server" >> open `server.xml` and add the following xml snippet.
