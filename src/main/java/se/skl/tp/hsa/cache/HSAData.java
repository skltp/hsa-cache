package se.skl.tp.hsa.cache;

import java.util.HashMap;
import java.util.Map;

public class HSAData {
    public HSAData() {
        cache = new HashMap<Dn, HsaNode>();
    }

    /**
     * Map holding the cache
     */
    private Map<Dn, HsaNode> cache;

    /**
     * Date of creation from hsa xml file
     */
    private String hsaFileCreationDate;


    public Map<Dn, HsaNode> getCache() {
        return cache;
    }

    public void setCache(Map<Dn, HsaNode> cache) {
        this.cache = cache;
    }

    public String getHsaFileCreationDate() {
        return hsaFileCreationDate;
    }

    public void setHsaFileCreationDate(String hsaFileCreationDate) {
        this.hsaFileCreationDate = hsaFileCreationDate;
    }
}
