mvn clean install -DskipTests

docker build -t lexplatform.azurecr.io/user-automation:0.2.1-RC3 .

docker push lexplatform.azurecr.io/user-automation:0.2.1-RC3
