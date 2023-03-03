terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.33.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}

variable "product" {}

variable "common_tags" {}

variable "env" {}
