# Cryptographic Keys and Certificates
**NOTE:** The following guide assumes you want to run all services on the same machine. If you deploy these on different machines, use the respective IPs and Paths as suited/desired.

1. We use the Apache Portable Runtime (APR) in the security modules of the Web of Needs. Normally, APR is included in the Tomcat by default. But it could happen that if you install the Tomcat by its Windows installer, APR will not be installed with Tomcat. To be sure that you have APR, check your `<TOMCAT_FOLDER>/bin`. A DLL file called `tcnative-1.dll` MUST be there. If you do not have this DLL file you need to add the Tomcat Native Downloads to your `<TOMCAT_FOLDER>/bin`. Follow the instructions here: http://tomcat.apache.org/download-native.cgi (simply download the zip file and then add the DLL to your `<TOMCAT_FOLDER>/bin`).

**NOTE:** After all the configurations below are done, if you have problems starting the tomcat (cannot be started, or started with the default keys, or complains about key format), most probably the APR library is not set up. Check Tomcat documenentation for OS specific setup, e.g. http://tomcat.apache.org/tomcat-8.0-doc/apr.html. For Mac OS users this is helpful: http://mrhaki.blogspot.co.at/2011/01/add-apr-based-native-library-for-tomcat.html (Mac Ports have to be installed)

2. Copy the webofneeds conf-folder: `cp -r conf conf.local` if you haven't done that already.

3. Change all instances of `localhost` in the configurations to your your ip (or computer name or domain) if you are not going to run node owner or matcher locally.

4. In `conf.local`, edit `matcher-service.properties`, `node.properties` and `owner.properties` and change all instances of keystore/truststore locations to point to a folder where you will store generated keys and certificates (in ), e.g. I used `C:/WoN/Keystore/`). Do not change the file name, just the path.

5. Change the accompanying passwords to something at least 6-letter long (e.g. `"changeit"`)

6. In `conf.local/owner.properties` set `http.port` and `node.default.http.port` to `8443`

7. Copy the SSL connector statement given below to <TOMCAT_FOLDER>/conf/server.xml as a child of the `<Service name="Catalina">`-node and change the password and key-folders there to values used in previous steps as well (e.g. `"changeit"`, `C:/WoN/Keystore/t-cert.pem`, `C:/WoN/Keystore/t-key.pem`). If the instructions on https://github.com/researchstudio-sat/webofneeds/blob/master/documentation/build-with-eclipse.md were followed before, modify the statement already added at step 6 if needed.

    ```xml
    <Connector
    clientAuth="wanted" port="8443" minSpareThreads="5" maxSpareThreads="75"
    enableLookups="true" disableUploadTimeout="true"
    acceptCount="100" maxThreads="200"
    scheme="https" secure="true" SSLEnabled="true"
    SSLCertificateFile="C:/WoN/Keystore/t-cert.pem"
    SSLCertificateKeyFile="C:/WoN/Keystore/t-key.pem"
    SSLPassword="changeit"
    SSLVerifyClient="optionalNoCA"
    SSLVerifyDepth="2"
    sslProtocol="TLS"/>
    ```
8. In the console navigate to the folder for the keystore created in previous steps (e.g. `C:/WoN/Keystore/`), adapt and run the following lines:

    ```sh
    openssl req -x509 -newkey rsa:2048 -keyout t-key.pem -out t-cert.pem  -passout pass:changeit -days 365 -subj "/CN=myhost.mydomain.com"

    openssl pkcs12 -export -out sometmpfile_deletme -passout pass:changeit -inkey t-key.pem -passin pass:changeit -in t-cert.pem

    <YOUR_JDK_DIR>/bin/keytool.exe -importkeystore -srckeystore sometmpfile_deletme -srcstoretype pkcs12 -destkeystore t-keystore.jks -deststoretype JKS -srcstorepass changeit  -deststorepass changeit

    rm sometmpfile_deletme
    ```
**NOTE:** the openssl commands can be executed in windows using cygwin or the git bash
**NOTE:** If you're getting the error message `Subject does not start with '/'.`, change last parameter to `-subj "//CN=myhost.mydomain.com"`


9. The other key stores, and the trust stores are created and filled in automatically when the application is run (in the locations defined in step 4 with the passwords defined in step 5).

10. Depending on your java-setup it might not be able to generate keys of a relevant length. In that case, you need to download and install the [Java Cryptography Extension](http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html). There's a readme in the zip detailing its setup. At the time of this writing, this consists of copying the two jars into `$JAVA_HOME/(jre/)lib/security`. If you don't do this or the jars are in the wrong folder, you'll get an exception like `java.security.InvalidKeyException: Illegal key size` when trying to run the app.

11. After building the project, copy the bouncycastle libraries (as of current state, `bcpkix-jdk15on-1.52.jar` and `bcprov-jdk15on-1.52.jar`) from the generated in your project `target/required-libs/` folder into the the tomcat's `lib/` (if you miss this step, you'll see BC exceptions when running the owner/node)

**NOTE:** During the steps layed out above, I've also updated to Tomcat 8 and I haven't verified that the app also runs on Tomcat 7.

**NOTE:** If you're re-deploying the project and want to use defferent to previously used digital certificates (e.g., generate new ones according to step 8, or use your server certificates certified by a CA), in addition to replacing corresponding server certificate files and the broker's key store, you have to also delete (or empty) all the trust store files used by the applications. Also, if you have deleted or replaced the owner keystore, and your deployment uses other then in-memory database, you have to empty this database (the stored data related to owner registration at node has to be deleted).

**NOTE:** If you're running docker containers:

Deploy sripts for building and running web of needs as docker containsers (see `webofneeds/webofneeds/won-docker/deploy*.sh`) include building and running server/broker certificate generation container `gencert`. It generates self-signed certificates for the server with the specified name (or IP) protected with the specified password if there are no certificates already present in the mounted volume. The generated key and certificate are in pem format (for Tomcat server) and java keystore format (for broker). If you already have server certificate for your server, and they are in the required format, you don't need to run the `gencert`. If you do want to generate and use self-signed certificate for your web of needs deployment, run it with changed parameters (server name and password), so that they correspond to your server name and to your desired password. Make sure that the same volume containing the certificate is mounted to the wonnode or owner container it is intended for. E.g. the location with the certificate of `server.example.at` should be mounted when owner application is run as docker container if the owner is being deployed at `server.example.com`. When using other than default file pathes and passwords for certificates, make sure that the same values are used in application properies files and in `server.xml`.

**NOTE:** Inspecting keystores using `keytool`:
owner/node keystores are saved in bouncycastle's UBER format. Therefore, the bouncycastle libraries need to be present in the `<YOUR_JRE_DIR>/lib/ext` folder. Follow these steps:

1. build the project. the bouncycastle libs are copied to `webofneeds/webofneeds/target/required-libs/`
2. copy the bouncycastle libs to your JRE's `bin/ext` folder
3. use keytool to inspect the keystore, naming the `providerclass` as shown below :

`keytool -list -v -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider -storetype UBER -storepass <YOUR_KEYSTORE_PASSWORD> -keystore t-keystore.jks`
