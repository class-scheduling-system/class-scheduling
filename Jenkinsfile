node {
  stage('SCM') {
    checkout scm
  }
  stage('SonarQube Analysis') {
    def mvn = tool 'Default Maven';
    withSonarQubeEnv() {
      sh "${mvn}/bin/mvn clean verify sonar:sonar -Dsonar.projectKey=class-scheduling-system_class-scheduling_adf82c13-2ed9-4412-b641-55fe84228bae -Dsonar.projectName='class-scheduling'"
    }
  }
}
