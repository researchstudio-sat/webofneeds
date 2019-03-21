package won.matcher.utils.tensor;

import won.matcher.utils.preprocessing.OpenNlpTokenExtraction;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Used for tokenization of {@link TensorEntry} objects.
 * <p>
 * Created by hfriedrich on 21.04.2017.
 */
public class TensorEntryTokenizer implements TensorEntryGenerator {

  private OpenNlpTokenExtraction tokenizer;
  private Collection<TensorEntry> tensorEntries;

  public TensorEntryTokenizer(Collection<TensorEntry> tensorEntries) throws IOException {

    this.tensorEntries = tensorEntries;
    tokenizer = new OpenNlpTokenExtraction();
  }

  @Override public Collection<TensorEntry> generateTensorEntries() throws IOException {

    Collection<TensorEntry> tokenEntries = new LinkedList<>();
    for (TensorEntry entry : tensorEntries) {
      String tokens[] = tokenizer.extractWordTokens(entry.getValue());
      for (String token : tokens) {
        TensorEntry newEntry = new TensorEntry(entry.getSliceName(), entry.getNeedUri(), token);
        tokenEntries.add(newEntry);
      }
    }

    return tokenEntries;
  }
}
