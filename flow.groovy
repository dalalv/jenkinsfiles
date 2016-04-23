
#!groovy 
// Orig Ref From :: https://documentation.cloudbees.com/docs/cookbook/_setting_up_a_basic_build_pipeline_with_jenkins.html

def devQAStaging() {
    env.PATH="${tool 'Maven 3.x'}/bin:${env.PATH}"
    stage 'Dev'
    sh 'mvn clean install package'
    archive 'target/x.war'
  try {
        checkpoint('Archived war')
    } catch (NoSuchMethodError _) {
        echo 'Checkpoint feature available in Jenkins Enterprise by CloudBees.'
    }
    stage 'QA'
    parallel(longerTests: {
        runWithServer {url ->
            sh "mvn -f sometests/pom.xml test -Durl=${url} -Dduration=30"
        }
    }, quickerTests: {
        runWithServer {url ->
            sh "mvn -f sometests/pom.xml test -Durl=${url} -Dduration=20"
        }
    })
    stage name: 'Staging', concurrency: 1
    deploy 'target/x.war', 'staging'
}
def production() {
    input message: "Does http://localhost:8888/staging/ look good?"
    try {
        checkpoint('Before production')
    } catch (NoSuchMethodError _) {
        echo 'Checkpoint feature available in Jenkins Enterprise by CloudBees.'
    }
    stage name: 'Production', concurrency: 1
    node {
        unarchive mapping: ['target/x.war' : 'x.war']
        deploy 'target/x.war', 'production'
        echo 'Deployed to http://localhost:8888/production/'
    }
}
def deploy(war, id) {
    sh "cp ${war} /tmp/webapps/${id}.war"
}
def undeploy(id) {
    sh "rm /tmp/webapps/${id}.war"
}
return this;
