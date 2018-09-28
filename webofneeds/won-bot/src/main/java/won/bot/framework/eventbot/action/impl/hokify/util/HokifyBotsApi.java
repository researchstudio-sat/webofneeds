package won.bot.framework.eventbot.action.impl.hokify.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import won.bot.framework.eventbot.action.impl.hokify.HokifyJob;

/**
 * 
 * @author MS
 * 
 *         Handles all needed webrequests
 *
 */
public class HokifyBotsApi {

    private String jsonURL;

    public HokifyBotsApi(String jsonURL) {
        this.jsonURL = jsonURL;
    }

    public ArrayList<HokifyJob> fetchHokifyData() {
        ArrayList<HokifyJob> jobsList = new ArrayList<HokifyJob>();
        System.out.println("Requeted URL:" + jsonURL);
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(jsonURL);
            getRequest.addHeader("accept", "application/json");

            HttpResponse response = httpClient.execute(getRequest);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

            StringBuilder sb = new StringBuilder();
            String line;
            System.out.println("Output from Server .... \n");
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            JSONArray jobArray = new JSONArray(json.getString("jobs"));
            ObjectMapper objectMapper = new ObjectMapper();

            for (int count = 0; count < jobArray.length(); count++) {
                try {

                    HokifyJob tmpJob = objectMapper.readValue(jobArray.getJSONObject(count).toString(),
                            HokifyJob.class);
                    jobsList.add(tmpJob);
                } catch (JsonParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (JsonMappingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            httpClient.close();
            response.getEntity().getContent().close();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("The jobsList contains " + jobsList.size() + " jobs");
        return jobsList;
    }

}
