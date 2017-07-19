# PIWIK

piwik is deployed only on satvm01 together with the other live containers of won. 
the docker-compose script specifies the files mounted from the host ($base_folder/piwik/config). 
Here is the piwik config saved that is kept during all redeployments of the live environment. 

## How PIWIK was setup first time:

 * went manually through the piwik setup (creating user 'piwik' with password, etc)
 * add the following lines in piwik config.ini.php:

```
assume_secure_protocol = 1
proxy_client_headers[] = HTTP_X_FORWARDED_HOST
proxy_host_headers[] = HTTP_X_FORWARDED_HOST
```

(these headers are set in the nginx config)
