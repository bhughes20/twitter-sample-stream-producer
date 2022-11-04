# twitter-sample-stream-producer

An application designed to produce events as part of an end-to-end streaming pipeline.   
The events being produced are tweets from the [Twitter API sampled stream](https://developer.twitter.com/en/docs/twitter-api/tweets/volume-streams/quick-start/sampled-stream).  

## Technologies

Uses Java 11 and Gradle.

## Pre-requisites

Sign up for a Twitter Developer account to get a bearer token for authorisation. Set the bearer token in a .env file at the project root.  
Sign up for a confluent cloud account and follow documents/tutorials for configuring application properties with [Confluent Developer](https://developer.confluent.io/).
