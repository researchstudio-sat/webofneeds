## Start WoN with Docker Tutorial

This tutorial will guide you quick through all the necessary steps to run the following webofneeds components locally with docker.

WoN components:

- wonnode: the won node application
- owner: the owner application
- postgres: postgres database for the wonnode and owner
- matcher_service: a general matcher service
- matcher_solr: a matcher that uses solr search index to match atoms
- bigdata: a RDF store that is used by the matcher-service to store rdf data
- solr: a solr server with used by the matcher-solr
- gencert: a tool that can generate certificates for wonnode and owner application
- bots: bots are used to test the communication between the servers in the application

### Steps

1. Download and install docker (https://www.docker.com)
2. Download and install docker compose (https://docs.docker.com/compose/)
3. Download and run one of the following scripts:
   - Bash: [deploy_bash.sh](/webofneeds/won-docker/deploy/local_image/deploy_bash.sh)
   - PowerShell: [deploy_powershell.ps1](/webofneeds/won-docker/deploy/local_image/deploy_powershell.ps1)
4. After Docker downloading and starting the components you can access the owner and the node here:
   - owner: [https://10.0.75.1:8082/owner](https://10.0.75.1:8082/owner)
   - wonnode: [https://10.0.75.1:8889/won](https://10.0.75.1:8889/won)

### Troubleshooting based on steps

**Step 1:** Docker is to slow: Make sure after installing Docker Desktop, that it has enough resources on your machine `Docker Desktop` -> `Settings` -> `Advanced`

**Step 3:** Problems starting docker:

- The scripts define the `deploy_host` which represents the docker local network address: Check if this matches your local docker network address
- The scripts define a default `base_folder`: Change it to another existing folder

For a more detailed overview about the WoN docker setup see [here](/webofneeds/won-docker/README.md)
