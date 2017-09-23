TOMCAT_HOME="/cygdrive/c/programs/apache-tomcat-7.0.54"

mvn clean package
mv "target/crossclipboard.war" "$TOMCAT_HOME/webapps"

pushd "$TOMCAT_HOME/bin"

TOMCAT_RUNNING=`jps | grep Bootstrap | wc -l`
if [ $TOMCAT_RUNNING == 1 ] 
then
  ./shutdown.bat
fi

sleep 1
rm -rf ../webapps/crossclipboard
sleep 1
./catalina.bat jpda start

popd
