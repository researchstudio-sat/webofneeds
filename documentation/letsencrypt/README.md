# LetsEncrypt

letsencrypt container (see [docker-compose live deployment](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-docker/deploy/live_satvm01/docker-compose.yml)) that helps renew the matchat.org (including match.org and node.matchat.org) certificate.
It is used by the nginx (external matchat representation of owner) and the wonnode.
This is only used around every 90 days to manually renew the certificate.

# Certificate Renewal

To renew the certificate note the following things:
- all involved containers (letsencrypt, nginx, wonnode) are configured to mount the right folders
- the certificate folder on the host is configured to be `$base_folder/letsencrypt/certs/live/matchat.org"`
- nginx must be running so that the acme challenge can be executed (for a new creation of the certificate you can start nginx with the nginx-http-only.conf which doesnt atom certificate file for startup)
- execute `docker exec livesatvm01_letsencrypt_1 bash //usr/local/bin/certificate-request-and-renew.sh` for certificate renewal on host satvm01
- this script can be changed for testing e.g. by adding parameters like `--dry-run or --test-cert` to the certbot
- this should renew the letsencrypt certificate in `$base_folder/letsencrypt/certs/live/matchat.org` on the host
- check if the .pem files and the java key store files (.jks and .pfx) in the same folder have also been updated. These are symlinks into `../../archive/<domainname>/`, you might want to follow those symlinks and check that they are pointing to new files
- delete all (trust store) files in directory `$base_folder/won-client-certs/` on all hosts (satvm01)
- redeploy all live containers (with jenkins job)
- check if everything works (HTTPS, websocket and JMS communication)


