# Google cloud function for logging
Google PubSub cloud function for receiving log events from google stack driver

These log events are then routed to Slack and for ALERT logs they trigger a 
VictorOps alarm

## Prerequisites

### Slack account
Register a Slack app with access to your workspace. It needs
to have the following scopes; 

`chat:write`

`chat:write.public`

Associate your created Slack app with your workspace and take note 
of your Bot User OAuth Token, you will need it when setting the environment
variables.


### VictorOps account
Register a VictorOps account and activate the Rest integration and set up 
a routing key for where you want the alerts to be routed to.

The url will look something like this 

`https://alert.victorops.com/integrations/generic/[accountId]/alert/[secretKey]/[routingKey]`

You will need to set the accountId, secretKey and rountingKey in your environment
variables for the cloud function

### Google cloud platform account
You will also need a Google cloud platform account where you already have your services
running, and activate the cloud function api.

Have your logs set up so that you can see them under your Logs Explorer
Preferably you should already have monitoring and alerting setup. So that 
you can use the same log-based metrics for sending these events to your 
cloud function. 

You should also have `gcloud` installed and configured so that it can access
the projects where you want to deploy the cloud function.

### Development environment
You need Java 11 and Gradle installed on your developer machine.

## Getting started

Clone the project and build it locally 

`./gradlew build`

### Setting up the pubsub topic
Set up the topic you are going to publish log events to and that the cloud function will act
as a consumer for

`gcloud pubsub topics create GSLACK_LOG_EVENTS`

### Deploying the cloud function
If you do not set the environment variables (all the variables) the logger will just system exit on startup

Create an `env.yaml` file in gcloud-logging-pubsub to contain your credentials

```yaml
projectId: [projectId]
pubSub: GSLACK_LOG_EVENTS
SLACK_API_KEY: xoxb-
SLACK_CHANNEL: channel
KUBE_PROJECT_IDS: [list of your kube project ids separated with ,]
VICTOR_OPS_ACCOUNT_ID: [victorCompanyId]
VICTOR_OPS_SECRET_KEY: [victorSecretKey]
VICTOR_OPS_ROUTING_KEY: [victorRoutingKey]
```
Then you can just call the `deployFunction` from gradle 

### Configure your log metrics
Now just go to your log metrics in google cloud console and have them publish their events 
to your topic and profit
