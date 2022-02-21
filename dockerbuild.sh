mvn clean install -DskipTests

docker build -t lexplatform.azurecr.io/user-automation:space-v3-sprint-1 .

docker push lexplatform.azurecr.io/user-automation:space-v3-sprint-1
