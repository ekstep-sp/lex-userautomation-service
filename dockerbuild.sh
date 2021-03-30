mvn clean install -DskipTests

docker build -t lexplatform.azurecr.io/user-automation:0.2.3-RELEASE .

docker push lexplatform.azurecr.io/user-automation:0.2.3-RELEASE
