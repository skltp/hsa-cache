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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SpringTest {

	@BeforeEach
	void before() {
		System.setProperty("hsaFile1", Objects.requireNonNull(getClass().getClassLoader().getResource("simpleTestPart1.xml")).getFile());
		System.setProperty("hsaFile2", Objects.requireNonNull(getClass().getClassLoader().getResource("simpleTestPart2.xml")).getFile());
	}
	
	@AfterEach
	void after() {
		System.clearProperty("hsaFile1");
		System.clearProperty("hsaFile2");
	}
	
	@Test
	void testSpringContext() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
		
		
		HsaCache cache = (HsaCache) ctx.getBean("cache");
		assertNotNull(cache);
		String p1 = cache.getParent("SE0000000000-1234");
		String p2 = cache.getParent("SE0000000001-1234");
		
		assertSame(p1,p2);
		
		String p3 = cache.getParent(p1);
		assertEquals("SE0000000003-1234", p3);
		
		String p4 = cache.getParent(p3);
		assertEquals("SE0000000004-1234", p4);
	}
}
