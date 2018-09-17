package won.bot.framework.eventbot.action.impl.hokify;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author MS on 17.09.2018
 *
 */
public class HokifyJob {

    private String title;
    // TODO Change to right date format
    private String date;
    private String referencenumber;
    private String image;
    private String url;
    private String company;
    private String city;
    private String country;
    private String description;
    private String salary;
    private String jobtype;
    private JSONArray field;
    private Stats stats;

    public HokifyJob(JSONObject object) {
        this.createJobFromJson(object);
        System.out.println(this.toString());
    }

    public void createJobFromJson(JSONObject object) {
        try {
            this.title = object.getString("title");
            this.date = object.getString("date");
            this.referencenumber = object.getString("referencenumber");
            this.image = object.getString("image");
            this.url = object.getString("url");
            this.company = object.getString("company");
            this.city = object.getString("city");
            this.country = object.getString("country");
            this.description = object.getString("description");
            try {
                this.salary = object.getString("salary");
            } catch (org.json.JSONException e) {
                this.salary = "Na";
                System.out.println("no salary for " + this.title);
            }
            this.jobtype = object.getString("jobtype");
            this.field = object.getJSONArray("field");
            try {
                this.stats = new Stats(object.getJSONObject("stats"));
            } catch (org.json.JSONException e) {
                this.stats = null;
                System.out.println("no stats for " + this.title);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        if (this.stats != null) {
            return "HokifyJob [title=" + title + ", date=" + date + ", referencenumber=" + referencenumber + ", image="
                    + image + ", url=" + url + ", company=" + company + ", city=" + city + ", country=" + country
                    + ", description=" + description + ", salary=" + salary + ", jobtype=" + jobtype + ", field="
                    + field + ", stats=" + stats.toString() + "]";
        } else {
            return "HokifyJob [title=" + title + ", date=" + date + ", referencenumber=" + referencenumber + ", image="
                    + image + ", url=" + url + ", company=" + company + ", city=" + city + ", country=" + country
                    + ", description=" + description + ", salary=" + salary + ", jobtype=" + jobtype + ", field="
                    + field + ", stats=" + 0 + "]";
        }
    }

}

class Stats {
    private Views views;
    private int discarded;
    private int saved;
    private int applied;
    private int appliedLast20Days;
    private int smslink;

    public Stats(JSONObject object) throws JSONException {
        this.views = new Views(object.getJSONObject("views"));
        this.discarded = Integer.parseInt(object.getString("discarded"));
        this.saved = Integer.parseInt(object.getString("saved"));
        this.applied = Integer.parseInt(object.getString("applied"));
        this.appliedLast20Days = Integer.parseInt(object.getString("appliedLast20Days2"));
        this.smslink = Integer.parseInt(object.getString("smslink"));
    }

    @Override
    public String toString() {
        return "Stats [views=" + views.toString() + ", discarded=" + discarded + ", saved=" + saved + ", applied="
                + applied + ", appliedLast20Days=" + appliedLast20Days + ", smslink=" + smslink + "]";
    }

}

class Views {
    private int web;
    private int app;
    private int external;

    public Views(JSONObject object) throws NumberFormatException, JSONException {
        this.web = Integer.parseInt(object.getString("web"));
        this.app = Integer.parseInt(object.getString("app"));
        this.external = Integer.parseInt(object.getString("external"));

    }

    @Override
    public String toString() {
        return "Views [web=" + web + ", app=" + app + ", external=" + external + "]";
    }

}
