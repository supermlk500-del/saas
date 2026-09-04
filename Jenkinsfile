pipeline {
    agent any

    environment {
        IMAGE_NAME = 'zhihuitong'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Backend test') {
            steps {
                dir('backend') {
                    sh 'mvn -B test'
                }
            }
        }

        stage('Frontend test') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm test -- --run'
                }
            }
        }

        stage('Build image') {
            steps {
                sh 'docker build --pull -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .'
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f || true'
        }
    }
}
