mvn clean --quiet
mvn package -Pbukkit -Dmaven.test.skip=true --quiet
mvn package -Pbungee -Dmaven.test.skip=true --quiet