Spring Sample Test Using Docker

A simple Spring Boot application for managing student data with MongoDB, built with Maven and deployed using Docker and Jenkins.
Overview

Purpose: A RESTful API for CRUD operations on student records.
Tech Stack:
Java 21
Spring Boot 3.3.4
MongoDB
Maven
Docker
Jenkins


Environments:
QA: Runs on port 8082
Pre-Prod: Runs on port 8083
Local: Runs on port 8081



Prerequisites

Java 21
Maven
Docker
Jenkins (with Docker Pipeline and Git plugins)
Docker Hub account
MongoDB (local or Dockerized)

Project Structure

src/main/java: Application code (REST API for students).
src/main/resources:
application.properties: Default config (port 8081).
application-qa.properties: QA config (port 8082).
application-preprod.properties: Pre-Prod config (port 8083).


src/test/java: Integration tests (StudentCrudIntegrationTest).
Dockerfile: Builds the Docker image for the application.
Jenkinsfile: Defines the CI/CD pipeline.
docker-compose.yml: Runs the app and MongoDB locally.

Setup Instructions
1. Clone the Repository
git clone https://github.com/PS-Thoufiq/spring-sample-test-docker.git
cd spring-sample-test

2. Build the Application
mvn clean package -DskipTests


Creates target/first-0.0.1-SNAPSHOT.jar.

3. Run Locally with Docker

Create a Docker network:docker network create spring-network


Start MongoDB:docker run -d --name mongodb --network spring-network -p 27017:27017 -e MONGO_INITDB_DATABASE=studentdb mongo:latest


Build and run the application:docker build -t first:latest .
docker run -d --name first --network spring-network -p 8081:8081 -e MONGODB_URI=mongodb://mongodb:27017/studentdb first:latest


Verify: Open http://localhost:8081/students/health (should show "status":"UP").

4. Run with Docker Compose
docker-compose up -d


Starts default (port 8081), QA (port 8082), and Pre-Prod (port 8083) environments with MongoDB.
Verify:
Default: http://localhost:8081/students/health
QA: http://localhost:8082/students/health
Pre-Prod: http://localhost:8083/students/health



5. Jenkins Deployment

Setup Jenkins:

Install Docker on the Jenkins server.
Install plugins: Docker Pipeline, Git, Pipeline.
Add Docker Hub credentials in Jenkins:
ID: docker-hub-credentials-id
Username: Your Docker Hub username 
Password: Your Docker Hub password or token




Configure Pipeline:

Create a new pipeline job in Jenkins.
Set Pipeline > Definition to Pipeline script from SCM.
Repository URL: https://github.com/PS-Thoufiq/spring-sample-test.git
Branch: main
Script Path: Jenkinsfile


Run Pipeline:

Trigger the pipeline (manually or via GitHub webhook).
The pipeline:
Clones the repository.
Builds the JAR with Maven.
Creates and pushes a Docker image to ps-thoufiq/first:<build-number>.
Deploys QA (port 8082) and Pre-Prod (port 8083) with MongoDB containers.
Runs integration tests against QA.
Requires manual approval before Pre-Prod deployment.




Verify Deployment:

QA: http://<jenkins-server-ip>:8082/students/health
Pre-Prod: http://<jenkins-server-ip>:8083/students/health
Check containers: docker ps



6. Pipeline Details

Checkout: Clones the repository.
Build: Runs mvn clean package -DskipTests.
Build Docker Image: Creates ps-thoufiq/first:<build-number>.
Push to Docker Hub: Pushes the image to Docker Hub.
Setup Network: Creates spring-network for Docker communication.
Deploy to QA:
Starts MongoDB (mongodb-qa) and app (first-qa) containers.
Checks health at http://localhost:8082/students/health.


Integration Tests: Runs CRUD tests against QA.
Approval: Waits for manual approval.
Deploy to Pre-Prod:
Starts MongoDb (mongodb-preprod) and app (first-preprod) containers.
Checks health at http://localhost:8083/students/health.



7. Troubleshooting

Port Conflict: Ensure ports 8081, 8082, 8083, 27017, 27018, 27019 are free.
MongoDB Issues: Check logs with docker logs mongodb-qa or docker logs mongodb-preprod.
Pipeline Failure: Check Jenkins console output or container logs (docker logs first-qa, docker logs first-preprod).
Docker Hub: Verify credentials in Jenkins (docker-hub-credentials-id).

8. Notes

QA and Pre-Prod containers run until stopped, replaced by a new pipeline run, or the Jenkins server restarts.
To keep containers running after server restarts, add --restart=always to docker run commands in the Jenkinsfile.
For production, consider a dedicated server or Kubernetes.

