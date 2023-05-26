pipeline {
  environment {
    devRegistry = 'ghcr.io/datakaveri/gis-dev'
    deplRegistry = 'ghcr.io/datakaveri/gis-depl'
    testRegistry = 'ghcr.io/datakaveri/gis-test:latest'
    registryUri = 'https://ghcr.io'
    registryCredential = 'datakaveri-ghcr'
    GIT_HASH = GIT_COMMIT.take(7)
  }
  agent { 
    node {
      label 'slave1' 
    }
  }
  stages {

    stage('Building images') {
      steps{
        script {
          echo 'Pulled - ' + env.GIT_BRANCH
          devImage = docker.build( devRegistry, "-f ./docker/dev.dockerfile .")
          deplImage = docker.build( deplRegistry, "-f ./docker/depl.dockerfile .")
          testImage = docker.build( testRegistry, "-f ./docker/test.dockerfile .")
        }
      }
    }

    stage('Unit Tests and CodeCoverage Test'){
      steps{
        script{
          sh 'docker compose -f docker-compose.test.yml up test'
        }
        xunit (
          thresholds: [ skipped(failureThreshold: '0'), failed(failureThreshold: '0') ],
          tools: [ JUnit(pattern: 'target/surefire-reports/*.xml') ]
        )
        jacoco classPattern: 'target/classes', execPattern: 'target/jacoco.exec', sourcePattern: 'src/main/java', exclusionPattern:'iudx/gis/server/apiserver/*.class,**/*VertxEBProxy.class,**/Constants.class,**/*VertxProxyHandler.class,**/*Verticle.class,iudx/gis/server/deploy/*.class,iudx/gis/server/databroker/DataBrokerService.class,iudx/gis/server/databroker/DataBrokerServiceImpl.class,iudx/gis/server/apiserver/validation/types/Validator.class,iudx/gis/server/metering/MeteringService.class,iudx/gis/server/cache/CacheService.class,iudx/gis/server/database/postgres/PostgresService.class,**/*JwtDataConverter.class'
      }
      post{
      always {
               recordIssues(
                             enabledForFailure: true,
                             blameDisabled: true,
                             forensicsDisabled: true,
                             qualityGates: [[threshold:0, type: 'TOTAL', unstable: false]],
                             tool: checkStyle(pattern: 'target/checkstyle-result.xml')
                            )
               recordIssues(
                             enabledForFailure: true,
                             blameDisabled: true,
                             forensicsDisabled: true,
                             qualityGates: [[threshold:0, type: 'TOTAL', unstable: false]],
                             tool: pmdParser(pattern: 'target/pmd.xml')
                            )
             }
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
          error "Test failure. Stopping pipeline execution!"
        }
        cleanup{
          script{
            sh 'sudo rm -rf target/'
          }
        }
      }
    }

    stage('Run GIS interface server'){
      steps{
        script{
            sh 'scp src/test/resources/IUDX_GIS_Server_APIs_V4.5.0.postman_collection.json jenkins@jenkins-master:/var/lib/jenkins/iudx/gis/Newman/'
            sh 'docker compose -f docker-compose.test.yml up -d integTest'
            sh 'sleep 45'
        }
      }
      post{
        failure{
          script{
            sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
        }
      }
    }

    stage('Integration Tests & ZAP pen test'){
      steps{
        node('built-in') {
          script{
            startZap ([host: 'localhost', port: 8090, zapHome: '/var/lib/jenkins/tools/com.cloudbees.jenkins.plugins.customtools.CustomTool/OWASP_ZAP/ZAP_2.11.0'])
            sh 'curl http://127.0.0.1:8090/JSON/pscan/action/disableScanners/?ids=10096'
            sh 'HTTP_PROXY=\'127.0.0.1:8090\' newman run /var/lib/jenkins/iudx/gis/Newman/IUDX_GIS_Server_APIs_V4.5.0.postman_collection.json -e /home/ubuntu/configs/gis-postman-env.json -n 2 --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/gis/Newman/report/report.html --reporter-htmlextra-skipSensitiveData'
            runZapAttack()
          }
        }
      }
      post{
        always{
          node('built-in') {
            script{
              publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/gis/Newman/report/', reportFiles: 'report.html', reportName: 'HTML Report', reportTitles: '', reportName: 'Integration Test Report'])
              archiveZap failHighAlerts: 1, failMediumAlerts: 1, failLowAlerts: 1
            }  
          }
          script{
             sh 'docker compose -f docker-compose.test.yml down --remove-orphans'
          }
        }
      }
    }

    stage('Continuous Deployment') {
      when {
        allOf {
          anyOf {
            changeset "docker/**"
            changeset "docs/**"
            changeset "pom.xml"
            changeset "src/main/**"
            triggeredBy cause: 'UserIdCause'
          }
          expression {
            return env.GIT_BRANCH == 'origin/master';
          }
        }
      }
      stages {
        stage('Push Images') {
          steps {
            script {
              docker.withRegistry( registryUri, registryCredential ) {
                devImage.push("5.0.0-alpha-${env.GIT_HASH}")
                deplImage.push("5.0.0-alpha-${env.GIT_HASH}")
              }
            }
          }
        }
        stage('Docker Swarm deployment') {
          steps {
            script {
              sh "ssh azureuser@docker-swarm 'docker service update gis_gis --image ghcr.io/datakaveri/gis-depl:5.0.0-alpha-${env.GIT_HASH}'"
              sh 'sleep 10'
            }
          }
          post{
            failure{
              error "Failed to deploy image in Docker Swarm"
            }
          }
        }
        stage('Integration test on swarm deployment') {
          steps {
            node('built-in') {
              script{
                sh 'newman run /var/lib/jenkins/iudx/gis/Newman/IUDX_GIS_Server_APIs_V4.5.0.postman_collection.json -e /home/ubuntu/configs/cd/gis-postman-env.json --insecure -r htmlextra --reporter-htmlextra-export /var/lib/jenkins/iudx/gis/Newman/report/cd-report.html --reporter-htmlextra-skipSensitiveData'
              }
            }
          }
          post{
            always{
              node('built-in') {
                script{
                  publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true, reportDir: '/var/lib/jenkins/iudx/gis/Newman/report/', reportFiles: 'cd-report.html', reportTitles: '', reportName: 'Docker-Swarm Integration Test Report'])
                }
              }
            }
            failure{
              error "Test failure. Stopping pipeline execution!"
            }
          }
        }
      }
    }
  }
  post{
    failure{
      script{
        if (env.GIT_BRANCH == 'origin/master')
        emailext recipientProviders: [buildUser(), developers()], to: '$RS_RECIPIENTS, $DEFAULT_RECIPIENTS', subject: '$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!', body: '''$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS:
Check console output at $BUILD_URL to view the results.'''
      }
    }
  }
}

