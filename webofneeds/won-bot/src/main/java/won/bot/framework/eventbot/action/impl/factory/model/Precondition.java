package won.bot.framework.eventbot.action.impl.factory.model;

import java.io.Serializable;

public class Precondition implements Serializable {
    private String uri;
    private boolean met;

    public Precondition(String uri, boolean met) {
        this.uri = uri;
        this.met = met;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isMet() {
        return met;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Precondition precondition = (Precondition) o;
        return uri.equals(precondition.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public String toString() {
        return "Precondition{" + "uri='" + uri + '\'' + ", met=" + met + '}';
    }
}
