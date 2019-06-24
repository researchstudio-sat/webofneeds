/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.owner.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.jena.query.Dataset;
import won.protocol.model.AtomState;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultAtomModelWrapper;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collection;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AtomPojo {
    private URI uri;
    private AtomState state;
    private Collection<URI> types;
    private Collection<URI> seeksTypes;
    private Coordinate location;
    private Coordinate jobLocation;
    @JsonIgnore
    private ZonedDateTime creationZonedDateTime;
    private String creationDate;
    @JsonIgnore
    private ZonedDateTime modifiedZonedDateTime;
    private String modifiedDate;
    private Collection<URI> flags;
    private Collection<URI> holds;
    private URI heldBy;
    private Collection<URI> eventObjectAboutUris;
    private Collection<URI> seeksEventObjectAboutUris;

    // TODO: add s:object/s:Event/s:about URI for content and seeks -> otherwise we
    // do not know which interest it is
    public AtomPojo() {
    }

    public AtomPojo(Dataset atom) {
        this(new DefaultAtomModelWrapper(atom));
    }

    public AtomPojo(DefaultAtomModelWrapper atom) {
        uri = URI.create(atom.getAtomUri());
        modifiedZonedDateTime = atom.getModifiedDate();
        modifiedDate = modifiedZonedDateTime.toString();
        creationZonedDateTime = atom.getCreationDate();
        creationDate = creationZonedDateTime.toString();
        location = atom.getLocationCoordinate();
        jobLocation = atom.getJobLocationCoordinate();
        flags = atom.getAllFlags();
        types = atom.getContentTypes();
        seeksTypes = atom.getSeeksTypes();
        state = atom.getAtomState();
        heldBy = atom.getHeldBy();
        holds = atom.getHolds(); // TODO: IMPL ME
        seeksEventObjectAboutUris = atom.getSeeksEventObjectAboutUris();
        eventObjectAboutUris = atom.getContentEventObjectAboutUris();
    }

    /*
     * public DraftPojo(URI uri, Model content, Draft draftState ){ super(uri,
     * content); this.setCurrentStep(draftState.getCurrentStep());
     * this.setUserName(draftState.getUserName()); }
     */
    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public Collection<URI> getTypes() {
        return types;
    }

    public void setTypes(Collection<URI> types) {
        this.types = types;
    }

    public Collection<URI> getSeeksTypes() {
        return seeksTypes;
    }

    public void setSeeksTypes(Collection<URI> seeksTypes) {
        this.seeksTypes = seeksTypes;
    }

    public Coordinate getLocation() {
        return location;
    }

    public void setLocation(Coordinate location) {
        this.location = location;
    }

    public Coordinate getJobLocation() {
        return jobLocation;
    }

    public void setJobLocation(Coordinate jobLocation) {
        this.jobLocation = jobLocation;
    }

    public ZonedDateTime getCreationZonedDateTime() {
        return creationZonedDateTime;
    }

    public ZonedDateTime getModifiedZonedDateTime() {
        return modifiedZonedDateTime;
    }

    public Collection<URI> getFlags() {
        return flags;
    }

    public void setFlags(Collection<URI> flags) {
        this.flags = flags;
    }

    public AtomState getState() {
        return state;
    }

    public void setState(AtomState state) {
        this.state = state;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(String modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public void setCreationZonedDateTime(ZonedDateTime creationZonedDateTime) {
        this.creationZonedDateTime = creationZonedDateTime;
    }

    public void setModifiedZonedDateTime(ZonedDateTime modifiedZonedDateTime) {
        this.modifiedZonedDateTime = modifiedZonedDateTime;
    }

    public Collection<URI> getHolds() {
        return holds;
    }

    public void setHolds(Collection<URI> holds) {
        this.holds = holds;
    }

    public URI getHeldBy() {
        return heldBy;
    }

    public void setHeldBy(URI heldBy) {
        this.heldBy = heldBy;
    }

    public Collection<URI> getEventObjectAboutUris() {
        return eventObjectAboutUris;
    }

    public void setEventObjectAboutUris(Collection<URI> eventObjectAboutUris) {
        this.eventObjectAboutUris = eventObjectAboutUris;
    }

    public Collection<URI> getSeeksEventObjectAboutUris() {
        return seeksEventObjectAboutUris;
    }

    public void setSeeksEventObjectAboutUris(Collection<URI> seeksEventObjectAboutUris) {
        this.seeksEventObjectAboutUris = seeksEventObjectAboutUris;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AtomPojo atomPojo = (AtomPojo) o;
        if (!uri.equals(atomPojo.uri))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
