pipeline {
    agent { label 'centos' }
    environment {
        SONAR_TOKEN = credentials('xiaolfeng-sonar-token')
    }

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    stages {
        stage('SCM') {
            steps {
                ansiColor('xterm') {
                    echo(message: '拉取代码...')
                    checkout scm  // 拉取代码
                }
            }
        }
        stage('SonarQube Analysis') {
            steps {
                ansiColor('xterm') {
                    withSonarQubeEnv('SonarScanner') {
                        sh 'java -version'
                        sh '''
                            mvn clean verify sonar:sonar \
                                -Dsonar.projectKey=class-scheduling-system_class-scheduling_adf82c13-2ed9-4412-b641-55fe84228bae \
                                -Dsonar.projectName="class-scheduling" \
                                -Dsonar.token=${SONAR_TOKEN}
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo(message: 'SonarQube 分析完成 🎉')
        }
        failure {
            error(message: 'SonarQube 分析失败，请检查日志！')
        }
    }
}
