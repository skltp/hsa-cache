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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.junit.Test;

public class HsaFileParserTest {

	@Test
	public void testParseFile() throws Exception {
		HsaFileParser parser = new HsaFileParser();
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("simpleTest.xml");
		
		Map<Dn, HsaNode> nodes = parser.parse(is).getCache();
		
		assertEquals(6, nodes.size());
		
		HsaNode node1 = nodes.get(new Dn("o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"));
		HsaNode node2 = nodes.get(new Dn("ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"));
		HsaNode node3 = nodes.get(new Dn("ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"));
		HsaNode node4 = nodes.get(new Dn("ou=N\u00e4ssj\u00f6 VC DLM,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"));
		HsaNode node5 = nodes.get(new Dn("ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE"));
		HsaNode node6 = nodes.get(new Dn("o=Landstinget i Kalmar l\u00e4n,l=VpW,c=SE"));
	
		assertNotNull(node1);
		assertNotNull(node2);
		assertNotNull(node3);
		assertNotNull(node4);
		assertNotNull(node5);
		assertNotNull(node6);
		
		assertEquals("SE0000000004-1234", node1.getHsaId());
		assertEquals("SE0000000003-1234", node2.getHsaId());
		assertEquals("SE0000000002-1234", node3.getHsaId());
		assertEquals("SE0000000001-1234", node4.getHsaId());
		assertEquals("SE0000000000-1234", node5.getHsaId());
		assertEquals("SE0000000005-1234", node6.getHsaId());
		
		assertEquals("Landstinget i J\u00f6nk\u00f6ping", node1.getName());
		assertEquals("H\u00f6glandets sjukv\u00e5rdsomr\u00e5de", node2.getName());
		assertEquals("N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de", node3.getName());
		assertEquals("N\u00e4ssj\u00f6 VC DLM", node4.getName());
		assertEquals("N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de", node5.getName());
		assertEquals("UNIQUE_STRING_IN_FILE_TO_TEST_FREETEXT_SEARCH_ON_NAME", node6.getName());
	}

	@Test
	public void testParseDuplicateFile() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		HsaFileParser parser = new HsaFileParserWithLog(pw);	
		
		InputStream is = getClass().getClassLoader().getResourceAsStream("duplicateTest.xml");
		
		try {
			parser.parse(is);
			fail("Expected IllegalStateException");
		} catch(Exception ex) {
			assertTrue(ex.getMessage().startsWith("HsaObject entry invalid @ LineNo:34, Duplicate with: dn=ou="));
		}
	}
	
	@Test
	public void testInvalidEntry() throws Exception {		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		HsaFileParser parser = new HsaFileParserWithLog(pw);	
		InputStream is = getClass().getClassLoader().getResourceAsStream("invalidTest.xml");
		
		parser.parse(is);
		
		assertTrue(sw.toString().startsWith("ERROR HsaObject entry invalid @ LineNo:33, entry: dn=ou="));		
	}

	@Test
	public void testParseFileWithSpecialHtmlSigns() throws Exception {
		HsaFileParser parser = new HsaFileParser();
		InputStream is = getClass().getClassLoader().getResourceAsStream("simpleTestSpecialHtmlSigns.xml");

		Map<Dn, HsaNode> nodes = parser.parse(is).getCache();
		assertEquals(1, nodes.size());

		HsaNode node1 = nodes.get(new Dn("ou=Forskning & Utveckling,ou=Kardiologi,ou=Södersjukhuset AB,o=Stockholms Läns Landsting,l=Stockholms län,c=SE"));

		assertNotNull(node1);
		assertEquals("SE2321000016-3Q41", node1.getHsaId());
		assertEquals("Forskning & Utveckling", node1.getName());
	}

}
