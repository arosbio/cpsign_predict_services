# README #

Prediction service for runnign cpsign on, _e.g._, OpenShift. A license and model jar file is needed for running the services 

### What is this repository for? ###

* Prediction service for cpsign
* Version: 0.0.1

### Set up ###
When starting the prediction service the server will need both a CPSign lincese and a model, these can be injected and specified in two different ways each:

__License:__
First the server will check if the envioronment variable "LICENSE_FILE" is set, if it is set it will handle this as URI to a valid license. Otherwise the server will check in the location "/opt/app-root/modeldata/license.license". If there is no license / invalid license the setup will fail.

__Model:__
First the server will check if the envioronment variable "MODEL_FILE" is set, if it is set it will handle this as URI to a model. Otherwise the server will check in the location "/opt/app-root/modeldata/model.jar". If there is no model the setup will fail.
