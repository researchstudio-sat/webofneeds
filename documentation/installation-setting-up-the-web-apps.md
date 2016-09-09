# Setting up the web apps

The WON node webapp is the central application of the system that manages all the data. It is contained in the module webofneeds/won-node-webapp.

The WON owner webapp is the user-oriented web application that allows users to manage their needs. It is contatined in the module webofneeds/won-owner-webapp. The owner application can connect to any node (WON node webapps) and create and manipulate needs there. For the [**owner-app-setup** see here](./Setting-up-Frontend-Development-Environment)

##prerequesites for won-node and won-owner

	jdk 1.6 or later
	tomcat 7.0.55 or later
	maven 2.5 (not later!)
	postgresql 9
	git

set JAVA_HOME and append <MAVEN-HOME>/bin to the PATH

installation steps:

clone webofneeds:
	git clone https://github.com/researchstudio-sat/webofneeds.git

**Very important note for Windows installation:** The path-length in windows has to be less than 255 characters. We highly recommend you to install the WoN in the root directory of your hard drive partition, otherwise the path of node.js dependencies in the owner application will be longer than the allowed length and you will get errors that do not directly mention the reason.

Setup your [build environment](Building-with-maven).

## Quick test on local machine
For a quick test, build with `maven install -Pskip-tests` and deploy the webofneeds/won-node-webapp/target/won.war and webofneeds/won-owner-webapp/target/owner.war to your local tomacat instance. Set its [WON_CONFIG_DIR](https://github.com/researchstudio-sat/webofneeds/blob/devel-interim/webofneeds/conf/README.txt) variable to `<WON-CHECKOUT-DIR>/webofneeds/conf`, restart your tomcat and point your browser to http://localhost:8080/owner.

Note: Do not set the field "HTTP port" in the "Run/Debug Configurations" of IntelliJ.

