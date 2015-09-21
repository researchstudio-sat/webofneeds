package siren_matcher;

/**
 * Created by soheilk on 26.08.2015.
 */
public class Configuration {
    public static String sIREnUri = "http://localhost:8983/solr/won3/"; //take care of the core name

    public static boolean ACTIVATE_DEBUGGING_LOGGS = true;

    //the following boolean variables can be used to switch different types of query builders off or on.
    public static boolean ACTIVATE_TITEL_BASED_QUERY_BUILDER = true;
    public static boolean ACTIVATE_DESCRIPTION_BASED_QUERY_BUILDER = false;
    public static boolean ACTIVATE_TITEL_AND_DESCRIPTION_BASED_QUERY_BUILDER = false;


    public static int NUMBER_OF_HINTS = 100;

    public static String ENGLISH_LANGUAGE_TAGGING_RESOURCES_NAME = "/en-pos-maxent.bin";

    public static int NUMBER_OF_CONSIDERED_TOKENS = 10; //Important: You have to increase the TomCat maxHttpHeaderSize to 100000 for <Connector port="8983"...>
}
