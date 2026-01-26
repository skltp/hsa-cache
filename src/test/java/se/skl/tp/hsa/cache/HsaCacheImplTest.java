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
import org.springframework.lang.NonNull;

import static org.junit.jupiter.api.Assertions.*;
import static se.skl.tp.hsa.cache.HsaCache.*;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class HsaCacheImplTest {

	@Test
	void testSimple() throws Exception {
		URL url = getUrl("simpleTest.xml");

		assertNotNull(url);
        HsaCache impl = new HsaCacheImpl().init(url.getFile());
				
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000000-1234"));
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));
		assertEquals("SE0000000003-1234", impl.getParent("SE0000000002-1234"));
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000003-1234"));
		assertEquals(DEFAULT_ROOTNODE, impl.getParent("SE0000000004-1234"));
	
		assertEquals(Set.of("SE0000000003-1234"), new HashSet<>(impl.getChildren("SE0000000004-1234")));
		assertEquals(Set.of("SE0000000002-1234"), new HashSet<>(impl.getChildren("SE0000000003-1234")));
		assertEquals(Set.of("SE0000000000-1234","SE0000000001-1234"), new HashSet<>(impl.getChildren("SE0000000002-1234")));
	}
	
	@Test
	void testSimpleISO88591() throws Exception {
		URL url = getUrl("simpleTest-ISO-8859-1.xml");

		HsaCache impl = new HsaCacheImpl().init(url.getFile());
				
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000000-1234"));
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));
		assertEquals("SE0000000003-1234", impl.getParent("SE0000000002-1234"));
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000003-1234"));
		assertEquals(DEFAULT_ROOTNODE, impl.getParent("SE0000000004-1234"));
	
		assertEquals(List.of("SE0000000003-1234"), impl.getChildren("SE0000000004-1234"));
		assertEquals(List.of("SE0000000002-1234"), impl.getChildren("SE0000000003-1234"));
		List<String> children = impl.getChildren("SE0000000002-1234");
        assertEquals(2, children.size());
		assertTrue(children.contains("SE0000000000-1234"));
		assertTrue(children.contains("SE0000000001-1234"));
	}

	@NonNull
    private URL getUrl(String name) {
		URL url = getClass().getClassLoader().getResource(name);
		assertNotNull(url);
		return url;
	}

	@Test
	void testReinitialize() {
		URL url = getUrl("simpleTestShort.xml");
		HsaCache impl = new HsaCacheImpl(url.getFile());
		
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000001-1234"));
		
		url = getUrl("simpleTest.xml");
		impl.init(url.getFile());
		
		assertEquals("SE0000000002-1234", impl.getParent("SE0000000001-1234"));	
	}
	
	@Test
	void testReinitializeFail() {
		URL url = getUrl("simpleTestShort.xml");
		HsaCache impl = new HsaCacheImpl(url.getFile());
		
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000001-1234"));
		
		try {
			impl.init("dummyfile.txt");
			fail("Expected exception");
		} catch(Exception e) {
			// Expected
		}
		assertEquals("SE0000000004-1234", impl.getParent("SE0000000001-1234"));
	}
	
	@Test
	void testInvalid() {
		assertThrows(HsaCacheInitializationException.class, () -> new HsaCacheImpl("notfound.xml"));
	}
	
	@Test
	void testNotInitializedGivesDefaultRoot() {
		HsaCacheImpl impl = new HsaCacheImpl();
		assertEquals(DEFAULT_ROOTNODE,impl.getParent("jabbadabba"));
	}
	
	@Test
	void defaultRootNodeReturnedWhenHsaIdNotFoundInCache() {
		HsaCacheImpl impl = new HsaCacheImpl();
		URL url = getUrl("simpleTest.xml");
		impl.init(url.getFile());

		assertEquals(DEFAULT_ROOTNODE, impl.getParent("jabbadabba"));
	}
}
