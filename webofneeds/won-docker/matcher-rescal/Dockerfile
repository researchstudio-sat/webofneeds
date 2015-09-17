# before docker build can be executed, the war file and the conf
# directory have to be copied into this folder (done by maven build)

#FROM python:2.7
# use java as a base image
FROM java:8


# python itself is already installed on the base image
#RUN apt-get update && apt-get install -y \
#    python-dev \
#    python-pip

# add webofneeds default config env variables
ENV WON_CONFIG_DIR=/usr/src/matcher-rescal/conf
ENV LOGBACK_CONFIG=logback_info.xml

# add the jar and the conf directory
ADD ./won-matcher-rescal.jar /usr/src/matcher-rescal/
ADD ./conf ${WON_CONFIG_DIR}

# add the python files
ADD ./python /usr/src/matcher-rescal/python
ENV matcher.rescal.pythonScriptDir=/usr/src/matcher-rescal/python

# add the necessary python modules
WORKDIR /usr/src/matcher-rescal/python
RUN pip install numpy
RUN pip install scipy
#RUN python python/setup.py install

# start siren matcher
WORKDIR /usr/src/matcher-rescal
CMD java -Dconfig.file=${WON_CONFIG_DIR}/matcher-rescal/application.conf -DWON_CONFIG_DIR=${WON_CONFIG_DIR}/matcher-rescal -Dlogback.configurationFile=${WON_CONFIG_DIR}/${LOGBACK_CONFIG} -jar won-matcher-rescal.jar