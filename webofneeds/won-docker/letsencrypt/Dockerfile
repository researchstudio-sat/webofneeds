FROM debian:jessie
ADD ./jessie-backports.list /etc/apt/sources.list.d/jessie-backports.list
RUN apt-get update && apt-get install -y certbot -t jessie-backports
RUN apt-get install -y cron

VOLUME /etc/letsencrypt
VOLUME /usr/share/nginx/html

ADD crontab-certificate-request-and-renew /etc/cron.d/crontab-certificate-request-and-renew
ADD certificate-request-and-renew.sh /usr/local/bin/certificate-request-and-renew.sh

RUN chmod 777 /etc/cron.d/crontab-certificate-request-and-renew
RUN chmod 777 /usr/local/bin/certificate-request-and-renew.sh

RUN /usr/bin/crontab /etc/cron.d/crontab-certificate-request-and-renew
RUN touch /var/log/cron.log

CMD cron && tail -f /var/log/cron.log



