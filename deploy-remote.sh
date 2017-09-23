#!/bin/bash

set -u
set -e

PROJECT_NAME=crossclipboard
CURRENT_DIR=`pwd`

svn up
mvn clean package -P no-jetty
service tomcat7 stop
sh backup.sh
if [ ! -e /var/lib/tomcat7/webapps/${PROJECT_NAME} ]
then
    mkdir -p /var/lib/tomcat7/webapps/${PROJECT_NAME}
fi
pushd /var/lib/tomcat7/webapps/${PROJECT_NAME}
jar xvf ${CURRENT_DIR}/target/${PROJECT_NAME}.war
chown -R tomcat7.tomcat7 /var/lib/tomcat7/webapps/${PROJECT_NAME}
popd
service tomcat7 start