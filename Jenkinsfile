pipeline {
    agent any

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