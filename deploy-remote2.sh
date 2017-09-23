#!/bin/bash

set -u
set -e

PROJECT_NAME=crossclipboard

svn up
mvn clean package -P no-jetty
service tomcat7 stop
#sh backup.sh
rm -rf /var/lib/tomcat7/webapps/${PROJECT_NAME}
cp target/${PROJECT_NAME}.war /var/lib/tomcat7/webapps
service tomcat7 start