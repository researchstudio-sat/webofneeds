# Ubuntu Installation Notes

## Installing Java

In Ubuntu 18.04 there seems to be a bug with the certificate configuration. This manifests in an error:

> java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty

A fix mentioned in [this Stackoverflow answer](https://stackoverflow.com/a/50103533) rewrites the Java cacerts store.

I also installed Oracle Java 8 instead of OpenJdk 11, but I am not positive that this is necessary.

## Building libtcnative for APR

APR is needed to properly load the ssl engine in tomcat.

The build instructions are [here](http://tomcat.apache.org/native-doc/)

### Building TCNative

1. Download tcnative from the [download page](http://tomcat.apache.org/download-native.cgi)
    
    I used version `1.2.16`, version `1.2.7` which is shipped with tomcat `8.0.36` has a bug when building with ssl support

2. Install dependencies

    ```
    apt install libapr1-dev libssl-dev
    ```

3. Configure and build

    In the `native/` folder of your downloaded source package:

    ```
    mkdir target
    ./configure --prefix=`pwd`/target
    make && make install
    ```

4. Copy the libraries to tomcat

    In `target/lib/` copy all `tcnative*.so*` files into tomcats `bin/` directory
