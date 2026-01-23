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

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.springframework.lang.NonNull;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;

class HsaFileVerifierImplTest {


    @Test
    void testOkValidationAgainstSchema() throws Exception {
        URL url = getUrl("simpleTestPart1.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();

        url = getUrl("simpleTestPart2.xml");
        hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();

    }

    @Test
    void testValidationOfMalformedFile() {
        URL url = getUrl("hsaCacheMalformed.xml");
        String filename = url.getFile();
        assertThrows(HsaCacheInitializationException.class, () -> new HsaFileVerifierImpl(filename));
    }

    @Test
    void testValidationOfEmptyFile() {
        URL url = getUrl("hsaCacheNoUnits.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        assertThrows(SAXException.class, hsaFileVerifier::validateFileAgainstXSD);
    }

    @Test
    void testValidationOfFileWithMissingDN() {
        URL url = getUrl("hsaCacheMissingDN.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        assertThrows(SAXException.class, hsaFileVerifier::validateFileAgainstXSD);
    }

    @Test
    void testHsaCacheDiffZeroDiffs() {
        URL url = getUrl("simpleTest.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        int diffSize = hsaFileVerifier.hsaCacheDiff(url.getFile());
        assertEquals(0, diffSize);
    }

    @Test
    void testHsaCacheDiff3Diffs() {
        URL url = getUrl("simpleTest.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        URL url2 = getUrl("simpleTestPart2.xml");
        int diffSize = hsaFileVerifier.hsaCacheDiff(url2.getFile());
        assertEquals(3, diffSize);
    }

    @Test
    void testHsaCacheDiff5Diffs() {
        URL url = getUrl("simpleTestPart1.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        URL url2 = getUrl("simpleTestPart2.xml");
        int diffSize = hsaFileVerifier.hsaCacheDiff(url2.getFile());
        assertEquals(5, diffSize);
    }

    @NonNull
    private URL getUrl(String name) {
        URL url = getClass().getClassLoader().getResource(name);
        assertNotNull(url);
        return url;
    }
}