# smarttracker
SmartTracker - Live-Tracking System

>**Start with [rentadrone-smarttracker-integration project](https://github.com/satspeedy/rentadrone-smarttracker-integration) first before continuing on this page.**

## Quickstart with minikube

- Download and start a minikube cluster

Steps in _project folder_:

- Setup infrastructure
```bash
docker compose -f deploy-compose/docker-compose.infra.yml up -d
```
- Upload/refresh app image to minikube
```bash
minikube image load com.hha/smarttracker:latest
```
- Create the app pod
```bash
kubectl apply -f deploy-k8s/app.yaml
```
- Add environment variables
```bash
kubectl set env deploy smarttracker-app AZURE_CLIENT_ID=<YOUR_AZURE_CLIENT_ID>
kubectl set env deploy smarttracker-app AZURE_CLIENT_SECRET=<YOUR_AZURE_CLIENT_SECRET>
kubectl set env deploy smarttracker-app AZURE_TENANT_ID=<YOUR_AZURE_TENANT_ID>
kubectl set env deploy smarttracker-app AZURE_VAULT_URL=<YOUR_AZURE_VAULT_URL>
```
- Determine the current port
```bash
minikube service smarttracker-app --url
```
- Send requests to app via host `localhost` and given port. E.g., get all trackings and format with `jq`
```bash
curl -X 'GET' 'http://localhost:<MINIKUBE PORT>/api/trackings?role=admin' -H 'accept: application/json' | jq
```
- Stop project
```bash
kubectl delete -f deploy-k8s/app.yaml
docker compose -f deploy-compose/docker-compose.infra.yml down --remove-orphans
```

## Quickstart with docker-compose
>**Note to docker-compose Usage:** Sometimes the app cannot connect to the mqtt broker. Especially identified when starting docker compose within the IDE. The app logs out something like `org.eclipse.paho.client.mqttv3.MqttException: Unable to connect to server`. A quick workaround is to restart the app with `docker restart smarttracker-app`.

Steps in _project folder_:

- Setup project
```bash
docker compose up -d
```
- Stop project
```bash
docker compose down --remove-orphans
```

## Quickstart with plain docker containers
Steps in _project folder_:

- Create the mqtt broker
```bash
docker run \
--name smarttracker-mqtt-broker \
--rm \
-p 9001:9001 \
-p 1883:1883 \
-e TZ=Europe/Berlin \
--mount type=bind,source=$(pwd)/mqtt-broker-configuration/mosquitto.conf,target=/mosquitto/config/mosquitto.conf \
-d eclipse-mosquitto:2.0.12
```
- Create the redis container
```bash
docker run \
--name smarttracker-store \
--rm \
-p 26379:6379 \
-e TZ=Europe/Berlin \
-d redis:alpine
```
- Build the app
```bash
mvn clean package
```
- Create the app container
```bash
docker run \
--name smarttracker-app \
--rm \
-p 8383:8383 \
-p 3083:3083 \
-p 52083:52083 \
-e SPRING_REDIS_HOST=host.docker.internal \
-e SPRING_REDIS_PORT=26379 \
-e MQTT_BROKER_HOST=host.docker.internal \
-e MQTT_BROKER_PORT=1883 \
-e TZ=Europe/Berlin \
-e DAPR_HTTP_PORT=3083 \
-e DAPR_GRPC_PORT=52083 \
-e NAMESPACE=default \
-d com.hha/smarttracker:latest
```
- Stop project
```bash
docker rm -f smarttracker-app smarttracker-store smarttracker-mqtt-broker
```
## API Documentation
- Swagger UI: http://localhost:8383/swagger-ui.html
- OpenAPI 3 data: http://localhost:8383/v3/api-docs
- Postman Collection with example requests: [smart-tracker.postman_collection.json](smart-tracker.postman_collection.json)

## Open Tasks
- [ ] UI to visualize a tracking
- [ ] Mosquitto Mqtt Broker mosquitto.conf file is only working on unix
- [ ] Pass environment variables DAPR_TRUST_ANCHORS, DAPR_CERT_CHAIN and DAPR_CERT_KEY to execute plain docker container with mTLS
- [ ] Pass environment variables AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID and AZURE_VAULT_URL to execute plain docker container with Azure key vault
