:: I don't like batch, I think it's a stupid language. If you want to build this in Windows, I suggest installing git bash and running build.sh instead.
mvn clean --quiet
mvn package -Dmaven.test.skip=true --quiet
mvn package -pl bungee -am -Dmaven.test.skip=true --quiet