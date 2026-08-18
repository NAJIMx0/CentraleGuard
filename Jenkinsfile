pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/NAJIMx0/CentraleGuard.git'
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                sh '''
                    docker rm -f $(docker ps -aq) || true
                    docker-compose up --build -d
                '''
            }
        }

        stage('Wait for SonarQube') {
            steps {
                sh '''
                    until curl -s http://sonarqube:9000/api/system/status | grep -q "\\"status\\":\\"UP\\""; do
                        echo "Waiting for SonarQube to be ready..."
                        sleep 5
                    done
                '''
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('api-gateway') {
                    sh 'mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=centraleguard-gateway -Dsonar.host.url=http://sonarqube:9000 -Dsonar.token=$SONAR_TOKEN'
                }
            }
        }

    }
}