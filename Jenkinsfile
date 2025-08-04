
 pipeline {
    agent any
    tools {
  maven 'Maven-3.9.11'
        jdk 'JDK 21 '
    }
    environment {
        APP_NAME = "first"
        QA_PORT = "8082"
        PREPROD_PORT = "8083"
        LOG_DIR = "${WORKSPACE}/logs"
SERVER_IP = "localhost"
        QA_URL = "http://${SERVER_IP}:${QA_PORT}"
        DOCKER_IMAGE = "thoufiqzeero/first"
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    stages {
        stage('Verify Docker Access') {
            steps {
                script {
                    try {
                        sh 'docker ps'
                        echo "Docker access verified"
                    } catch (Exception e) {
                        error "Docker not accessible. Ensure Jenkins user has Docker permissions (sudo usermod -aG docker jenkins)"
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                git url: 'https://github.com/PS-Thoufiq/spring-sample-test-docker.git', branch: 'main'
                sh 'chmod +x mvnw'
            }
        }

        stage('Checkout Integration Tests') {
            steps {
                dir('integration-tests') {
                    git url: 'https://github.com/PS-Thoufiq/repo2.git', branch: 'main'
                    sh 'chmod +x mvnw'
                }
            }
        }

        stage('Create Log Directory') {
            steps {
                sh 'mkdir -p ${LOG_DIR}'
            }
        }

        stage('Build') {
            steps {
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh """
                            docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        """
                    }
                }
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-creds', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh """
                            echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin
                            docker push ${DOCKER_IMAGE}:${DOCKER_TAG}
                        """
                    }
                }
            }
        }

        stage('Setup Network') {
            steps {
                sh 'docker network create spring-network || true'
            }
        }

        stage('Deploy to QA') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                echo "Deploying to QA environment on port ${QA_PORT}"
                script {
                    sh """
                        docker rm -f mongodb-qa || true
                        docker rm -f ${APP_NAME}-qa || true
                    """
                    sh """
                        docker run -d --name mongodb-qa --network spring-network -p 27018:27017 -e MONGO_INITDB_DATABASE=qa_db mongo:latest
                    """
                    sh """
                        docker run -d --name ${APP_NAME}-qa --network spring-network -p ${QA_PORT}:8082 \
                        -e SPRING_PROFILES_ACTIVE=qa \
                        -e MONGODB_QA_URI=mongodb://mongodb-qa:27017/qa_db \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                    sleep(time: 60, unit: "SECONDS")
                    def maxRetries = 3
                    def retryDelay = 10
                    def success = false
                    for (int i = 0; i < maxRetries; i++) {
                        try {
                            sh """
                                curl -f http://${SERVER_IP}:${QA_PORT}/students/health | grep -q '\"status\":\"Live\"' && \
                                curl -f http://${SERVER_IP}:${QA_PORT}/students/health | grep -q '\"stage\":\"qa\"' || exit 1
                            """
                            success = true
                            break
                        } catch (Exception e) {
                            echo "Health check failed, retrying in ${retryDelay} seconds..."
                            sleep(time: retryDelay, unit: "SECONDS")
                        }
                    }
                    if (!success) {
                        error "QA health check failed after ${maxRetries} retries"
                    }

                    echo "QA is running on http://${SERVER_IP}:${QA_PORT}/students/health"
                    sh "docker ps | grep ${APP_NAME}-qa || exit 1"
                }
            }
        }

        stage('Integration Tests') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                dir('integration-tests') {
                    sh '''
                        ./mvnw verify -Dspring.profiles.active=qa -Dservice.url=${QA_URL}
                        if [ -d "target/failsafe-reports" ]; then
                            grep -l "FAILURE" target/failsafe-reports/*.txt && exit 1 || exit 0
                        fi
                    '''
                }
            }
        }

        stage('Approval for Pre-Prod') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                input message: 'Integration tests passed. Approve deployment to Pre-Prod?', ok: 'Deploy'
            }
        }

        stage('Deploy to Pre-Prod') {
            steps {
                echo "Deploying to Pre-Prod on port ${PREPROD_PORT}"
                script {
                    sh """
                        docker rm -f mongodb-preprod || true
                        docker rm -f ${APP_NAME}-preprod || true
                    """
                    sh """
                        docker run -d --name mongodb-preprod --network spring-network -p 27019:27017 -e MONGO_INITDB_DATABASE=preprod_db mongo:latest
                    """
                    sh """
                        docker run -d --name ${APP_NAME}-preprod --network spring-network -p ${PREPROD_PORT}:8083 \
                        -e SPRING_PROFILES_ACTIVE=preprod \
                        -e MONGODB_PREPROD_URI=mongodb://mongodb-preprod:27017/preprod_db \
                        ${DOCKER_IMAGE}:${DOCKER_TAG}
                    """
                    sleep(time: 60, unit: "SECONDS")
                    def maxRetries = 3
                    def retryDelay = 10
                    def success = false
                    for (int i = 0; i < maxRetries; i++) {
                        try {
                            sh """
                                curl -f http://${SERVER_IP}:${PREPROD_PORT}/students/health | grep -q '\"status\":\"Live\"' && \
                                curl -f http://${SERVER_IP}:${PREPROD_PORT}/students/health | grep -q '\"stage\":\"preprod\"' || exit 1
                            """
                            success = true
                            break
                        } catch (Exception e) {
                            echo "Health check failed, retrying in ${retryDelay} seconds..."
                            sleep(time: retryDelay, unit: "SECONDS")
                        }
                    }
                    if (!success) {
                        error "Pre-Prod health check failed after ${maxRetries} retries"
                    }

                    echo "Pre-Prod is running on http://${SERVER_IP}:${PREPROD_PORT}/students/health"
                    sh "docker ps | grep ${APP_NAME}-preprod || exit 1"
                }
            }
        }
    }
    post {
        success {
            echo 'Pipeline completed successfully! Both instances are running:'
            echo "QA: http://${SERVER_IP}:${QA_PORT}/students/health"
            echo "Pre-Prod: http://${SERVER_IP}:${PREPROD_PORT}/students/health"
        }
        failure {
            echo 'Pipeline failed. Check Docker container logs for details.'
            script {
                sh "docker logs ${APP_NAME}-qa || true"
                sh "docker logs ${APP_NAME}-preprod || true"
            }
        }
        always {
            script {
                sh 'docker logout || true'
                cleanWs()
            }
        }
    }
}
