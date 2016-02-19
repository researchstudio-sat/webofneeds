#!/bin/sh

#####################################################################
# set up certificate Common Name CN (for servers should be its host)
#####################################################################
if [ -z "$CN" ]; then
	CN="$DEFAULT_CN"
	echo "CN is empty, assigning default value $CN"
#else
#	echo "CN is $CN"
fi

############################################
# set up certificate's and stores' password
############################################
if [ -z "$PASS" ]; then
	PASS="$DEFAULT_PASS"
	echo "PASS is empty, assigning default value $PASS"
	echo "WARN: it is unsafe to use default value for password, instead specify password via -e PASS=your_password"
#else
#	echo "PASS is $PASS"
fi


############################
# print some info messages
############################
echo "......................................................................."
echo "any generated certificates will be put into $out_folder"
echo "if run as docker, use -v YOUR_MOUNTED_FOLDER:$out_folder if you want to persist any generated certificates in YOUR_MOUNTED_FOLDER"
echo "if specified YOUR_MOUNTED_FOLDER already contains certificates, no new certificates are generated"
echo "......................................................................."


#######################################################################
# generate pem private key and self-signed certificate, if do not exist
#######################################################################
if [ -f "$key_pem_file" ]
then
	echo "$key_pem_file already exists. No new self-signed certificate generated"
	if [ -f "$cert_pem_file" ]
  then
  	echo "$cert_pem_file exists."
  else
  	echo "Error: $cert_pem_file does not exist. Should contain public key that corresponds to private key in $key_pem_file"
  	exit 1
  fi
else
	echo "$key_pem_file not found. Generating self-signed certificate."


	if [ -z "$OPENSSL_CONFIG_FILE" ]; then
		openssl req -x509 -newkey rsa:2048 -keyout $key_pem_file -out $cert_pem_file -passout $PASS -days 365 -subj "/CN=${CN}"
	else
		openssl req -x509 -newkey rsa:2048 -keyout $key_pem_file -out $cert_pem_file -passout $PASS -days 365 -subj "/CN=${CN}" -config $OPENSSL_CONFIG_FILE
	fi

	openssl x509 -in $cert_pem_file -noout -text
	echo "Self-signed certificate generated - please do not use it for production!"
fi


#########################################################################
# store generated key and certificate in pfx key store, if does not exist
#########################################################################
if [ -f "$pfx_store_file" ]
then
	echo "$pfx_store_file already exists."
else
	echo "$pfx_store_file not found. Creating pfx store containing certificate from $key_pem_file and $cert_pem_file"
	openssl pkcs12 -export -out $pfx_store_file -passout $PASS -inkey $key_pem_file -passin $PASS -in $cert_pem_file
	echo "$pfx_store_file created"
fi


##########################################################################
# store generated key and certificate in java key store, if does not exist
##########################################################################
if [ -f "$jks_store_file" ]
then
	echo "$jks_store_file already exists."
else
	echo "$jks_store_file not found. Creating java key store containing certificates from $pfx_store_file"
	$KEYTOOL -importkeystore -srckeystore $pfx_store_file -srcstoretype pkcs12 -destkeystore $jks_store_file -deststoretype JKS -srcstorepass $PASS  -deststorepass $PASS
	echo "$jks_store_file created"
fi





