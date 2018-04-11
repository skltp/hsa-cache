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

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;


public class HsaFileVerifierImplTest {


    @Test
    public void testOkValidationAgainstSchema() throws Exception {
        URL url = getClass().getClassLoader().getResource("simpleTestPart1.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();

        url = getClass().getClassLoader().getResource("simpleTestPart1.xml");
        hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();

        url = getClass().getClassLoader().getResource("simpleTestPart2.xml");
        hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();

    }

    @Test(expected = HsaCacheInitializationException.class)
    public void testValidationOfMalformedFile() throws Exception {
        URL url = getClass().getClassLoader().getResource("hsaCacheMalformed.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();
    }

    @Test(expected = SAXException.class)
    public void testValidationOfEmptyFile() throws Exception {
        URL url = getClass().getClassLoader().getResource("hsaCacheNoUnits.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();
    }

    @Test(expected = SAXException.class)
    public void testValidationOfFileWithMissingDN() throws Exception {
        URL url = getClass().getClassLoader().getResource("hsaCacheMissingDN.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        hsaFileVerifier.validateFileAgainstXSD();
    }

    @Test
    public void testHsaCacheDiffZeroDiffs() throws Exception {
        URL url = getClass().getClassLoader().getResource("simpleTest.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        int diffSize = hsaFileVerifier.hsaCacheDiff(url.getFile());
        assertEquals("Should not be a diff",0, diffSize);
    }

    @Test
    public void testHsaCacheDiff3Diffs() throws Exception {
        URL url = getClass().getClassLoader().getResource("simpleTest.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        URL url2 = getClass().getClassLoader().getResource("simpleTestPart2.xml");
        int diffSize = hsaFileVerifier.hsaCacheDiff(url2.getFile());
        assertEquals(3, diffSize);
    }

    @Test
    public void testHsaCacheDiff5Diffs() throws Exception {
        URL url = getClass().getClassLoader().getResource("simpleTestPart1.xml");
        HsaFileVerifier hsaFileVerifier = new HsaFileVerifierImpl(url.getFile());
        URL url2 = getClass().getClassLoader().getResource("simpleTestPart2.xml");
        int diffSize = hsaFileVerifier.hsaCacheDiff(url2.getFile());
        assertEquals(5, diffSize);
    }

}