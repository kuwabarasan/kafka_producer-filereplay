package hiro.kafka.clients;


import hiro.kafka.FileFilters.SimpleFilter;
import hiro.kafka.KeyMethods.KeyMethod;
import hiro.kafka.KeyMethods.RandomKeyer;
import org.apache.commons.io.FileUtils;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Paths;
import java.util.Properties;

public class FileReplayProducer {

    /*
    #############################
    #### High-level overview ####
    #############################

    This is a file replay producer.

    It requires an input directory path as an argument.  It will navigate through
    the input directory path, and send messages based on the files existing there.

    Directory recursion can be turned on or off.

    By default, it will send every line in the files encountered.

    There will be a file-type parameter which will expose additional options based
    on the file type.  For example, a JSON type which allows JSON parsing for a
    specific JSON-path equating to a message key.
    */

    private static final String TOPIC   =  "topic.name";
    private static final String BYLINE  =  "readbyline.enable";
    private static final String REPEAT  =  "repeat.mode.enable";
    private static final String ITERNUM =  "repeat.iterations";
    private static final String PAUSE   =  "repeat.iteration.pause";

    private static final Logger LOG = Logger.getLogger(FileReplayProducer.class);

    public static void main(String args[]) throws IOException {

        // Shutdown hook for cleaning pid
        Runtime.getRuntime().addShutdownHook(new Thread(new OnShutDown()));

        // Argument check
        if (args.length != 3) {
            System.out.println("Please provide three arguments:" + '\n');
            System.out.println("(1) Kafka producer properties");
            System.out.println("(2) Application properties path");
            System.out.println("(3) Input directory path");
            return;
        }

        LOG.info("Loading producer.properties file");
        // Read in producer properties file
        Properties kafkaProps = new Properties();
        try (FileReader kafkaPropsFileReader = new FileReader(args[0])) {
            kafkaProps.load(kafkaPropsFileReader);
        }

        LOG.info("Loading app.properties file");
        // Reader in application properties file
        Properties appProps = new Properties();
        try (FileReader appPropsReader = new FileReader(args[1])) {
            appProps.load(appPropsReader);
        }

        LOG.info("Initializing application variables");
        // Initialize application property variables
        String theTopic = appProps.getProperty(TOPIC);
        boolean byLine = Boolean.parseBoolean(appProps.getProperty(BYLINE));
        boolean repeatList = Boolean.parseBoolean(appProps.getProperty(REPEAT));
        int iternum = Integer.parseInt(appProps.getProperty(ITERNUM));
        long repeatPause = Long.parseLong(appProps.getProperty(PAUSE));

        // iternum check
        if ( (iternum <= 0) && (iternum != -1) ){
            LOG.error("Invalid 'repeat.iterations' value.  Must be a positive integer or -1.");
            return;
        }

        // Check if input path is directory
        File inputDirectory = new File(args[2]);
        if (!inputDirectory.isDirectory()) {
            LOG.error("Input path must be a directory.");
            return;
        }

        // Create configHolder object
        // Filter and KeyMethod objects must be passed in base on file-type
        ConfigHolder configured = new ConfigHolder(theTopic, byLine, new SimpleFilter(), new RandomKeyer());

        // Initialize KafkaProducer
        Producer<String, String> theProducer = new KafkaProducer<>(kafkaProps);

        // *Basic Replay options:
        //  - Replay infinitely or just-once
        //  - Recursive mode
        // *Advanced replay options
        //  - Partition keying based on a JSON tag

        // Traverse the directory list filing
        try {
            if(repeatList && iternum >= 0) {
                for(int i = iternum; i > 0; i--){
                    FileWalker(inputDirectory, theProducer, configured);
                    Thread.sleep(repeatPause);
                }
            } else {
                do {
                    FileWalker(inputDirectory, theProducer, configured);
                    Thread.sleep(repeatPause);
                } while (repeatList);
            }
        } catch (InterruptedException e){
            LOG.error(e);
        } finally {
            theProducer.close();
        }

    }

    public static class ConfigHolder{
        private String topic;
        private boolean byLine;
        private FileFilter filter;
        private KeyMethod keyer;

        ConfigHolder (String topic, boolean byLine, FileFilter filter, KeyMethod keyer){
            this.topic = topic;
            this.byLine = byLine;
            this.filter = filter;
            this.keyer = keyer;
        }

        public String getTopic(){
            return topic;
        }
        public boolean getByLine(){
            return byLine;
        }
        public FileFilter getFileFilter(){
            return filter;
        }
        public KeyMethod getKeyMethod(){
            return keyer;
        }
    }

    public static void FileWalker(File thisDirectory, Producer<String, String> producer, ConfigHolder configured) {

        File[] fileListing = thisDirectory.listFiles(configured.getFileFilter());

        // For a specific file-type, implement a filter to pass in, e.g. =>
        // File[] fileListing = thisDirectory.listFiles(new JsonOrDirFilter());

        // If there is no specific key scheme specified, use a simple incrementing key
            for (File f : fileListing) {
                if (f.isDirectory())
                    FileWalker(f, producer, configured);
                else {
                    //Parse and send
                    try{
                        ProcessAndSend(f, producer, configured);
                    } catch (IOException e) {
                        LOG.error("IOException encountered during process and send of file" + f.getName(), e);
                    }
                }
            }
    }

    public static void ProcessAndSend(File thisFile, Producer<String, String> producer, ConfigHolder configured) throws IOException {

       String topic = configured.getTopic();

        if(configured.getByLine()) {
            String thisLine;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(thisFile))) {
                while ((thisLine = bufferedReader.readLine()) != null) {
                    producer.send(new ProducerRecord<String, String>(topic, configured.getKeyMethod().getKey(thisFile, topic, thisLine), thisLine));
                }
            }
        }
        else{
            String thisMessage = FileUtils.readFileToString(thisFile, "UTF-8");
            producer.send(new ProducerRecord<String, String>(topic, configured.getKeyMethod().getKey(thisFile, topic, thisMessage), thisMessage));
        }
    }

    private static void OnExit() throws IOException {
        String pidpath = System.getProperty("krp.pid.path");
        File pidfile = new File(pidpath);
        if (pidfile.isFile()){
            LOG.info("Removing PID file...");
            pidfile.delete();
        } else
            throw new FileNotFoundException();
        LOG.info("Exiting");
    }

    public static class OnShutDown implements Runnable {

        public void run(){
            LOG.info("Shutting down...");
            try{
                OnExit();
            } catch (Exception e) {
                LOG.error("Failure removing PID file.", e);
            }
        }
    }

}