## edit for your environment here:
$Env:deploy_host=10.0.75.1
$Env:base_folder="/tmp/won"
cd $Env:base_folder
## download the yml file:
Invoke-RestMethod -Uri https://raw.githubusercontent.com/researchstudio-sat/webofneeds/master/webofneeds/won-docker/deploy/local_image/docker-compose.yml > .\docker-compose.yml
## start the containers
docker-compose up -d 