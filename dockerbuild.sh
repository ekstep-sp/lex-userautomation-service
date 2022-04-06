mvn clean install -DskipTests

docker build -t lexplatform.azurecr.io/user-automation:space-v3-sprint-4.2 .

docker push lexplatform.azurecr.io/user-automation:space-v3-sprint-4.2
