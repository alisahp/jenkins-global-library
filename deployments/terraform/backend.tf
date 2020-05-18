terraform {
  backend "gcs" {
    bucket  = "craftapi"
    prefix  = "dev/craft"
    project = "mybestsea"
  }
}
