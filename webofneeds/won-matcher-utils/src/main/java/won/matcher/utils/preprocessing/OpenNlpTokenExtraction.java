package won.matcher.utils.preprocessing;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Class uses OpenNLP to extract tokens from a text. Used for pre-processing the
 * need attribute content.
 * <p>
 * User: hfriedrich Date: 13.06.2015
 */
public class OpenNlpTokenExtraction {
  Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
  POSTaggerME posTagger = null;

  public OpenNlpTokenExtraction() throws IOException {

    InputStream modelIn = this.getClass().getClassLoader().getResourceAsStream("en-pos-maxent.bin");
    POSModel model = new POSModel(modelIn);
    posTagger = new POSTaggerME(model);
  }

  /**
   * Extract tokens that are words and have length > 1 from a text
   *
   * @param text
   * @return
   */
  public String[] extractWordTokens(String text) {

    text = text.toLowerCase();
    String[] tokens = tokenizer.tokenize(text);

    // filter out tokens with length 1, tokens that start with non-word characters
    // or numbers
    Pattern filter = Pattern.compile(".{1}+|\\W.*|\\d.*");
    return filterTokens(Arrays.asList(tokens), filter);
  }

  /**
   * Extract tokens that are words of type nouns, adjectives or forgein words and
   * have length > 1 from a text. NOTE: This method uses pos-tagger and is
   * supposed to be slower than the extractWordTokens()
   *
   * @param text
   * @return
   */
  public String[] extractRelevantWordTokens(String text) {

    text = text.toLowerCase();
    List<String> extracted = new LinkedList<>();
    String[] tokens = tokenizer.tokenize(text);
    String[] tags = posTagger.tag(tokens);

    // extract nouns, adjectives and foreign words
    for (int i = 0; i < tags.length; i++) {
      if (tags[i].startsWith("N") || tags[i].startsWith("J") || tags[i].equals("FW")) {
        extracted.add(tokens[i]);
      }
    }

    // filter out tokens with length 1, tokens that start with non-word characters
    // or numbers
    Pattern filter = Pattern.compile(".{1}+|\\W.*|\\d.*");
    return filterTokens(extracted, filter);
  }

  private String[] filterTokens(Iterable<String> tokens, Pattern pattern) {

    List<String> extracted = new LinkedList<>();
    for (String token : tokens) {
      if (!pattern.matcher(token).matches()) {
        extracted.add(token);
      }
    }

    return extracted.toArray(new String[extracted.size()]);
  }
}
