# LetsEncrypt

letsencrypt container (see [docker-compose live deployment](https://github.com/researchstudio-sat/webofneeds/blob/master/webofneeds/won-docker/deploy/live_satvm01/docker-compose.yml)) that helps renew the matchat.org (including match.org and node.matchat.org) certificate.
It is used by the nginx (external matchat representation of owner) and the wonnode.
Certificate renewal happens automatically via a cronjob.

# Certificate Renewal

To renew the certificate note the following things:

- all involved containers (letsencrypt, nginx, wonnode) are configured to mount the right folders
- the certificate folder on the host is configured to be `$base_folder/letsencrypt/certs/live/matchat.org"`
- nginx must be running so that the acme challenge can be executed (for a new creation of the certificate you can start nginx with the nginx-http-only.conf which doesn't need a certificate file for startup)
- execute `docker start livesatvm01_letsencrypt_1` for certificate renewal on host satvm01 (this should happen once a day via cronjob)
- to manually renew the certificates, execute `docker exec livesatvm01_letsencrypt1 bash //usr/local/bin/certificate-request-and-renew.sh`
- this script can be changed for testing e.g. by adding parameters like `--dry-run or --test-cert` to the certbot commands
- this should renew the letsencrypt certificate in `$base_folder/letsencrypt/certs/live/matchat.org` on the host
- check if the .pem files and the java key store files (.jks and .pfx) in the same folder have also been updated. These are symlinks into `../../archive/<domainname>/`, you might want to follow those symlinks and check that they are pointing to new files
- delete all (trust store) files in directory `$base_folder/won-client-certs/` on all hosts (satvm01)
- redeploy all live containers (with jenkins job)
- check if everything works (HTTPS, websocket and JMS communication)

# Troubleshooting

Updating the certificates sometimes fails. If things have stopped working for you after a certificate renewal, this [troubleshooting tip](/documentation/building.md#exception-in-owner-webapp-log-pkix-path-building-failed) may help.
