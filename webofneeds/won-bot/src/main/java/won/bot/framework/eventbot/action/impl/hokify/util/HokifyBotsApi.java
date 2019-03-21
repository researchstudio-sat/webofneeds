package won.bot.framework.eventbot.action.impl.hokify.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author MS
 *         <p>
 *         Handles all needed webrequests
 */
public class HokifyBotsApi {

  private String jsonURL;
  private String geoURL;

  public HokifyBotsApi(String jsonURL, String geoURL) {
    this.jsonURL = jsonURL;
    this.geoURL = geoURL;
  }

  public ArrayList<HokifyJob> fetchHokifyData() {
    ArrayList<HokifyJob> jobsList = new ArrayList<HokifyJob>();
    CloseableHttpResponse response = null;
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      HttpGet getRequest = new HttpGet(jsonURL);
      getRequest.addHeader("accept", "application/json");

      response = httpClient.execute(getRequest);

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      JSONObject json = new JSONObject(sb.toString());
      JSONArray jobArray = new JSONArray(json.getString("jobs"));
      ObjectMapper objectMapper = new ObjectMapper();

      for (int count = 0; count < jobArray.length(); count++) {
        try {

          HokifyJob tmpJob = objectMapper.readValue(jobArray.getJSONObject(count).toString(), HokifyJob.class);
          jobsList.add(tmpJob);
        } catch (JsonParseException e) {
          e.printStackTrace();
        } catch (JsonMappingException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return jobsList;
  }

  public HashMap<String, String> fetchGeoLocation(String city, String country) {
    HashMap<String, String> loc = new HashMap<String, String>();

    String cityString = city != null ? city.replace(" ", "+") : "";
    String countrySting = country != null ? country.replace(" ", "+") : "";

    String searchString = geoURL + "?city=" + cityString + "&country=" + countrySting + "&format=json";

    HttpGet getRequest = new HttpGet(searchString);
    getRequest.addHeader("accept", "application/json");

    CloseableHttpResponse response = null;
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
      response = httpClient.execute(getRequest);

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
      }

      BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }

      JSONArray jsonArray = new JSONArray(sb.toString());

      if (jsonArray.length() > 0) {
        JSONObject obj = jsonArray.getJSONObject(0);

        JSONArray bBox = (JSONArray) obj.get("boundingbox");

        loc.put("nwlat", (String) bBox.get(1));
        loc.put("nwlng", (String) bBox.get(3));
        loc.put("selat", (String) bBox.get(0));
        loc.put("selng", (String) bBox.get(2));
        loc.put("lat", (String) obj.get("lat"));
        loc.put("lng", (String) obj.get("lon"));
        loc.put("name", (String) obj.get("display_name"));
      } else {
        loc.put("nwlat", "0");
        loc.put("nwlng", "0");
        loc.put("selat", "0");
        loc.put("selng", "0");
        loc.put("lat", "0");
        loc.put("lng", "0");
        loc.put("name", "no location");
      }
      httpClient.close();
      response.getEntity().getContent().close();

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    return loc;
  }

}
