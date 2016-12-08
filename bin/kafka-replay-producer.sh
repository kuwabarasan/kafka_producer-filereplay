#!/bin/bash

#############################################
## Kafka Replay Producer deployment script ##
#############################################

# Current script location
DIR=$(dirname $0)

# Determine input path validity
if [ ! -d "$1" ]; then
  echo "Please provide a valid input directory path! Exiting.."
  exit 1
else
  INPUT="$1"
fi

# Check properties files
PRODUCER_PROPS=$DIR/../config/producer.properties
APP_PROPS=$DIR/../config/app.properties

if [ ! -f "$PRODUCER_PROPS" ]; then
  echo "config/producer.properties does not exist or is not a valid file! Exiting.."
  exit 1
fi

if [ ! -f "$APP_PROPS" ]; then
  echo "config/app.properties does not exist or is not a valid file! Exiting.."
  exit 1
fi

# Run env file
P_ENV=$DIR/../config/env.sh
if [ -f "$P_ENV" ]; then
  . $P_ENV
else
  echo "config/env.sh does not exist or is not a valid file! Exiting.."
  exit 1
fi

# Java home
if [ ! -d "$JAVA_HOME" ]; then
  echo "Please provide a valid JAVA_HOME path in config/env.sh!"
  exit 1
else
  JAVA=""$JAVA_HOME"/bin/java"
fi

# Classpath
if [ -z "$CLASSPATH" ]; then
  CLASSPATH=.:$DIR/../lib/*
fi

# SASL endpoint detection
grep -i 'security.protocol' $DIR/../config/producer.properties | grep SASL > /dev/null
if [ "$?" == "0" ]; then
  export KERBEROS_PARAMS=-Djava.security.auth.login.config=$JAAS_FILE
else
  export KERBEROS_PARAMS=""
fi

# log4j
LOG_PROP=-Dlog4j.configuration=file:$DIR/../config/log4j.properties
LOG_DIR=-Dkrp.log.dir=$DIR/../logs
LOG4JPROP="$LOG_PROP $LOG_DIR"

# PID file path
PID_PATH=-Dkrp.pid.path=$DIR/replayproducer.pid

nohup $JAVA -cp $CLASSPATH $KERBEROS_PARAMS $LOG4JPROP $PID_PATH hiro.kafka.clients.FileReplayProducer $PRODUCER_PROPS $APP_PROPS $INPUT 1>$DIR/../logs/replayproducer.out 2>&1 &

echo "$!" > $DIR/replayproducer.pid
