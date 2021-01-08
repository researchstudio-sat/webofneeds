#!/bin/bash

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

if [ -z "$SAN" ]
then
  echo "SAN is empty, certificate will not have subject alternative names. To set, use eg SAN=DNS:localhost,DNS:example.com"
  SAN_OPT=""
else
  SAN_OPT="-config req.config"
  rm -rf req.config
  cat >> req.config << EOL
[ req ]
distinguished_name = dn
x509_extensions = ext
[ dn ]
common_name = ${CN}
[ ext ]
subjectAltName = ${SAN}
EOL
  echo "SAN is ${SAN}, adding as the certificate's alternative names, complete openssl option is ${SAN_OPT}"
  echo "contents of req.config: "
  cat req.config
fi

############################
# print some info messages
############################
echo "......................................................................."
echo "any generated certificates will be put into $out_folder"
echo "if run as docker, use -v YOUR_MOUNTED_FOLDER:$out_folder if you want to persist any generated certificates in YOUR_MOUNTED_FOLDER"
echo "if specified YOUR_MOUNTED_FOLDER already contains certificates, no new certificates are generated"
echo "......................................................................."
echo "openssl version: $(openssl version)"

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


	if [ ! -z "$OPENSSL_CONFIG_FILE" ]; then
	  echo "OPENSSL_CONFIG_FILE parameter is no longer supported!"
	fi

  openssl req -x509 -newkey rsa:2048 -keyout $key_pem_file -out $cert_pem_file -passout $PASS -days 365 -subj "/CN=${CN}" ${SAN_OPT}

	openssl x509 -in $cert_pem_file -noout -text

	echo "Self-signed certificate generated - please do not use it for production!"
fi


#########################################################################
# store generated key and certificate in pfx key store, if does not exist
#########################################################################

# $PASS can be file:<filetopassword> or pass:<password>
PASS_STR="$PASS"

if [ -f "$pfx_store_file" ]
then
	echo "$pfx_store_file already exists."
else
	echo "$pfx_store_file not found. Creating pfx store containing certificate from $key_pem_file and $cert_pem_file"

	# since the openssl tool throws an error if there is the same passin file specified as for passout, we have to read
	# the password from the file to avoid this error in case the password is passed in a file (PASS=file:<pathToFile>)
	if [[ $PASS == file:* ]]; then
		PASS_STR=`cat ${PASS:5}`
		PASS_STR="pass:$PASS_STR"
	fi

	openssl pkcs12 -export -out $pfx_store_file -passout $PASS_STR -inkey $key_pem_file -passin $PASS -in $cert_pem_file
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

	# since the keytool has problems with file:<password> format, we pass the password here directly (without pass: prefix however)
	$KEYTOOL -importkeystore -srckeystore $pfx_store_file -srcstoretype pkcs12 -destkeystore $jks_store_file 	-deststoretype JKS -srcstorepass ${PASS_STR:5} -deststorepass ${PASS_STR:5}
	echo "$jks_store_file created"
fi





