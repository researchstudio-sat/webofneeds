package won.bot.framework.extensions.serviceatom;

import java.util.Collection;
import java.util.Objects;

public class ServiceAtomContent {
    private String name;
    private String description;
    private String termsOfService;
    private Collection<String> tags;

    public ServiceAtomContent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void setTags(Collection<String> tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServiceAtomContent that = (ServiceAtomContent) o;
        return name.equals(that.name) &&
                        Objects.equals(description, that.description) &&
                        Objects.equals(termsOfService, that.termsOfService) &&
                        Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, termsOfService, tags);
    }
}
