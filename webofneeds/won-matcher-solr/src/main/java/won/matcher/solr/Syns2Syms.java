package won.matcher.solr;

/**
 * Based off of the Lucene prolog parser in the wordnet contrib package within the
 * main Lucene project. It has been modified to remove the Lucene bits and generate
 * a synonyms.txt file suitable for consumption by Solr. The idea was mentioned in
 * a sidebar of the book Solr 1.4 Enterprise Search Server by Eric Pugh.
 *
 * @see <a href="http://lucene.apache.org/java/2_3_2/lucene-sandbox/index.html#WordNet/Synonyms">Lucene Sandbox WordNet page</a>
 * @see <a href="http://svn.apache.org/repos/asf/lucene/dev/trunk/lucene/contrib/wordnet/">SVN Repository of the WordNet contrib</a>
 * @see <a href="https://www.packtpub.com/solr-1-4-enterprise-search-server/book">Solr 1.4 Enterprise Search Server Book</a>
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Convert the prolog file wn_s.pl from the <a href="http://www.cogsci.princeton.edu/2.0/WNprolog-2.0.tar.gz">WordNet prolog download</a>
 * into a text file suitable for Solr synonym matching
 *
 * This has been tested with WordNet 3.0.
 *
 * <p>
 * The source word is the first entry, followed by a comma separated list of synonyms
 * </p>
 * <p>
 * While the WordNet file distinguishes groups of synonyms with
 * related meanings we don't do that here.
 * </p>
 *
 *
 * @see <a href="http://www.cogsci.princeton.edu/~wn/">WordNet home page</a>
 * @see <a href="http://www.cogsci.princeton.edu/~wn/man/prologdb.5WN.html">prologdb man page</a>
 */
public class Syns2Syms {
  /**
   *
   */
  private static final PrintStream o = System.out;

  /**
   *
   */
  private static final PrintStream err = System.err;

  /**
   * Takes arg of prolog file name and output file
   */
  public static void main(String[] args) throws Throwable {
    // get command line arguments
    String prologFilename = null; // name of file "wn_s.pl"
    String outputFilename = null;
    if (args.length == 2) {
      prologFilename = args[0];
      outputFilename = args[1];
    }
    else {
      usage();
      System.exit(1);
    }

    // ensure that the prolog file is readable
    if (! (new File(prologFilename)).canRead()) {
      err.println("Error: cannot read Prolog file: " + prologFilename);
      System.exit(1);
    }
    // ensure that the output file is writeable
    if (! (new File(outputFilename)).canWrite()) {
      if (! (new File(outputFilename)).createNewFile()) {
        err.println("Error: cannot write output file: " + outputFilename);
        System.exit(1);
      }
    }

    o.println("Opening Prolog file " + prologFilename);
    final FileInputStream fis = new FileInputStream(prologFilename);
    final BufferedReader br = new BufferedReader(new InputStreamReader(fis));
    String line;

    // maps a word to all the "groups" it's in
    final Map<String,List<String>> word2Nums = new TreeMap<String,List<String>>();
    // maps a group to all the words in it
    final Map<String,List<String>> num2Words = new TreeMap<String,List<String>>();
    // number of rejected words
    int ndecent = 0;

    // status output
    int mod = 1;
    int row = 1;

    // parse prolog file
    o.println( "[1/2] Parsing " + prologFilename);
    while ((line = br.readLine()) != null) {
      // occasional progress
      if ((++row) % mod == 0) { // periodically print out line we read in
        mod *= 2;
        o.println("\t" + row + " " + line + " " + word2Nums.size() + " " + num2Words.size() + " ndecent=" + ndecent);
      }

      // syntax check
      if (! line.startsWith("s(")) {
        err.println("OUCH: " + line);
        System.exit(1);
      }

      // parse line
      line = line.substring(2);
      int comma = line.indexOf(',');
      String num = line.substring(0, comma);
      int q1 = line.indexOf('\'');
      line = line.substring(q1 + 1);
      int q2 = line.lastIndexOf('\'');
      String word = line.substring(0, q2).toLowerCase().replace("''", "'");

      // make sure is a normal word
      if (! isDecent(word)) {
        ndecent++;
        continue; // don't store words w/ spaces
      }

      // 1/2: word2Nums map
      // append to entry or add new one
      List<String> lis = word2Nums.get(word);
      if (lis == null) {
        lis = new LinkedList<String>();
        lis.add(num);
        word2Nums.put(word, lis);
      }
      else {
        lis.add(num);
      }

      // 2/2: num2Words map
      lis = num2Words.get(num);
      if (lis == null) {
        lis = new LinkedList<String>();
        lis.add(word);
        num2Words.put(num, lis);
      }
      else
        lis.add(word);
    }

    // close the streams
    fis.close();
    br.close();

    // create the index
    o.println( "[2/2] Building index to store synonyms, " + " map sizes are " + word2Nums.size() + " and " + num2Words.size());
    index(outputFilename, word2Nums, num2Words);
  }

  /**
   * Checks to see if a word contains only alphabetic characters by
   * checking it one character at a time.
   *
   * @param s string to check
   * @return <code>true</code> if the string is decent
   */
  private static boolean isDecent(String s) {
    int len = s.length();
    for (int i = 0; i < len; i++) {
      if (!Character.isLetter(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Forms a static text file based on the 2 maps.
   *
   * @param outputFileName the file where the synonyms should be created
   * @param word2Nums
   * @param num2Words
   */
  private static void index(String outputFileName, Map<String,List<String>> word2Nums, Map<String,List<String>> num2Words) throws Throwable {
    int row = 0;
    int mod = 1;

    o.println("Opening output file");
    FileWriter output_writer = new FileWriter(outputFileName);

    try {
      Iterator<String> i1 = word2Nums.keySet().iterator();
      while (i1.hasNext()) { // for each word
        String g = i1.next();
        StringBuilder builder = new StringBuilder();

        builder.append(g);

        int n = index(word2Nums, num2Words, g, builder);
        if (n > 0) {
          //doc.add( new Field( F_WORD, g, Field.Store.YES, Field.Index.NOT_ANALYZED)); // Add root word
          if ((++row % mod) == 0) {
            o.println("\trow=" + row + "/" + word2Nums.size() + " builder= " + builder);
            mod *= 2;
          }

          builder.append("\n");
          output_writer.write(builder.toString());
        } // else degenerate
      }
    } finally {
      output_writer.close();
    }
  }

  /**
   * Given the 2 maps fills a document for 1 word.
   */
  private static int index(Map<String,List<String>> word2Nums, Map<String,List<String>> num2Words, String g, StringBuilder builder) throws Throwable {
    List<String> keys = word2Nums.get(g); // get list of key#'s
    Iterator<String> i2 = keys.iterator();

    Set<String> already = new TreeSet<String>(); // keep them sorted

    // pass 1: fill up 'already' with all words
    while (i2.hasNext()) { // for each key#
      already.addAll(num2Words.get(i2.next())); // get list of words
    }
    int num = 0;
    already.remove(g); // of course a word is it's own syn
    Iterator<String> it = already.iterator();
    while (it.hasNext()) {
      String cur = it.next();
      // don't store things like 'pit bull' -> 'american pit bull'
      if (!isDecent(cur)) {
        continue;
      }
      num++;
      builder.append(", ");
      builder.append(cur);
    }
    return num;
  }

  /**
   * Usage message to aide nooblets
   */
  private static void usage() {
    o.println("\n\n" + "Generates the appropriate synonyms in a format for Apache Solr\nUsage: java Syns2Syms <prolog file> <output file>\nExample: java Syns2Syms prologwn/wn_s.pl synonyms.txt\n");
  }
}
