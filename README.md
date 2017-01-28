# kafka_producer-filereplay

Kafka producer implementation (new producer) which recursively replays files within an input directory provided as an argument.

##To deploy:

- Run git clone:
```
git clone https://github.com/kuwabarasan/kafka_producer-filereplay
```
- Confirm and change as necessary the artifact versions in pom.xml
- Install maven if you do not have it, and preferrably add the maven bin directory to $PATH
- Run the following:
```
mvn clean package
```
- This should have created a target directory.  Copy over the jar in the target directory into the lib directory:
```
cp target/kafkatools-0.0.1-SNAPSHOT.jar lib
```
- Modify the following files to suit your testing needs and cluster properties:
```
config/app.properties
config/env.sh
config/producer.properties
```
- Finally, run the following and you are good to go:
```
bin/kafka-replay-producer.sh <input_directory>
```
If all is well, you should see a PID file in the bin/ directory.  If you do not, check the logs under logs/ directory.
##To stop:
To stop the producer at any time:
```
bin/stop-producer.sh
```

That's it!
