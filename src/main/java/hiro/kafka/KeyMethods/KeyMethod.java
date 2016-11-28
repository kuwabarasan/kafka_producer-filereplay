package hiro.kafka.KeyMethods;

import java.io.Closeable;
import java.io.File;

public interface KeyMethod {

    // Implement a method to get a Key value.
    // Make sure to have an appropriate Partitioner class to go
    // along with particular KeyMethod instance
    public String getKey(File thisFile, String topicName, String value);

}