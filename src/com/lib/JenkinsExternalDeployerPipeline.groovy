#!/usr/bin/env groovy
package com.lib
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat


def runPipeline() {


  def environment    = ""
  def branch         = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()
  def messanger      = new com.lib.JenkinsNotificator()
  def slackChannel   = "devops-alerts"
  def branchName     = "${scm.branches[0].name}".replaceAll(/^\*\//, '')
  String dateTime    = new SimpleDateFormat("yyyy/MM/dd.HH-mm-ss").format(Calendar.getInstance().getTime())
  def credId         = scm.getUserRemoteConfigs()[0].getCredentialsId()
  String repoUrl     = scm.getUserRemoteConfigs()[0].getUrl().replace('https://', '')
  def deploymentName = "${JOB_NAME}"
                        .split('/')[0]
                        .replace('-mybestsea', '')
                        .replace('-build', '')
                        .replace('-deploy', '')

  switch(branch) {
    case "master": environment = "prod"
    branch = "prod"
    break

    case "qa": environment = "qa"
    break

    case "dev": environment = "dev"
    break

    default:
        currentBuild.result = 'FAILURE'
        println('This branch does not supported')
  }

  try {
    properties([ parameters([
      choice(name: 'selectedDockerImage', choices: findDockerImages(deploymentName), description: 'Please select docker image to deploy!'),
      booleanParam(defaultValue: false, description: 'Apply All Changes', name: 'terraformApply'),
      booleanParam(defaultValue: false, description: 'Destroy deployment', name: 'terraformDestroy'),
      string(defaultValue: 'mybestsea-google-service-account', name: 'common_service_account', description: 'Please enter service Account ID', trim: true),
      string(defaultValue: 'webplatform-configuration', name: 'deployment_configuration', description: 'Please enter configuration name', trim: true)
      ])])

      node('master') {
        withCredentials([
          file(credentialsId: "${common_service_account}", variable: 'common_user'),
           file(credentialsId: "${deployment_configuration}", variable: 'deployment_config')]) {
            messanger.sendMessage("slack", "STARED", slackChannel)
            stage('Poll code') {
              checkout scm
              sh """#!/bin/bash -e
              cp -rf ${common_user} ${WORKSPACE}/deployment/terraform/mybestsea-service-account.json
              """
            }

          stage('Generate Vars') {
            def file = new File("${WORKSPACE}/deployment/terraform/deployment_configuration.tfvars")
            file.write """
            deployment_environment    =  "${environment}"
            deployment_name           =  "${deploymentName}"
            deployment_image          =  "docker.mybestsea.com/${selectedDockerImage}"
            credentials               =  "./mybestsea-service-account.json"
            """.stripIndent()
            sh "cat ${deployment_config} >> ${WORKSPACE}/deployment/terraform/deployment_configuration.tfvars"
          }

          stage('Terraform Apply/Plan') {
            if (!params.terraformDestroy) {
              if (params.terraformApply) {

                dir("${WORKSPACE}/deployment/terraform") {
                  echo "##### Terraform Applying the Changes ####"
                  sh '''#!/bin/bash -e
                  source set-env.sh ./deployment_configuration.tfvars
                  terraform apply --auto-approve -var-file=$DATAFILE'''
                  messanger.sendMessage("slack", "APPLIED", slackChannel)
                }
                if (branch == "prod") {
                  sh("""
                    git config --global user.email 'jenkins@mybestsea.com'
                    git config --global user.name  'Jenkins'
                    git config --global credential.helper cache
                    """)
                    tagForGit = "deploy_prod_${dateTime}"
                    sh("git clone http://${repoUrl} ${WORKSPACE}/git_tagger")
                      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: "${credId}", usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                        dir("${WORKSPACE}/git_tagger") {
                          sh("""git tag -a '${tagForGit}' -m 'Jenkins deployment has been deployed successfully. time: ${dateTime}'
                          git push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@${repoUrl} --tags""")
                        }
                    sh("rm -rf ${WORKSPACE}/git_tagger")
                  }
                }

              } else {

                  dir("${WORKSPACE}/deployment/terraform") {
                    echo "##### Terraform Plan (Check) the Changes #####"
                    sh "cat ./deployment_configuration.tfvars"
                    sh '''#!/bin/bash -e
                    source set-env.sh ./deployment_configuration.tfvars
                    terraform plan -var-file=$DATAFILE'''
                    messanger.sendMessage("slack", "PLANED", slackChannel)
                  }

              }
            }
          }

          stage('Terraform Destroy') {
            if (!params.terraformApply) {
              if (params.terraformDestroy) {
                if ( branch.toLowerCase() != "prod" ) {
                  dir("${WORKSPACE}/deployment/terraform") {
                    echo "##### Terraform Destroing #####"
                    sh '''#!/bin/bash -e
                    source set-env.sh ./deployment_configuration.tfvars
                    terraform destroy --auto-approve -var-file=$DATAFILE'''
                    messanger.sendMessage("slack", "DESTROYED", slackChannel)
                  }
                } else {
                  println("""

                    Sorry I can not destroy PROD!!!
                    I can Destroy only dev and qa branch

                  """)
                }
              }
           }

           if (params.terraformDestroy) {
             if (params.terraformApply) {
               println("""
               Sorry you can not destroy and apply at the same time
               """)
             }
         }
       }
     }
   }

  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
    messanger.sendMessage("slack", "FAILURE", slackChannel)
  }
}

import groovy.json.JsonSlurper


def findDockerImages(nameApp) {
  def foundRepo    = ""
  def token        = ""
  def myJsonreader = new JsonSlurper()
  def nexusData = myJsonreader.parse(new URL("https://registry.hub.docker.com/v2/repositories/mybestsea/"))
  def repoVersions = []

  nexusData.results.each {
    if (it.name ==  nameApp ) {
        foundRepo = it.name
      }
    }

  nexusData = myJsonreader.parse(new URL("https://registry.hub.docker.com/v2/repositories/mybestsea/${foundRepo}/tags/"))

  nexusData.results.each {
    repoVersions.add( "${foundRepo}:" +  it.name)
  }

  if(!repoVersions) {
    versionList.add('ImmageNotFound')
  }

  return repoVersions.sort()
}


return this
