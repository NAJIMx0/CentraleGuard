pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

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

        stage('Test') {
            steps {
                dir('api-gateway') {
                    sh 'mvn test'
                }
                dir('plc-command-service') {
                    sh 'mvn test'
                }
                dir('telemetry-service') {
                    sh 'mvn test'
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                sh '''
                    docker-compose down --remove-orphans -v || true
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

    post {
        failure {
            sh 'docker-compose down --remove-orphans || true'
        }
    }
}