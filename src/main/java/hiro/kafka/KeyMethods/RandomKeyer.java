package hiro.kafka.KeyMethods;

import java.io.File;
import java.util.Random;

public class RandomKeyer implements KeyMethod{

    @Override
    public String getKey(File thisFile, String topicName, String value) {
        Random rand = new Random();
        int posRandInt = rand.nextInt( Integer.MAX_VALUE );
        return Integer.toString(posRandInt);
    }
}
