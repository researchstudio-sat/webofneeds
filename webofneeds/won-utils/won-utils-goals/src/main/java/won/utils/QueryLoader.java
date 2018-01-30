package won.utils;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.IOException;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

public class QueryLoader {

	private String queryString;
	private static String queryFile;
	
	  public QueryLoader(String queryFile) {
		 QueryLoader.queryFile = queryFile;
		 InputStream is  = QueryLoader.class.getResourceAsStream(queryFile);
		 StringWriter writer = new StringWriter();
		 try {
	            IOUtils.copy(is, writer, Charsets.UTF_8);
	        } catch (IOException e) { 
	        	throw new IllegalStateException("Could not read queryString file", e);	        
	        }
		 
		 this.queryString = writer.toString();		 
	 }	
	 
	 public String getQueryAsString() {
		 return queryString;
	 }
}
