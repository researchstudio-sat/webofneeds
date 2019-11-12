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
package won.protocol.exception;

import java.net.URI;

import org.apache.jena.rdf.model.Property;

/**
 * Exception indicating a illegal message property.
 */
public class IllegalMessagePropertyException extends WonMessageNotWellFormedException {
    /**
     * 
     */
    private static final long serialVersionUID = 1302098498497779601L;
    private URI illegalProperty;

    private static String createExceptionMessage(URI illegalProperty) {
        return String.format("Missing message property: %s", illegalProperty);
    }

    public IllegalMessagePropertyException(String illegalProperty) {
        this(URI.create(illegalProperty));
    }

    public IllegalMessagePropertyException(Property property) {
        this(property.toString());
    }

    public IllegalMessagePropertyException(URI illegalProperty) {
        super(createExceptionMessage(illegalProperty));
        this.illegalProperty = illegalProperty;
    }

    public IllegalMessagePropertyException(Throwable cause, URI illegalProperty) {
        super(cause);
        this.illegalProperty = illegalProperty;
    }

    public URI getMissingProperty() {
        return illegalProperty;
    }
}
