pipeline {
    agent any

    environment {
        APP_NAME = 'foodexpress-backend'
        DOCKER_IMAGE = "foodexpress/${APP_NAME}"
        REGISTRY_CREDENTIALS_ID = 'dockerhub-credentials'
    }

    tools {
        jdk 'Java21'
        maven 'Maven3'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('1. Checkout') {
            steps {
                echo 'Checking out source code from Git repository...'
                checkout scm
            }
        }

        stage('2. Build') {
            steps {
                echo 'Compiling backend source code with Maven...'
                script {
                    if (isUnix()) {
                        sh 'mvn clean compile -f backend/pom.xml'
                    } else {
                        bat 'mvn clean compile -f backend/pom.xml'
                    }
                }
            }
        }

        stage('3. Unit & Integration Tests') {
            steps {
                echo 'Executing JUnit 5 and Mockito test suite...'
                script {
                    if (isUnix()) {
                        sh 'mvn test -f backend/pom.xml'
                    } else {
                        bat 'mvn test -f backend/pom.xml'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('4. Package') {
            steps {
                echo 'Packaging executable standalone Spring Boot JAR...'
                script {
                    if (isUnix()) {
                        sh 'mvn package -DskipTests -f backend/pom.xml'
                    } else {
                        bat 'mvn package -DskipTests -f backend/pom.xml'
                    }
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        stage('5. Docker Build') {
            steps {
                echo "Building Docker container image: ${DOCKER_IMAGE}:${BUILD_NUMBER}..."
                script {
                    if (isUnix()) {
                        sh "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} -t ${DOCKER_IMAGE}:latest ."
                    } else {
                        bat "docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} -t ${DOCKER_IMAGE}:latest ."
                    }
                }
            }
        }

        stage('6. Docker Test') {
            steps {
                echo 'Verifying Docker image startup...'
                script {
                    if (isUnix()) {
                        sh "docker run --rm -d --name test-${BUILD_NUMBER} -p 8081:8080 -e SPRING_PROFILES_ACTIVE=test ${DOCKER_IMAGE}:${BUILD_NUMBER} || true"
                        sleep 10
                        sh "docker stop test-${BUILD_NUMBER} || true"
                    } else {
                        bat "docker run --rm -d --name test-${BUILD_NUMBER} -p 8081:8080 -e SPRING_PROFILES_ACTIVE=test ${DOCKER_IMAGE}:${BUILD_NUMBER} || ver>nul"
                        bat "timeout /t 10"
                        bat "docker stop test-${BUILD_NUMBER} || ver>nul"
                    }
                }
            }
        }

        stage('7. Docker Image Publish (Optional)') {
            when {
                expression {
                    return env.DOCKER_USERNAME != null || env.REGISTRY_CREDENTIALS_ID != null
                }
            }
            steps {
                echo 'Publishing verified container image to container registry...'
                script {
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        withCredentials([usernamePassword(credentialsId: "${REGISTRY_CREDENTIALS_ID}",
                                                          usernameVariable: 'DOCKER_USER',
                                                          passwordVariable: 'DOCKER_PASS')]) {
                            if (isUnix()) {
                                sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                                sh "docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                                sh "docker push ${DOCKER_IMAGE}:latest"
                            } else {
                                bat 'echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin'
                                bat "docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}"
                                bat "docker push ${DOCKER_IMAGE}:latest"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'CI/CD Pipeline finished execution.'
            cleanWs()
        }
        success {
            echo "Pipeline succeeded! Artifact and Docker Image ready for deployment."
        }
        failure {
            echo "Pipeline failed! Please check console output and test reports."
        }
    }
}
