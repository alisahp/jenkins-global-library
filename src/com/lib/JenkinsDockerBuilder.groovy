#!/usr/bin/env groovy

package com.lib
import groovy.json.JsonSlurper
import hudson.EnvVars


def runPipeline() {

  /**
  *  In feature we would like to create file build.yaml
  *  1. Read all required config from build.yaml
  *  2. Build the docker images
  *  3. Unitest the application
  *  4. Push the application
  */


  def commonDeployer = new com.lib.JenkinsDeployerPipeline()
  def messanger = new com.lib.JenkinsNotificator()
  def slackChannel = "devops-alerts"
  def repositoryName = "${JOB_NAME}"
      .split('/')[0]
      .replace('-mybestsea', '')
      .replace('-build', '')
      .replace('-deploy', '')
  def dockerImage
  def branch = "${scm.branches[0].name}".replaceAll(/^\*\//, '').replace("/", "-").toLowerCase()


  echo "The branch name is: ${branch}"

  switch(branch) {
    case 'master':
    repositoryName = repositoryName + '-prod'
    break

    case 'qa':
    repositoryName = repositoryName +  '-qa'
    break

    case 'dev':
    repositoryName = repositoryName + '-dev'
    break

    default:
        repositoryName = null
        currentBuild.result = 'FAILURE'
        print('You are using unsupported branch name')
        messanger.sendMessage("slack", "FAILURE", slackChannel)
  }


  /**
  * This library for now building image only to webplatform application
  */
  properties([
    parameters([
      booleanParam(defaultValue: false,
        description: 'Click this if you would like to deploy to latest',
        name: 'PUSH_LATEST'
        )])])

  try {
    node {
      checkout scm


      messanger.sendMessage("slack", "STARED", slackChannel)
      stage('Build docker image') {

          // Build the docker image
          dockerImage = docker.build(repositoryName, "--build-arg branch_name=${branch} .")
      }

      stage('Push image') {

         // Push image to the Nexus with new release
          docker.withRegistry('https://hub.docker.com/', 'nexus-private-admin-credentials') {
              dockerImage.push("0.${BUILD_NUMBER}")
              messanger.sendMessage("slack", "SUCCESS", slackChannel)


              if (params.PUSH_LATEST) {
                messanger.sendMessage("slack", "PUSHED", slackChannel,
                """############### This Job pushed to latest version. ###############""".stripIndent())
                dockerImage.push("latest")
            }
          }
       }

       stage('clean up') {
         sh "docker rmi docker.mybestsea.com/${repositoryName}:0.${BUILD_NUMBER} --force "
         sh "docker rmi docker.mybestsea.com/${repositoryName}:latest --force"
         sh "rm -rf ${WORKSPACE}/*"
       }

      }
  } catch (e) {
    currentBuild.result = 'FAILURE'
    println("ERROR Detected:")
    println(e.getMessage())
    messanger.sendMessage("slack", "FAILURE", slackChannel)
  }
}


return this
