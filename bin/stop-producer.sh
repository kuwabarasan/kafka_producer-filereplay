#!/bin/bash

DIR=$(dirname $0)

if [ ! -f $DIR/replayproducer.pid ]; then
  echo "There is no PID file."
  exit 1
fi

PID=$(cat $DIR/replayproducer.pid)
ps -ef | grep $PID | grep 'FileReplayProducer' > /dev/null
if [ $? != 0 ]; then
  echo "PID not found in process list."
else
  kill $PID
  if [ $? == 0 ]; then
    echo "Kafka replay producer has been killed."
  fi
fi
