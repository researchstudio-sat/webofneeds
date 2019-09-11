package won.bot.framework.eventbot.action.impl.hokify;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author MS on 17.09.2018
 */
public class HokifyJob {
    private static final String TITLE = "title";
    private static final String DATE = "date";
    private static final String REFERENCENUMBER = "referencenumber";
    private static final String IMAGE = "image";
    private static final String URL = "url";
    private static final String COMPANY = "company";
    private static final String CITY = "city";
    private static final String COUNTRY = "country";
    private static final String DESCRIPTION = "description";
    private static final String SALARY = "salary";
    private static final String JOBTYPE = "jobtype";
    private static final String FIELD = "field";
    private static final String STATS = "stats";
    private static final String PRIORIZE = "priorize";
    @JsonProperty(TITLE)
    private String title;
    // TODO Change to right date format
    @JsonProperty(DATE)
    private String date;
    @JsonProperty(REFERENCENUMBER)
    private String referencenumber;
    @JsonProperty(IMAGE)
    private String image;
    @JsonProperty(URL)
    private String url;
    @JsonProperty(COMPANY)
    private String company;
    @JsonProperty(CITY)
    private String city;
    @JsonProperty(COUNTRY)
    private String country;
    @JsonProperty(DESCRIPTION)
    private String description;
    @JsonProperty(SALARY)
    private String salary;
    @JsonProperty(JOBTYPE)
    private String jobtype;
    @JsonProperty(FIELD)
    private List<?> field;
    @JsonProperty(STATS)
    private Stats stats;
    @JsonProperty(PRIORIZE)
    private String priorize;

    public HokifyJob() {
    }

    @Override
    public String toString() {
        if (this.stats != null) {
            return "HokifyJob [title=" + title + ", date=" + date + ", referencenumber=" + referencenumber + ", image="
                            + image + ", url=" + url + ", company=" + company + ", city=" + city + ", country="
                            + country
                            + ", description=" + description + ", salary=" + salary + ", jobtype=" + jobtype
                            + ", field="
                            + field + ", stats=" + stats.toString() + ", priorize=" + priorize + "]";
        } else {
            return "HokifyJob [title=" + title + ", date=" + date + ", referencenumber=" + referencenumber + ", image="
                            + image + ", url=" + url + ", company=" + company + ", city=" + city + ", country="
                            + country
                            + ", description=" + description + ", salary=" + salary + ", jobtype=" + jobtype
                            + ", field="
                            + field + ", stats=" + 0 + ", priorize=" + priorize + "]";
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReferencenumber() {
        return referencenumber;
    }

    public void setReferencenumber(String referencenumber) {
        this.referencenumber = referencenumber;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getJobtype() {
        return jobtype;
    }

    public void setJobtype(String jobtype) {
        this.jobtype = jobtype;
    }

    public List<?> getField() {
        return field;
    }

    public void setField(List<?> field) {
        this.field = field;
    }

    public Stats getStats() {
        return stats;
    }

    public void Stats(Stats stats) {
        this.stats = stats;
    }

    public String getPriorize() {
        return priorize;
    }

    public void setPriorize(String priorize) {
        this.priorize = priorize;
    }

    static class Stats {
        private static final String VIEWS = "views";
        private static final String DISCARDED = "discarded";
        private static final String SAVED = "saved";
        private static final String APPLIED = "applied";
        private static final String APPLIEDLAST30DAYS = "appliedLast30Days";
        private static final String SMSLINK = "smslink";
        @JsonProperty(VIEWS)
        private Views views;
        @JsonProperty(DISCARDED)
        private int discarded;
        @JsonProperty(SAVED)
        private int saved;
        @JsonProperty(APPLIED)
        private int applied;
        @JsonProperty(APPLIEDLAST30DAYS)
        private int appliedLast30Days;
        @JsonProperty(SMSLINK)
        private int smslink;

        public Stats() {
        }

        @Override
        public String toString() {
            return "Stats [views=" + views.toString() + ", discarded=" + discarded + ", saved=" + saved + ", applied="
                            + applied + ", appliedLast30Days=" + appliedLast30Days + ", smslink=" + smslink + "]";
        }

        public Views getViews() {
            return views;
        }

        public void setViews(Views views) {
            this.views = views;
        }

        public int getDiscarded() {
            return discarded;
        }

        public void setDiscarded(int discarded) {
            this.discarded = discarded;
        }

        public int getSaved() {
            return saved;
        }

        public void setSaved(int saved) {
            this.saved = saved;
        }

        public int getApplied() {
            return applied;
        }

        public void setApplied(int applied) {
            this.applied = applied;
        }

        public int getAppliedLast30Days() {
            return appliedLast30Days;
        }

        public void setAppliedLast30Days(int appliedLast30Days) {
            this.appliedLast30Days = appliedLast30Days;
        }

        public int getSmslink() {
            return smslink;
        }

        public void setSmslink(int smslink) {
            this.smslink = smslink;
        }

        static class Views {
            private static final String WEB = "web";
            private static final String APP = "app";
            private static final String EXTERNAL = "external";
            @JsonProperty(WEB)
            private int web;
            @JsonProperty(APP)
            private int app;
            @JsonProperty(EXTERNAL)
            private int external;

            public Views() {
            }

            @Override
            public String toString() {
                return "Views [web=" + web + ", app=" + app + ", external=" + external + "]";
            }

            public int getWeb() {
                return web;
            }

            public void setWeb(int web) {
                this.web = web;
            }

            public int getApp() {
                return app;
            }

            public void setApp(int app) {
                this.app = app;
            }

            public int getExternal() {
                return external;
            }

            public void setExternal(int external) {
                this.external = external;
            }
        }
    }
}
