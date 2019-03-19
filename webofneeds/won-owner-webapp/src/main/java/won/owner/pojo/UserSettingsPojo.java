package won.owner.pojo;

/**
 * User: ypanchenko Date: 23.02.2015
 */
public class UserSettingsPojo extends UsernamePojo {
    private String email;
    private String needUri;
    private boolean notifyMatches;
    private boolean notifyRequests;
    private boolean notifyConversations;

    public UserSettingsPojo() {
    }

    public UserSettingsPojo(String username, String email) {
        this.setUsername(username);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public boolean isNotifyMatches() {
        return notifyMatches;
    }

    public void setNotifyMatches(final boolean notifyMatches) {
        this.notifyMatches = notifyMatches;
    }

    public boolean isNotifyRequests() {
        return notifyRequests;
    }

    public void setNotifyRequests(final boolean notifyRequests) {
        this.notifyRequests = notifyRequests;
    }

    public boolean isNotifyConversations() {
        return notifyConversations;
    }

    public void setNotifyConversations(final boolean notifyConversations) {
        this.notifyConversations = notifyConversations;
    }

    public String getNeedUri() {
        return needUri;
    }

    public void setNeedUri(final String needUri) {
        this.needUri = needUri;
    }

    public void setNotify(final boolean notifyMatches, final boolean notifyRequests,
            final boolean notifyConversations) {
        this.notifyMatches = notifyMatches;
        this.notifyRequests = notifyRequests;
        this.notifyConversations = notifyConversations;
    }
}
