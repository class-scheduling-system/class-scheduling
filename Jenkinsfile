pipeline {
    agent { label 'centos' }
    environment {
        SONAR_TOKEN = credentials('xiaolfeng-sonar-token')
        BRANCH_NAME = env.GH_BRANCH
    }

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    stages {
        stage('Check Branch and Start Build') {
            steps {
                script {
                    // 获取当前分支
                    echo "当前分支: ${BRANCH_NAME}"

                    // 判断当前分支，如果不是 master，则终止构建
                    if (BRANCH_NAME != 'master') {
                        echo "当前不是 master 分支，终止构建。"
                        error "Not on master branch. The pipeline has been aborted."
                    }
                    echo "在 master 分支，继续执行构建流程。"
                }
            }
        }
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
        stage('Reset Database Again') {
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
        stage('deploy project') {
            steps {
                ansiColor('xterm') {
                    script {
                        def workspace = pwd()
                        echo "当前工作目录: ${workspace}"

                        // 定义账号密码
                        def serverPassword = '123456'  // 目标服务器的密码

                        // 修改 application.yaml 文件，将 Redis 配置文件的 dev 改为 test
                        sh """
                            sed -i 's/active: dev/active: test/' ${workspace}/src/main/resources/application.yaml
                        """

                        // 打包项目
                        sh 'mvn clean package -Dmaven.test.skip=true'

                        // 使用 sshpass 和密码进行文件上传
                        sh """
                            sshpass -p ${serverPassword} scp -r ${workspace}/target/*.jar root@172.16.11.10:/root/project/
                        """

                        // 获取打包的 .jar 文件(不含地址名字)
                        def jarFile = sh(script: 'ls target/*.jar | awk -F "/" \'{print $2}\'', returnStdout: true).trim()

                        // 在服务器上执行操作，杀掉旧进程并启动新项目
                        sh """
                            sshpass -p ${serverPassword} ssh root@172.16.11.10 '
                            # 检查是否有旧的进程PID记录
                            if [ -f /root/project/pid.txt ]; then
                                OLD_PID=\$(cat /root/project/pid.txt)
                                # 杀掉旧的进程
                                echo "Killing old process with PID: \${OLD_PID}"
                                kill -9 \${OLD_PID}
                            fi

                            # 启动新的项目并记录PID
                            echo "Starting new project..."
                            nohup java -jar /root/project/${jarFile} > /root/project/output.log 2>&1 &
                            NEW_PID=\$!
                            echo \${NEW_PID} > /root/project/pid.txt
                            echo "New project started with PID: \${NEW_PID}"
                            '
                        """
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
