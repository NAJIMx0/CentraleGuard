pipeline {
    agent any

    tools {
        maven 'Maven3'
    }

  stage('Deploy with Docker Compose') {
      steps {
          sh 'docker compose up --build -d'
      }
  }
}