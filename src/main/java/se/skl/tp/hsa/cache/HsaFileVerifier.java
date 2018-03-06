package se.skl.tp.hsa.cache;


import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Date;

public interface HsaFileVerifier {

    Date getCreationDate();

    int hsaCacheDiff(String oldCacheFile);

    void validateFileAgainstXSD() throws IOException, SAXException;
}
