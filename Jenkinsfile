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
                    echo '拉取代码...'
                    checkout scm
                }
            }
        }
        stage('Reset Database') {
            steps {
                script {
                    def workspace = pwd()
                    echo "当前工作目录: ${workspace}"

                    // 使用 withCredentials 传递 GITHUB_TOKEN
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        def tag = sh(script: '''
                            curl -s https://api.github.com/repos/class-scheduling-system/table-install-cli/releases/latest \
                            -H "Authorization: Bearer $GITHUB_TOKEN" | grep tag_name | cut -d '"' -f4
                        ''', returnStdout: true).trim()

                        if (!tag) {
                            error "无法获取 CLI 最新 RELEASE 版本，请检查 GITHUB_TOKEN 或网络连接！"
                        }

                        echo "当前 CLI 最新 RELEASE ${tag}"

                        sh """
                            cd ${workspace}
                            rm -rf ${workspace}/cli-linux-amd64
                            curl -L --retry 3 --retry-delay 5 -o cli-linux-amd64 "https://github.com/class-scheduling-system/table-install-cli/releases/download/${tag}/cli-linux-amd64"
                            chmod +x cli-linux-amd64
                        """
                        sh "cd ${workspace}/ && ./cli-linux-amd64 reset"
                        echo "清空数据库完成"
                    }
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
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                                -Dmaven.test.failure.ignore=true
                        '''
                    }
                }
            }
        }
        stage('deploy project') {
            steps {
                ansiColor('xterm') {
                    script {
                        def workspace = pwd()
                        echo "当前工作目录: ${workspace}"

                        sh """
                            sed -i 's/spring.profiles.active: dev/spring.profiles.active: test/g' ${workspace}/src/main/resources/application.yml
                        """

                        sh '''
                            mvn clean package \
                                -Dmaven.test.failure.ignore=true
                        '''

                        sh """
                            # 上传整个文件夹到服务器
                            scp -r ${workspace}/* root@172.16.11.10:/root/project
                        """

                        sh '''
                            # 在服务器上执行以下操作
                            ssh root@172.16.11.10 "
                            # 检查是否有旧的进程PID记录
                            if [ -f /root/project/pid.txt ]; then
                                OLD_PID=\$(cat /root/project/pid.txt)
                                # 杀掉旧的进程
                                echo 'Killing old process with PID: \${OLD_PID}'
                                kill -9 \${OLD_PID}
                            fi

                            # 启动新的项目并记录PID
                            echo 'Starting new project...'
                            nohup java -jar /root/project/your-app.jar > /root/project/output.log 2>&1 &
                            NEW_PID=\$!
                            echo \${NEW_PID} > /root/project/pid.txt
                            echo 'New project started with PID: \${NEW_PID}'
                            "
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
