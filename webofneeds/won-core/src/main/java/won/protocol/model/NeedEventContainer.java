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
package won.protocol.model;

import java.net.URI;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;

import won.protocol.model.parentaware.ParentAware;

@Entity
@DiscriminatorValue("Need")
public class NeedEventContainer extends EventContainer implements ParentAware<Need> {
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "eventContainer", optional = false)
    private Need need;

    public NeedEventContainer() {
    }

    public NeedEventContainer(final Need need, URI parentUri) {
        super(parentUri);
        this.need = need;
        if (need != null) {
            need.setEventContainer(this);
        }
    }

    public Need getNeed() {
        return need;
    }

    @Override
    public Need getParent() {
        return getNeed();
    }

    protected void setNeed(final Need need) {
        this.need = need;
    }
}
