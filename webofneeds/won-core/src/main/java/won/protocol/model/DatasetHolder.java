/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates a jena dataset for storing it in a relational db.
 */
@Entity
@Table(name = "rdf_datasets")
public class DatasetHolder {
    private static final int DEFAULT_BYTE_ARRAY_SIZE = 500;

    //the URI of the dataset
    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long id;

    @Column(name = "version", columnDefinition = "integer DEFAULT 0", nullable = false)
    private int version = 0;

    @Transient
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Column(name = "datasetURI", unique = true)
    @Convert(converter = URIConverter.class)
    private URI uri;

    //the model as a byte array
    @Lob
    @Column(name = "dataset", nullable = false, length = 10000000)
    private byte[] datasetBytes;

    //for multiple accesses to model, cache it.
    @Transient
    private Dataset cachedDataset;

    DatasetHolder() {
    }

    public DatasetHolder(final URI uri, final Dataset dataset) {
        this.uri = uri;
        setDataset(dataset);
        this.cachedDataset = dataset;
    }

    @PreUpdate
    @PrePersist
    public void incrementVersion() {
        this.version++;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    protected void setVersion(final int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public byte[] getDatasetBytes() {
        return datasetBytes;
    }

    void setDatasetBytes(final byte[] datasetBytes) {
        this.datasetBytes = datasetBytes;
        this.cachedDataset = null;
    }

    /**
     * Careful, expensive operation: writes dataset to string.
     *
     * @param dataset
     */
    public void setDataset(Dataset dataset) {
        assert this.uri != null : "uri must not be null";
        assert this.datasetBytes != null : "model must not be null";
        ByteArrayOutputStream out = new ByteArrayOutputStream(DEFAULT_BYTE_ARRAY_SIZE);
        synchronized (this) {
            RDFDataMgr.write(out, dataset, Lang.NQUADS);
            this.datasetBytes = out.toByteArray();
            this.cachedDataset = dataset;
            if (logger.isDebugEnabled()) {
                logger.debug("wrote dataset {} to byte array of length {}", this.uri, this.datasetBytes.length);
            }
        }
    }

    /**
     * Careful, expensive operation: reads dataset from string.
     *
     * @return
     */
    public Dataset getDataset() {
        assert this.uri != null : "uri must not be null";
        assert this.datasetBytes != null : "model must not be null";
        if (this.cachedDataset != null) return cachedDataset;
        synchronized (this) {
            if (this.cachedDataset != null) return cachedDataset;
            Dataset dataset = DatasetFactory.createGeneral();
            InputStream is = new ByteArrayInputStream(this.datasetBytes);
            try {
                try {
                    RDFDataMgr.read(dataset, is, this.uri.toString(), Lang.NQUADS);
                } catch (RiotException ex) {
                    //assume that the data is stored in TRIG old format, try that.
                    is = new ByteArrayInputStream(this.datasetBytes);
                    RDFDataMgr.read(dataset, is, Lang.TRIG);
                }
            } catch (Exception e) {
                logger.warn("could not read dataset {} from byte array. Byte array is null: {}, has length {}",
                        new Object[]{this.uri,
                                this.datasetBytes == null,
                                this.datasetBytes == null ? -1 : this.datasetBytes.length}
                );
                logger.warn("caught exception while reading dataset", e);
            }
            this.cachedDataset = dataset;
            return dataset;
        }
    }
}
