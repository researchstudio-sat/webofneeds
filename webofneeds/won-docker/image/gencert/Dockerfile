FROM openjdk:8-jre
RUN apt-get update && apt-get install -y \
    gnupg \
    vim \
    less \
    dos2unix

#keytool path, used by certificate generation script for creating jks
ENV KEYTOOL keytool

#specify your password when run via -e "PASS=pass:<your_password>" or in a file like "PASS=file:<your_file>"
ENV DEFAULT_PASS pass:changeit

#specify your certificate host when run via -e "CN=your_host"
ENV DEFAULT_CN localhost

#path to generated certificates/keys/stores
ENV out_folder /usr/local/certs/out
ENV key_pem_file="${out_folder}/t-key.pem"
ENV cert_pem_file="${out_folder}/t-cert.pem"
ENV pfx_store_file="${out_folder}/t-key-cert.pfx"
ENV jks_store_file="${out_folder}/t-keystore.jks"

#add certificate generation script and set it up to be able to run it
ADD ./generate-cert.sh /usr/local/certs/
RUN chmod +x /usr/local/certs/generate-cert.sh
RUN dos2unix /usr/local/certs/generate-cert.sh

#run certificate generation script
RUN mkdir -p ${out_folder}
CMD /usr/local/certs/generate-cert.sh
