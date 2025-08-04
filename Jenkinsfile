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
    QA_URL = "http://localhost:${QA_PORT}"
    DOCKER_HUB_CREDENTIALS = credentials('docker-hub-creds')
    DOCKER_IMAGE = "thoufiqzeero/first"
    DOCKER_TAG = "${env.BUILD_NUMBER}"
  }
  stages {
    stage('Checkout Repos') {
      steps {
        script {
          // checkout main application repo (assuming this Jenkinsfile lives here)
          checkout scm
          // checkout integration tests repo into subfolder
          dir('integration-tests') {
            git url: 'https://github.com/PS‑Thoufiq/repo2.git', branch: 'main'
          }
        }
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
      when { expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' } }
      steps {
        echo "Deploying to QA environment"
        script {
          bat '''
            docker rm -f mongodb-qa || exit 0
            docker rm -f ${APP_NAME}-qa || exit 0
            docker run -d --name mongodb-qa --network spring-network -p 27018:27017 -e MONGO_INITDB_DATABASE=qa_db mongo:latest
            docker run -d --name ${APP_NAME}-qa --network spring-network -p ${QA_PORT}:8082 ^
              -e SPRING_PROFILES_ACTIVE=qa ^
              -e MONGODB_QA_URI=mongodb://mongodb-qa:27017/qa_db ^
              %DOCKER_IMAGE%:%DOCKER_TAG%
          '''
          sleep time: 60, unit: 'SECONDS'
          retry(3) {
            bat "curl -f http://localhost:${QA_PORT}/students/health | findstr \"\\\"status\\\":\\\"Live\\\"\" | findstr \"\\\"stage\\\":\\\"qa\\\"\""
          }
          bat "docker ps | findstr ${APP_NAME}-qa || exit 1"
          echo "QA running at $QA_URL/students/health"
        }
      }
    }
    stage('Integration Tests') {
      when { expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' } }
      steps {
        script {
          dir('integration-tests') {
            bat """
              mvnw.cmd verify -Dspring.profiles.active=qa -Dservice.url=${QA_URL}
            """
            bat """
              if exist target\\failsafe-reports (
                findstr /m /c:"FAILURE" target\\failsafe-reports\\*.txt && exit 1 || exit 0
              )
            """
          }
        }
      }
    }
    stage('Approval for Pre‑Prod') {
      when { expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' } }
      steps {
        input message: 'Integration tests passed. Approve deployment to Pre‑Prod?', ok: 'Deploy'
      }
    }
    stage('Deploy to Pre‑Prod') {
      steps {
        echo "Deploying to Pre‑Prod on port ${PREPROD_PORT}"
        script {
          bat '''
            docker rm -f mongodb-preprod || exit 0
            docker rm -f ${APP_NAME}-preprod || exit 0
            docker run -d --name mongodb-preprod --network spring-network -p 27019:27017 -e MONGO_INITDB_DATABASE=preprod_db mongo:latest
            docker run -d --name ${APP_NAME}-preprod --network spring-network -p ${PREPROD_PORT}:8083 ^
              -e SPRING_PROFILES_ACTIVE=preprod ^
              -e MONGODB_PREPROD_URI=mongodb://mongodb-preprod:27017/preprod_db ^
              %DOCKER_IMAGE%:%DOCKER_TAG%
          '''
          sleep time: 60, unit: 'SECONDS'
          retry(3) {
            bat "curl -f http://localhost:${PREPROD_PORT}/students/health | findstr \"\\\"status\\\":\\\"UP\\\"\" | findstr \"\\\"stage\\\":\\\"preprod\\\"\""
          }
          bat "docker ps | findstr ${APP_NAME}-preprod || exit 1"
          echo "Pre‑Prod running on http://localhost:${PREPROD_PORT}/students/health"
        }
      }
    }
  }
  post {
    success {
      echo 'Pipeline done! QA and Pre‑Prod are up.'
    }
    failure {
      echo 'Pipeline failed — inspect logs.'
      bat "docker logs ${APP_NAME}-qa || exit 0"
      bat "docker logs ${APP_NAME}-preprod || exit 0"
    }
    always {
      bat 'docker logout || exit 0'
    }
  }
}
