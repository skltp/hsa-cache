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

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("UnnecessaryUnicodeEscape")
class HsaNodePrinterTest {
	
	List<String> expectedRows = List.of(
		"dn=o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000004-1234,name=Landstinget i J\u00f6nk\u00f6ping,lineNo=48",
		"  dn=ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000003-1234,name=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,lineNo=43",
		"    dn=ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000002-1234,name=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,lineNo=38"
	);
	Set<String> expectedUndefinedOrder = Set.of(
		"      dn=ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000000-1234,name=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,lineNo=33",
		"      dn=ou=N\u00e4ssj\u00f6 VC DLM,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000001-1234,name=N\u00e4ssj\u00f6 VC DLM,lineNo=28");

	@Test
	void testPrint() {
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");
        assertNotNull(url);
        HsaCacheImpl impl = (HsaCacheImpl)new HsaCacheImpl().init(url.getFile());
		
		HsaNode topNode = impl.getNode("SE0000000004-1234");
		
		StringWriter sw = new StringWriter();
		
		new HsaNodePrinter(topNode,2).printTree(new PrintWriter(sw));

		List<String> rows = List.of(sw.toString().split(System.lineSeparator()));
		int expectedRowCount = expectedRows.size() + expectedUndefinedOrder.size();
		assertEquals(expectedRowCount, rows.size());
		assertEquals(expectedRows, rows.subList(0, expectedRows.size()));
		assertEquals(expectedUndefinedOrder, new HashSet<>(rows.subList(expectedRows.size(), rows.size())));
	}
}
