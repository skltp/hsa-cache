/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
 package se.skl.tp.hsa.cache;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class HsaFileVerifierImpl implements HsaFileVerifier {
    private static final String HSA_CACHE_XSD = "HsaCache.xsd";
    private String file;
    private HsaCache newCache;

    public HsaFileVerifierImpl(String newHSACacheFile) {
        file = newHSACacheFile;
        newCache = new HsaCacheImpl(newHSACacheFile);
    }

    @Override
    public int hsaCacheDiff(String oldCacheFile) {
        HsaCache oldCache = new HsaCacheImpl(oldCacheFile);
        return newCache.calculateHSACacheDiff(oldCache);
    }

    @Override
    public Date getCreationDate() {
        return newCache.getHsaFileCreationDate();
    }

    @Override
    public void validateFileAgainstXSD() throws IOException, SAXException {
        Source xmlFile = new StreamSource(new File(file));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = schemaFactory.newSchema(getSchema());
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }

    private Source getSchema() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(HSA_CACHE_XSD);
        return new StreamSource(inputStream);
    }


}
