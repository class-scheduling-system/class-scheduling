node {
    agent { label 'centos' }
    environment {
            SONAR_TOKEN = credentials('xiaolfeng-sonar-token')
        }

        tools {
            maven 'maven'
        }

        stages {
            stage('SCM') {
                checkout scm
            }
            stage('SonarQube Analysis') {
                ansiColor('xterm') {
                    withSonarQubeEnv('SonarScanner') {
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
}
