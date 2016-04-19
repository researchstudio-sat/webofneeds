## WoN Docker Deployment

Docker images are provided in the [docker hub webofneeds repository] (https://hub.docker.com/r/webofneeds/) to run
the webofneeds applications as docker containers. There are images for the following components of webofneeds available:

* wonnode: the won node application
* owner: the owner application
* postgres: postgres database for the wonnode and owner
* matcher-service: a general matcher service
* matcher-siren: a matcher that uses siren to match needs
* bigdata: a RDF store that is used by the matcher-service to store rdf data
* sirensolr: a solr server with siren plugin used by the matcher-siren
* gencert: a tool that can generate certificates for wonnode and owner application

These components can be deployed on different servers (for an example see [deploy_dev.sh](deploy_dev.sh)) or on the
same server (for an example see [deploy_master_run.sh](deploy_master_run.sh)). The configuration can be done by
passing parameters and mounting volumes into the docker containers.


### How to deploy and run all webofneeds components on a Linux server

1. Install and configure docker
2. Download the script [deploy_master_run.sh](deploy_master_run.sh) and configure it
3. Make sure no other services on the server are running on the following ports that are used by the different
containers: 443, 8082, 5433, 61617, 10000, 2561, 7071, 8984, 2562
3. Execute the script

The script has an config section at the beginning where you can "set deployment specific variables". Change the
variables there according to your environment. The most important variables you may need to change are your deployment
host server (deploy_host) and the docker options (docker_options) you use to run docker commands. By default the
docker options are left empty.

When the script executes it runs all the above listed docker images as containers and downloads them from the
webofneeds docker hub repository if not available locally. If the script finishes without error all components
should be started.

The owner and won node applications should be accessible through the browser under the following URLS (substitute the
 deploy_host variable accordingly):

* **owner:** https://${deploy_host}:8082/owner
* **wonnode:** https://${deploy_host}/won


**NOTE**: for deployment on a Windows server basically the same steps as described for Linux apply expect that there is
no script available for Windows at the moment. But the docker commands in the scripts can be executed in the same way






