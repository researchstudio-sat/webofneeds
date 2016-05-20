## WoN Docker Deployment

Docker images are provided in the [docker hub webofneeds repository] (https://hub.docker.com/r/webofneeds/) to run
the webofneeds applications as docker containers. There are images for the following components of webofneeds available:

* wonnode: the won node application (~646 MB)
* owner: the owner application (~667 MB)
* postgres: postgres database for the wonnode and owner (~100 MB)
* matcher-service: a general matcher service (~297 MB)
* matcher-siren: a matcher that uses siren to match needs (~309 MB)
* bigdata: a RDF store that is used by the matcher-service to store rdf data (~365 MB)
* sirensolr: a solr server with siren plugin used by the matcher-siren (~219 MB)
* gencert: a tool that can generate certificates for wonnode and owner application (~496 MB)

These components can be deployed on different servers (for an example see [deploy_dev.sh](deploy_dev.sh)) or on the
same server (for an example see [deploy_master_run.sh](deploy_master_run.sh)). The configuration can be done by
passing parameters and mounting volumes into the docker containers.

### How to deploy and run all webofneeds components on a Linux (or MacOSX) server

1. Install and configure docker (https://www.docker.com)
2. Download the script [deploy_master_run.sh](deploy_master_run.sh) and configure it
3. Make sure no other services on the server are running on the following ports that are used by the different
containers: 
 * 443
 * 2561
 * 2562
 * 5433
 * 7071
 * 8082
 * 8984
 * 10000
 * 61617

4. Execute the script

The script has a config section at the beginning where you can "set deployment specific variables". Change the
variables there according to your environment. The most important variables you may need to change are your deployment
host server (`deploy_host`) and the docker options (`docker_options`) you use to run docker commands and the basefolder
(`base_folder`) which is used to store the certificates. By default the docker options are left empty.

When the script executes it runs all the above listed docker images as containers and downloads them from the
webofneeds docker hub repository if not available locally. If the script finishes without error all components
should be started. Some data containers (like, the databases, rdf-store and solr index) are kept and only
restarted if this script is started again. Also the certificate is created on the first execution of the script, and
reused in later executions. All other containers will be created new every time this script is started.

The owner and won node applications should be accessible through the browser under the following URLS (substitute the
 deploy_host variable accordingly):

**NOTE**: If you install Docker on MacOSX you will have to use the IP-Adress of the docker machine (see https://docs.docker.com/engine/installation/mac/), you can retrieve the IP-Adress with the command `$ docker-machine ls`.

* **owner:** https://${deploy_host}:8082/owner
* **wonnode:** https://${deploy_host}/won

**NOTE**: If you change the `${deploy_host}` in the script you must either provide a different directory for the certificates or delete the certificates beforehand, otherwise the creation of elements will not be possible due to faulty certificates. Also delete the postgres docker container to wipe all preexisting data it is not valid for different certificates anymore.

**NOTE**: for deployment on a Windows server basically the same steps as described for Linux apply expect that there is
no script available for Windows at the moment. But the docker commands in the scripts can be executed in the same way






