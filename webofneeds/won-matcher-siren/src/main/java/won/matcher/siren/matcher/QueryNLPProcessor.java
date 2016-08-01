package won.matcher.siren.matcher;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.synonym.SynonymFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilesystemResourceLoader;
import org.apache.lucene.util.Version;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by soheilk on 01.09.2015.
 */
@Component
public class QueryNLPProcessor {
    Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
    POSTaggerME posTagger = null;
    SynonymFilterFactory synonymFilterfactory = null;

    public QueryNLPProcessor() throws IOException {

        InputStream modelIn = getClass().getClassLoader().getResourceAsStream("en-pos-maxent.bin");
        POSModel model = new POSModel(modelIn);
        posTagger = new POSTaggerME(model);

        StringReader strReader = new StringReader("baby toy clothes");
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
        TokenStream ts0 = analyzer.tokenStream("name", strReader);

        Map<String, String> filterArgs = new HashMap<String, String>();
        filterArgs.put("luceneMatchVersion", Version.LUCENE_4_9.toString());
        filterArgs.put("synonyms", "synonyms.txt");
        filterArgs.put("expand", "false");
        synonymFilterfactory = new SynonymFilterFactory(filterArgs);
        synonymFilterfactory.inform(new FilesystemResourceLoader());
    }

    public String[] retrieveSynonyms(String text) throws IOException {

        // Synonyms are only added here before constructing the query because we didnt get
        // solr synonyms activated (on indexing level).
        // If we would activate query synonyms in solar then we do not have to send the synonyms to the solr server.
        // However, if we are already using query synonyms we have here the possibility to boost them differently
        // than the original terms.

        List<String> synonymList = new ArrayList<String>();
        StringReader strReader = new StringReader(text);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_4_9);
        TokenStream ts0 = analyzer.tokenStream("name", strReader);
        TokenStream ts = synonymFilterfactory.create(ts0);

        CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken()) {
            String term = termAttribute.toString();
            synonymList.add(term);
        }
        analyzer.close();

        String[] synonyms = new String[synonymList.size()];
        synonymList.toArray(synonyms);
        return synonyms;
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

        // filter out tokens with length 1, tokens that start with non-word characters or numbers
        Pattern filter = Pattern.compile(".{1}+|\\W.*|\\d.*");
        return filterTokens(Arrays.asList(tokens), filter);
    }

    /**
     * Extract tokens that are words of type nouns, adjectives or forgein words and have length > 1 from a text.
     * NOTE: This method uses pos-tagger and is supposed to be slower than the extractWordTokens()
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

        // filter out tokens with length 1, tokens that start with non-word characters or numbers
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
