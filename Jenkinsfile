pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/NAJIMx0/CentraleGuard.git'
            }
        }
        stage('Build telemetry-service') {
            steps {
                dir('telemetry-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        stage('Build plc-command-service') {
            steps {
                dir('plc-command-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        stage('Build Docker Images') {
            steps {
                sh 'docker build -t telemetry-service ./telemetry-service'
                sh 'docker build -t plc-command-service ./plc-command-service'
                sh 'docker build -t api-gateway ./api-gateway'
                sh 'docker build -t discovery-service ./discovery-service'
                sh 'docker build -t config-server ./config-server'
                sh 'docker build -t ai-service ./ai-service'
            }
        }
    }
}