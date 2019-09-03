## tomcat complains about missing keystore
```
SEVERE [main] org.apache.tomcat.util.net.jsse.JSSESocketFactory.getStore Failed to load keystore type JKS with path C:\Users\[username]/.keystore due to C:\Users\[username]\.keystore
 java.io.FileNotFoundException: C:\Users\[username]\.keystore
```
Reason: we use tomcat APR. This means that in conf/server.xml, the following line must be present:
```
<Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
```
and the apache tomcat native library needs to be installed. On Windows, this means you find the file `tcnative-1.dll` in tomcats `lib` folder,

for more information, see http://tomcat.apache.org/native-doc/

## maven build fails with NoClassDefFoundException: java/sql/SQLException
Turns out that you need to have a JDK installed. Download a recent JDK and tell eclipse to use that one by default: 
`Window >> Preferences >> Java >> Installed JREs`. For me, Jdk11 did not work, but Jdk8u202 did.

## maven dies saying 'github rate limit reached'
* for building on the *command line*, follow these [instructions](http://stackoverflow.com/questions/30995040/jspm-saying-github-rate-limit-reached-how-to-fix)
* for an *automated build*, like via jenkins, or inside an IDE, follow the [documentation of jspm-cli](https://github.com/jspm/jspm-cli/blob/master/docs/registries.md): set the environment variable `JSPM_GITHUB_AUTH_TOKEN` to `[github-username]:[github-personal-access-token]`.

**note:** we had the following error when using the above approach with TOKEN ([github-personal-access-token]) as credentials:
```
[ERROR] err  URIError: URI malformed
[ERROR]          at decodeURIComponent (native)
[ERROR]          at decodeCredentials
...
[ERROR]          at node.js:814:3
[ERROR]
[ERROR] err  Unable to load registry github
```
Using SSH KEY as credentials instead of TOKEN fixed that. On how to use ssh key for your github account see e.g. https://help.github.com/categories/ssh/ If this doesn't work, it is also possible to use Terminal and navigate to `won-owner-webapp/src/main/webapp` and enter `npm install`. After the rate error you wll be asked for credentials, enter your github name-password credentials, after that the project should built.


## Out of memory error

If you run your project and encounter a "Out of memory" error you should probably add this to your run configuration:

    -XX:MaxPermSize=128M

## icu4j: Invalid byte tag in constant pool

If you get a compile error like below <ou can just ignore it. There is a corrupt `.class` file in the maven dependencies and will go away with future updates and everything is OK untill you use it, but you won't. This will only affect Chinese speaking users since it is the `LocaleElements_zh__PINYIN.class` file for Pinyin.

Another possibility is to find the icu4j `.jar` file and delete the `LocaleElements_zh__PINYIN.class` file. This is a quick and dirty hack, but it works.

    Apr 16, 2013 2:07:59 PM org.apache.catalina.startup.ContextConfig processAnnotationsJar
    SEVERE: Unable to process Jar entry [com/ibm/icu/impl/data/LocaleElements_zh__PINYIN.class] from Jar [jar:file:/C:/DATA/atus/Code/webofneeds/webofneeds/won-node-webapp/target/won/WEB-INF/lib/icu4j-2.6.1.jar!/] for annotations
    org.apache.tomcat.util.bcel.classfile.ClassFormatException: Invalid byte tag in constant pool: 60
    at org.apache.tomcat.util.bcel.classfile.Constant.readConstant(Constant.java:133)
    at org.apache.tomcat.util.bcel.classfile.ConstantPool.<init>(ConstantPool.java:60)

More info about this error can be found at:
* http://stackoverflow.com/questions/6751920/tomcat-7-servlet-3-0-invalid-byte-tag-in-constant-pool
* http://maven.40175.n5.nabble.com/Problem-when-mvn-site-site-Generating-quot-Dependencies-quot-report-td113470.html
* http://jira.codehaus.org/browse/MPIR-142

## won.protocol.exception.RDFStorageException: Could not create File!

This means that Tomcat could not access the temp directory where it stores the `.ttl` files. This is either the `TMP` global variable or the `%tomcat_dir%/tmp` directory. The problem is that the first one is usually
located in `C:/WINDOWS/TEMP` and (if you installed Tomcat to `C:/Program Files/`) the second one is in `C:/Program files/apache-tomcat/temp/`. Unless you are an admin you will not have access to any of these.

You can either grant your user access to one of these directories or change the directory in `won-node/src/main/resources.localhost/` by changing the `rdf.file.path`.

Check the error message for the actual directory in question. Expect something like

    Caused by: java.io.FileNotFoundException: C:\Program Files\apache-tomcat-7.0.35\temp\1.ttl (Access denied)

## Port Bind Problem: org.apache.coyote.AbstractProtocol.init Failed to initialize end point associated with ProtocolHandler ["http-apr-8443"]

This problem or other similar errors that can be referred as "port bind problems" are causd by setting the port that is used by WoN-node or WoN-webapp in the Tomcat Server Settings of IntelliJ. Leave the field "HTTP port" in the "Run/Debug Configurations" free (the default is 8080 and does not cause any problem).

## maven dies in won-owner-webapp during 'clean' task
stating that 'node' was not found.
solution: run `mvn install` before `mvn clean`

## java.security.NoSuchProviderException: no such provider: BC

This happens when Tomcat can't find or access the the bc `.jar` files during startup. Below is a collection of actions that may fix the problem and places to copy the `.jar` files into. You may want to try them both separately and combined to find a setup that works for you.

You should be able to find both `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` in your maven directory (default location is: `C:/Users/[user name]/.m2/repository/org/bouncycastle/`). If not, build the whole project with `mvn install -Dmaven.test.skip=true` and check again. 

* in Eclipse, edit the server launch configuration properties (accessible via "Run As...") and add both `.jar` files as External JARs to User Entries (suggested in: [Build WON in Eclipse](https://github.com/researchstudio-sat/webofneeds/blob/master/documentation/build-with-eclipse.md), Step 9)
* copy both `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` to `%tomcat_dir%/lib/` (suggested in: [Crytpographic Keys and Certificates](https://github.com/researchstudio-sat/webofneeds/blob/5dc0db3747c201a87d94621453b8b898a34e7fc4/documentation/installation-cryptographic-keys-and-certificates.md), Step 11)
* copy both `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar` to `C:/Program Files/Java/[jre dir]/lib/ext/` and [install the bouncy castle security provider](http://www.bouncycastle.org/wiki/display/JA1/Provider+Installation) (suggested in: [issue#1393](https://github.com/researchstudio-sat/webofneeds/issues/1393))
* In the Tomcat server's `server.xml`, find the xml element `<Host appBase="webapps" ...` and add the xml attribute `startStopThreads="2"`
    * **NOTE:** This only has an effect if two or more webapps, e.g. a node and an owner, are started on the server.
    
 ## Exception in Owner-Webapp log: PKIX path building failed 
 One possible cause of this is that the [certificate renewal](/documentation/letsencrypt#certificate-renewal) updated the pem files but did not update the jks and pfx files. The consequence is that the node webapp uses the new key (as nginx loads the pem file) and the activemq server uses the old key (as it loads the jks file). 

*To check if this is the problem:* list the keys in the jks/pfx file (using `keytool -list -v -keystore t-keystore.pfx`) and compare them to the key information available for, eg. `https://{your-won-node}/won/resource` in the browser or some other http client. If the keys are the same, this is *not* the problem.
 
*To fix this:* overwrite the jks and pfx files by exporting the key from the pem file. This can be done by executing the `openssl`and `keytool` commands in the letsencrypt's container script [certificate-request-and-renew.sh](/webofneeds/won-docker/image/letsencrypt/certificate-request-and-renew.sh)
