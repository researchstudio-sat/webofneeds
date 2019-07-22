## WoN Docker Deployment

Docker images are provided in the [docker hub webofneeds repository](https://hub.docker.com/r/webofneeds/) to run
the webofneeds applications as docker containers. There are images for the following components of webofneeds available:

- wonnode: the won node application
- owner: the owner application
- postgres: postgres database for the wonnode and owner
- matcher_service: a general matcher service
- matcher_solr: a matcher that uses solr search index to match atoms
- bigdata: a RDF store that is used by the matcher-service to store rdf data
- solr: a solr server with used by the matcher-solr
- gencert: a tool that can generate certificates for wonnode and owner application
- bots: bots are used to test the communication between the servers in the application

For deployment of these components we use [docker-compose](https://docs.docker.com/compose/). It is possible to deploy
the components on multiple servers. For straight forward deployment however we have provided one docker-compose file
which can be used to deploy all components on one local server.

### How to deploy and run all webofneeds components on a single server

1. Install docker (https://www.docker.com) (make sure after installing Docker Desktop, that it has enough resources on your machine `Docker Desktop` -> `Settings` -> `Advanced`)
2. Install docker compose (https://docs.docker.com/compose/)
3. Download the [docker-compose.yml](deploy/local_image/docker-compose.yml) file that deploys all components at once
4. The script needs two environment parameters to be set. Export `deploy_host` to set the host you want to deploy the
   docker containers on. Export `base_folder` to set the folder where data (like certificates) are created and mounted
5. Make sure no other services on the server are running on the following ports that are used by the different
   containers: 8889, 2561, 2562, 5433, 7071, 8082, 8984, 10000, 61617
6. Execute the docker-compose.yml file on your "deploy_host" with `docker-compose up -d`

When the script executes it runs all the above listed docker images as containers and downloads them from the
webofneeds docker hub repository if not available locally. If the script finishes without error all components
should be started.

You can access the owner application to log in and create atoms using the following link:

- **owner:** [https://\${deploy_host}:8082/owner](https://${deploy_host}:8082/owner)

You can access the wonnode and check the generated RDF atoms using the following link:

- **wonnode:** [https://\${deploy_host}:8889/won](https://${deploy_host}:8889/won)

The certificates used by the application are created on the first execution of the script, and reused in later
executions. You can find this data in your "base_folder". If the containers are removed and recreated all data that
was created is lost because the data is not mounted to the host right now (you can change this by uncommenting the
"volumes:" parts in the script for the databases: postgres, solr, bigdata. However is might not work out of the box
on a windows systems with virtual box).

**NOTE**: If you install Docker on MacOSX/Windows you will have to use the IP-Adress of the docker machine (see
https://docs.docker.com/engine/installation/mac/), you can retrieve the IP-Adress with the command `$ docker-machine ls`.

**NOTE**: If you change the `$deploy_host` in the script you must either provide a different directory for the
certificates or delete the certificates beforehand, otherwise the creation of elements will not be possible due to
faulty certificates. Also delete the postgres docker container to wipe all preexisting data it is not valid for
different certificates anymore.
