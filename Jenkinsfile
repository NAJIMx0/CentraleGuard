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
        COMPOSE_PROJECT_NAME = 'centraleguard-pipeline'
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
            environment {
                // If your Jenkins agent is an Intel/AMD machine, use linux/amd64
                // If it is an ARM64 machine, change this to linux/arm64
                DOCKER_DEFAULT_PLATFORM = 'linux/amd64'
            }
            steps {
                sh '''
                    echo "Stopping and cleaning up previous containers..."
                    docker rm -f $(docker ps -aq) || true
                    docker rmi -f postgres:13 || true

                    echo "Pulling explicit image platforms..."
                    docker-compose pull kong-database || true

                    echo "Starting build and container spin-up..."
                    docker-compose up --build -d
                '''
            }
            post {
                failure {
                    sh 'docker-compose logs kong-database || true'
                }
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