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

/**
 * User: fkleedorfer Date: 30.07.2014
 */
public class MatchPojo {
    private double score;
    private String originator;
    private String atomURI;
    private String connectionURI;
    private String targetAtomURI;
    private String socketURI;
    private String targetSocketURI;
    private String title;
    private String description;
    private String imageURI;
    private String[] tags;

    public double getScore() {
        return score;
    }

    public void setScore(final double score) {
        this.score = score;
    }

    public String getOriginator() {
        return originator;
    }

    public void setOriginator(final String originator) {
        this.originator = originator;
    }

    public String getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(final String atomURI) {
        this.atomURI = atomURI;
    }

    public String getConnectionURI() {
        return connectionURI;
    }

    public void setConnectionURI(final String connectionURI) {
        this.connectionURI = connectionURI;
    }

    public String getTargetAtomURI() {
        return targetAtomURI;
    }

    public void setTargetAtomURI(final String targetAtomURI) {
        this.targetAtomURI = targetAtomURI;
    }

    public String getSocketURI() {
        return socketURI;
    }

    public void setSocketURI(final String socketURI) {
        this.socketURI = socketURI;
    }

    public String getTargetSocketURI() {
        return targetSocketURI;
    }

    public void setTargetSocketURI(final String targetSocketURI) {
        this.targetSocketURI = targetSocketURI;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getImageURI() {
        return imageURI;
    }

    public void setImageURI(final String imageURI) {
        this.imageURI = imageURI;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(final String[] tags) {
        this.tags = tags;
    }
}
