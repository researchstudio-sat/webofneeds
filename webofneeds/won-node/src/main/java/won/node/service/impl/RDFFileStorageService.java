/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import org.springframework.beans.factory.annotation.Value;
import won.protocol.exception.RDFStorageException;
import won.protocol.model.Need;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 15.02.13
 * Time: 11:24
 * To change this template use File | Settings | File Templates.
 */
public class RDFFileStorageService implements RDFStorageService {

    @Value ("${rdf.file.path}")
    private String path;

    @Override
    public void storeContent(Need need, Model graph) {
        FileOutputStream out = null;

        if(path.equals(""))
            path = System.getProperty("java.io.tmpdir");

        try {
            out = new FileOutputStream(new File(path, getFileName(need)));
            graph.write(out, "TTL");
        } catch (SecurityException se) {
            throw new RDFStorageException("Check the value of rdf.file.path in the needserver properties file!", se);
        } catch (FileNotFoundException e) {
            throw new RDFStorageException("Could not create File!", e);
        } finally {
            if(out != null)
                try {
                    out.close();
                } catch (IOException e) {
                    throw new RDFStorageException("Could not close File!", e);
                }
        }
    }

    @Override
    public Model loadContent(Need need) {
        InputStream in = null;
        Model m;

        if(path.equals(""))
            path = System.getProperty("java.io.tmpdir");

        try {
            m = ModelFactory.createDefaultModel();
            // use the FileManager to find the input file
            in = FileManager.get().open(path + "/" + getFileName(need));
            if (in == null) {
                throw new IllegalArgumentException("File: offer.ttl not found");
            }
            m.read(in, null, "TTL");
        } finally {
            if(in != null)
                try {
                    in.close();
                } catch (IOException e) {
                    throw new RDFStorageException("Could not close File!", e);
                }
        }
        return m;
    }

    private String getFileName(Need n) {
        return n.getId() + ".ttl";
    }
}
