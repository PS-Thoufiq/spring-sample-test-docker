pipeline {
    agent any
    tools {
        maven 'Maven'
        jdk 'JDK' // Configured for Java 21
    }
    environment {
        APP_NAME = "first"
        QA_PORT = "8082"
        PREPROD_PORT = "8083"
        LOG_DIR = "${WORKSPACE}/logs"
        QA_URL = "http://localhost:${QA_PORT}"
        DOCKER_HUB_CREDENTIALS = credentials('docker-hub-creds')
        DOCKER_IMAGE = "thoufiqzeero/first" // Replace with your Docker Hub username
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/PS-Thoufiq/spring-sample-test-docker.git', branch: 'main'
            }
        }
        stage('Create Log Directory') {
            steps {
                bat 'mkdir "%LOG_DIR%" || exit 0'
            }
        }
        stage('Build') {
            steps {
                bat 'mvnw.cmd clean package -DskipTests'
            }
        }
        stage('Build Docker Image') {
            steps {
                bat "docker build -t %DOCKER_IMAGE%:%DOCKER_TAG% ."
            }
        }
        stage('Push to Docker Hub') {
            steps {
                bat 'echo %DOCKER_HUB_CREDENTIALS_PSW% | docker login -u %DOCKER_HUB_CREDENTIALS_USR% --password-stdin'
                bat "docker push %DOCKER_IMAGE%:%DOCKER_TAG%"
            }
        }
        stage('Setup Network') {
            steps {
                bat 'docker network create spring-network || exit 0'
            }
        }
        stage('Deploy to QA') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                echo "Deploying to QA environment on port ${QA_PORT}"
                script {
                    // Stop and remove existing containers
                    bat """
                        docker rm -f mongodb-qa || exit 0
                        docker rm -f ${APP_NAME}-qa || exit 0
                    """
                    // Start MongoDB for QA
                    bat """
                        docker run -d --name mongodb-qa --network spring-network -p 27018:27017 -e MONGO_INITDB_DATABASE=qa_db mongo:latest
                    """
                    // Start QA application
                    bat """
                        docker run -d --name ${APP_NAME}-qa --network spring-network -p ${QA_PORT}:8082 ^
                        -e SPRING_PROFILES_ACTIVE=qa ^
                        -e MONGODB_QA_URI=mongodb://mongodb-qa:27017/qa_db ^
                        %DOCKER_IMAGE%:%DOCKER_TAG%
                    """
                    // Wait for application to start
                    sleep(time: 60, unit: "SECONDS")
                    // Verify health check
                    script {
                        def maxRetries = 3
                        def retryDelay = 10
                        def success = false
                        for (int i = 0; i < maxRetries; i++) {
                            try {
                                bat """
                                    curl -f http://localhost:${QA_PORT}/students/health | findstr \"\\\"status\\\":\\\"Live\\\"\" | findstr \"\\\"stage\\\":\\\"qa\\\"\" || exit 1
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
                    }
                    // Log QA status
                    echo "QA is running on http://localhost:${QA_PORT}/students/health"
                    // Verify process
                    bat """
                        docker ps | findstr ${APP_NAME}-qa || exit 1
                    """
                }
            }
        }
        stage('Integration Tests') {
            when {
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            steps {
                script {
                    // Run integration tests
                    bat """
                        mvnw.cmd verify -Dspring.profiles.active=qa -Dservice.url=${QA_URL}
                    """
                    // Verify integration tests
                    bat """
                        if exist target\\failsafe-reports (
                            findstr /m /c:"FAILURE" target\\failsafe-reports\\*.txt && exit 1 || exit 0
                        )
                    """
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
                    // Stop and remove existing containers
                    bat """
                        docker rm -f mongodb-preprod || exit 0
                        docker rm -f ${APP_NAME}-preprod || exit 0
                    """
                    // Start MongoDB for Pre-Prod
                    bat """
                        docker run -d --name mongodb-preprod --network spring-network -p 27019:27017 -e MONGO_INITDB_DATABASE=preprod_db mongo:latest
                    """
                    // Start Pre-Prod application
                    bat """
                        docker run -d --name ${APP_NAME}-preprod --network spring-network -p ${PREPROD_PORT}:8083 ^
                        -e SPRING_PROFILES_ACTIVE=preprod ^
                        -e MONGODB_PREPROD_URI=mongodb://mongodb-preprod:27017/preprod_db ^
                        %DOCKER_IMAGE%:%DOCKER_TAG%
                    """
                    // Wait for application to start
                    sleep(time: 60, unit: "SECONDS")
                    // Verify health check
                    script {
                        def maxRetries = 3
                        def retryDelay = 10
                        def success = false
                        for (int i = 0; i < maxRetries; i++) {
                            try {
                                bat """
                                    curl -f http://localhost:${PREPROD_PORT}/students/health | findstr \"\\\"status\\\":\\\"UP\\\"\" | findstr \"\\\"stage\\\":\\\"preprod\\\"\" || exit 1
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
                    }
                    // Log Pre-Prod status
                    echo "Pre-Prod is running on http://localhost:${PREPROD_PORT}/students/health"
                    // Verify process
                    bat """
                        docker ps | findstr ${APP_NAME}-preprod || exit 1
                    """
                }
            }
        }
    }
    post {
        success {
            echo 'Pipeline completed successfully! Both instances are running:'
            echo "QA: http://localhost:${QA_PORT}/students/health"
            echo "Pre-Prod: http://localhost:${PREPROD_PORT}/students/health"
        }
        failure {
            echo 'Pipeline failed. Check Docker container logs for details.'
            bat "docker logs ${APP_NAME}-qa || exit 0"
            bat "docker logs ${APP_NAME}-preprod || exit 0"
        }
        always {
            bat 'docker logout || exit 0'
        }
    }
}