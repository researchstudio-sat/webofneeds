echo renew letsencrypt certificate
certbot certonly --webroot -w /usr/share/nginx/html --email $certificate_email --text --non-interactive --agree-tos \
-d matchat.org -d www.matchat.org -d node.matchat.org -d uki.matchat.org -d node.uki.matchat.org

# create pfx key store
echo create pfx key store: $pfx_store_file
openssl pkcs12 -export -out $pfx_store_file -passout pass:$key_store_password -inkey $key_pem_file -in $cert_pem_file

# create java key store
echo create java key store: $jks_store_file
keytool -importkeystore -srckeystore $pfx_store_file -srcstoretype pkcs12 -destkeystore $jks_store_file \
-deststoretype JKS -srcstorepass $key_store_password -deststorepass $key_store_password -noprompt