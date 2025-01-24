pipeline {
    agent { label 'centos' }
    environment {
        SONAR_TOKEN = credentials('xiaolfeng-sonar-token')
        GITHUB_TOKEN = credentials('github-token')
    }

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    stages {
        stage('SCM') {
            steps {
                ansiColor('xterm') {
                    echo '拉取代码...'
                    checkout scm  // 拉取代码
                }
            }
        }
        stage('clear database') {
            steps {
                script {
                    def workspace = pwd()
                    echo "当前工作目录: ${workspace}"
                    sh "cd ${workspace}"
                    sh '''
                        rm -rf ./cli-linux-amd64
                        tag=$(curl -s https://api.github.com/repos/class-scheduling-system/table-install-cli/releases/latest -H "Authorization: Bearer ${GITHUB_TOKEN}" | grep tag_name | cut -f4 -d \")
                        echo "$tag"
                        wget https://github.com/class-scheduling-system/table-install-cli/releases/download/$tag/cli-linux-amd64
                        chmod +x cli-linux-amd64
                    '''
                    sh "./cli-linux-amd64 reset"
                    echo "清空数据库完成"
                }
            }
        }
        stage('Prepare Environment') {
            steps {
                script {
                    def workspace = pwd()
                    echo "当前工作目录: ${workspace}"
                    sh """
                    sed -i '/^host = localhost/a password = 123456' ${workspace}/src/main/resources/config/redis.setting
                    sed -i 's/host = localhost/host = 172.16.11.3/' ${workspace}/src/main/resources/config/redis.setting
                    """
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
                                -Dspring.profiles.active=test \
                                -Dsonar.projectKey=class-scheduling-system_class-scheduling_adf82c13-2ed9-4412-b641-55fe84228bae \
                                -Dsonar.projectName="class-scheduling" \
                                -Dsonar.token=${SONAR_TOKEN} \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'SonarQube 分析完成 🎉'
        }
        failure {
            error 'SonarQube 分析失败，请检查日志！'
        }
    }
}
