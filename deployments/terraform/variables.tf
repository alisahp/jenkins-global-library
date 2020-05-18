variable "deployment_environment" {
  default = "dev"
}

variable "deployment_endpoint" {
  type = "map"

  default = {
    test  = "test.mybestsea.com"
    dev  = "dev.mybestsea.com"
    qa   = "qa.mybestsea.com"
    prod = "prod.mybestsea.com"
  }
}
variable "deployment_image" {
  default = "hub.docker.com/u/alisahp/myapp:latest"
}
