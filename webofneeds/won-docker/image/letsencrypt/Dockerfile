# NOTE: this image is not working correctly, the cron job for updating needs to be reworked

FROM openjdk:8

# install certbot
ADD ./jessie-backports.list /etc/apt/sources.list.d/jessie-backports.list
RUN apt-get update && apt-get install -y certbot -t jessie-backports

# install cron
RUN apt-get install -y cron

ENV key_store_password=changeit
ENV out_folder=/etc/letsencrypt
ENV key_pem_file="/etc/letsencrypt/live/www.matchat.org/privkey.pem"
ENV cert_pem_file="/etc/letsencrypt/live/www.matchat.org/fullchain.pem"
ENV pfx_store_file="/etc/letsencrypt/t-key-cert.pfx"
ENV jks_store_file="/etc/letsencrypt/t-keystore.jks"

VOLUME /etc/letsencrypt
VOLUME /usr/share/nginx/html

ADD crontab-certificate-request-and-renew /etc/cron.d/crontab-certificate-request-and-renew
ADD certificate-request-and-renew.sh /usr/local/bin/certificate-request-and-renew.sh

RUN chmod 744 /etc/cron.d/crontab-certificate-request-and-renew
RUN chmod 744 /usr/local/bin/certificate-request-and-renew.sh

RUN /usr/bin/crontab /etc/cron.d/crontab-certificate-request-and-renew
RUN touch /var/log/cron.log

CMD cron && tail -f /var/log/cron.log



