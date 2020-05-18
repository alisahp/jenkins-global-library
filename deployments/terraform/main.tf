module "cramis-deploy" {
  source  = "mybestsea/chart/helm"
  deployment_name        = "myapp-platform"
  deployment_environment = "${var.deployment_environment}"
  deployment_endpoint    = "${lookup(var.deployment_endpoint, "${var.deployment_environment}")}"
  deployment_path        = "myapp"

  template_custom_vars  = {     
    deployment_image     = "${var.deployment_image}"    
  }
}