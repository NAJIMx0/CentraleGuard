pipeline {
    agent any
    tools {
            maven 'Maven3'
        }
    stages {
        stage('Build') {
            steps {
                dir('telemetry-service') {
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
    }
}