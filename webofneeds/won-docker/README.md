## WoN Docker Deployment

The [steps](#steps) below should guide you quickly through everything necessary to run the webofneeds components mentioned [below](#won-components) locally with docker.

### WoN components

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

1. Download and install docker (https://www.docker.com) and docker-compose (https://docs.docker.com/compose/)
2. Download and run one of the following scripts:
   - Bash: [deploy_bash.sh](/webofneeds/won-docker/deploy/local_image/deploy_bash.sh)
   - PowerShell: [deploy_powershell.ps1](/webofneeds/won-docker/deploy/local_image/deploy_powershell.ps1)
2. After Docker downloading and starting the components you can access the owner and the node here:
   - owner: [https://10.0.75.1:8082/owner](https://10.0.75.1:8082/owner)
   - wonnode: [https://10.0.75.1:8889/won](https://10.0.75.1:8889/won)

### Troubleshooting 

Docker is too slow: Make sure after installing Docker Desktop, that it has enough resources on your machine `Docker Desktop` -> `Settings` -> `Advanced`

Problems starting docker:

- The scripts define the `deploy_host` which represents the docker local network address: Check if this matches your local docker network address
- The scripts define a default `base_folder`: Change it to another existing folder
- If the scripts say that the `base_folder` does not exist, you might create this folder, or change the path to an existing in the script
- Make sure the ports used by the containers aren't used by anything else. Currently these are 8889, 2561, 2562, 5433, 7071, 8082, 8984, 10000 and 61617. You can check the `ports`-properties[docker-compose.yml](../webofneeds/won-docker/deploy/local_image/docker-compose.yml) for the definitive version

For a more detailed overview about the WoN docker setup see [here](/webofneeds/won-docker/README.md).

### Additional Information

Docker images are provided in the [**docker-hub** webofneeds repository](https://hub.docker.com/r/webofneeds/) to run the webofneeds applications as docker containers. See in the [section "WoN-components"](#won-components) for a quick overview over available containers. When the script executes it runs all the docker images listed above as containers and **downloads** them from the webofneeds docker hub repository if not available locally. If the script finishes without error all components should be started.

It is possible to deploy the components on **multiple servers**. For straight forward deployment however we have provided [one docker-compose file](../webofneeds/won-docker/deploy/local_image/docker-compose.yml) which can be used to deploy all components on one local server, which is used by the deploy-script (see [section "Steps"](#steps)). The script loads the most recent `docker-compose.yml` from github. If you want to use a **modified `docker-compose.yml`**, follow step in the setup-script, sans the download.

The **certificates** used by the application are created on the first execution of the script, and reused in later executions. You can find this data in the `$base_folder` specified in the setup-script. If the containers are removed and **recreated** all data that was created is lost because the data is not mounted to the host right now (you can change this by uncommenting the
`"volumes:"` properties in the `docker-compose.yml` for the databases: `postgres`, `solr`, `bigdata`)

**NOTE**: If you install Docker on MacOSX/Windows you will have to use the IP-Adress of the docker machine (see https://docs.docker.com/engine/installation/mac/), you can retrieve the IP-Adress with the command `$ docker-machine ls`.

**NOTE**: If you change the `$deploy_host` in the setup-script you must either provide a different directory for the certificates or delete the certificates beforehand, otherwise the creation of elements will not be possible due to faulty certificates. Also delete the postgres docker container to wipe all preexisting data, as it won't be valid anymore anyway due to the certificate change. 
