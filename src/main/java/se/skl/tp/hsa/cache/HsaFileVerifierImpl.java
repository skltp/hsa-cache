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
