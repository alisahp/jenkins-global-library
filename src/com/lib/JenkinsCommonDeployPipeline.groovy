#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper
import hudson.FilePath


def runPipeline() {
  def common_docker = new JenkinsDeployerPipeline()
  def environment = ""
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
  def k8slabel = "jenkins-pipeline-${UUID.randomUUID().toString()}"
  def deploymentName = "${JOB_NAME}"
                        .split('/')[0]
                        .replace('-fuchicorp', '')
                        .replace('-build', '')
                        .replace('-deploy', '')

  switch(branch) {
    case 'master': environment = 'prod'
    break

    case 'qa': environment = 'qa'
    break

    case 'dev': environment = 'dev'
    break

    case 'tools': environment = 'tools'
    break

    default:
        currentBuild.result = 'FAILURE'
        print('This branch does not supported')
  }

  println("Branch: ${branch}")
  println("Environment: ${environment}")

  try {
    properties([ parameters([
      // This hard coded params should be configured inside code
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraform_apply'),
      booleanParam(defaultValue: false, description: 'Destroy deployment', name: 'terraform_destroy'),
      choice(name: 'selectedDockerImage', choices: common_docker.findDockerImages(deploymentName + '-' + environment), description: 'Please select docker image to deploy!'),
      text(name: 'deployment_tfvars', defaultValue: 'extra_values = "tools"', description: 'terraform configuration')
      ]
      )])

      // Pod slave for Jenkins so Jenkins master can run the job on slaves
      def slavePodTemplate = """
      metadata:
        labels:
          k8s-label: ${k8slabel}
        annotations:
          jenkinsjoblabel: ${env.JOB_NAME}-${env.BUILD_NUMBER}
      spec:
        affinity:
          podAntiAffinity:
            requiredDuringSchedulingIgnoredDuringExecution:
            - labelSelector:
                matchExpressions:
                - key: component
                  operator: In
                  values:
                  - jenkins-jenkins-master
              topologyKey: "kubernetes.io/hostname"
        containers:
        - name: docker
          image: docker:latest
          imagePullPolicy: Always
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
            - mountPath: /etc/secrets/service-account/
              name: google-service-account
        - name: fuchicorptools
          image: fuchicorp/buildtools
          imagePullPolicy: Always
          command:
          - cat
          tty: true
          volumeMounts:
            - mountPath: /var/run/docker.sock
              name: docker-sock
            - mountPath: /etc/secrets/service-account/
              name: google-service-account
        serviceAccountName: common-service-account
        securityContext:
          runAsUser: 0
          fsGroup: 0
        volumes:
          - name: google-service-account
            secret:
              secretName: google-service-account
          - name: docker-sock
            hostPath:
              path: /var/run/docker.sock
    """

  podTemplate(name: k8slabel, label: k8slabel, yaml: slavePodTemplate) {
      node(k8slabel) {
        container('fuchicorptools') {

          stage("Polling SCM") {
            checkout scm
          }

          stage('Generate Configurations') {
            // sh "sleep 200"
            sh """
            mkdir -p ${WORKSPACE}/deployments/terraform/
            cat  /etc/secrets/service-account/credentials.json > ${WORKSPACE}/deployments/terraform/fuchicorp-service-account.json
            ls ${WORKSPACE}/deployments/terraform/
            ## This script should move to docker container to set up ~/.kube/config
            sh /scripts/Dockerfile/set-config.sh
            """
            // sh /scripts/Dockerfile/set-config.sh Should go to Docker container CMD so we do not have to call on slave 
            deployment_tfvars += """
            deployment_name        = \"${deploymentName}\"
            deployment_environment = \"${environment}\"
            deployment_image       = \"docker.fuchicorp.com/${selectedDockerImage}\"
            credentials            = \"./fuchicorp-service-account.json\"
            """.stripIndent()

            writeFile(
              [file: "${WORKSPACE}/deployments/terraform/deployment_configuration.tfvars", text: "${deployment_tfvars}"]
              )

              sh "cat ${WORKSPACE}/deployments/terraform/deployment_configuration.tfvars"
          }
          stage('Terraform Apply/Plan') {
            if (!params.terraform_destroy) {
              if (params.terraform_apply) {

                dir("${WORKSPACE}/deployments/terraform/") {
                  echo "##### Terraform Applying the Changes ####"
                  sh '''#!/bin/bash -e
                  source set-env.sh deployment_configuration.tfvars
                  terraform apply --auto-approve -var-file=deployment_configuration.tfvars'''
                }

              } else {

                dir("${WORKSPACE}/deployments/terraform/") {
                  echo "##### Terraform Plan (Check) the Changes #### "
                  sh '''#!/bin/bash -e
                  source set-env.sh deployment_configuration.tfvars
                  terraform plan -var-file=deployment_configuration.tfvars
                  '''
                }
              }
            }
          }

          stage('Terraform Destroy') {
            if (!params.terraform_apply) {
              if (params.terraform_destroy) {
                if ( environment != 'tools' ) {
                  dir("${WORKSPACE}/deployments/terraform/") {
                    echo "##### Terraform Destroing ####"
                    sh '''#!/bin/bash -e
                    source set-env.sh deployment_configuration.tfvars
                    terraform destroy --auto-approve -var-file=deployment_configuration.tfvars'''
                  }
                } else {
                  println("""

                    Sorry I can not destroy Tools!!!
                    I can Destroy only dev and qa branch

                  """)
                }
              }
           }

           if (params.terraform_destroy) {
             if (params.terraform_apply) {
               println("""

               Sorry you can not destroy and apply at the same time

               """)
               currentBuild.result = 'FAILURE'
            }
          }
        }
       }
      }
    }
  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
  }
}



return this
