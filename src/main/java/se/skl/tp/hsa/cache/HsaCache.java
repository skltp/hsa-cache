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

import java.util.Date;
import java.util.List;


public interface HsaCache {
	
	/**
	 * Default root for the HSA cache is SE.
	 */
	public static final String DEFAUL_ROOTNODE = "SE";

	/**
	 * Date format
	 */
	public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	
	/**
	 * Initialize the Cache. If the cache has a value before a call to this method, that state is 
	 * retained in in case of an exception.
	 * 
	 * @param filenames file to initialize from
	 * 
	 * @return a populated HsaCache or an unchanged HSA Cache in case of an exception
	 * 
	 * @throws HsaCacheInitializationException if a fatal error occurred initializing the file
	 */
	HsaCache init(String ... filenames) throws HsaCacheInitializationException;

    /**
     * Free text search of the HSA tree. SEarch both HSA-ID and DN infomrmation.
     * If the search text contains several words all must match either HSA-ID and DN infomrmation.
     *
     * @param searchText
     * @param maxNoOfHits, -1 means all...
     * @return
     */
    public List<HsaNodeInfo> freeTextSearch(String searchText, int maxNoOfHits);

	public HsaNode getNode(String hsaId);

	/**
	 * Get the parent HSA-ID for a specific HSA-ID. If the HSA-ID is not found in 
	 * HSA cache the default root parent is returned, in this case the SE node. 
	 * 
	 * @param hsaId the HSA-ID
	 * @return parent HSA-ID
	 * 
	 */
	String getParent(String hsaId);
	
	/**
	 * Get the children HSA-ID for a specific HSA-ID
	 * 
	 * @param hsaId the HSA-ID
	 * 
	 * @return list of children HSA-ID. Can be empty.
	 * 
	 * @throws HsaCacheNodeNotFoundException if the hsaId is not found in the cache
	 * @throws HsaCacheInitializationException if the cache has not been initialized
	 */
	List<String> getChildren(String hsaId) throws HsaCacheNodeNotFoundException;

	/**
	 * Get the number of entries in the HSA cache
	 * 
	 * @return {@link int} Number of entries
	 */
	int getHSACacheSize();

	/**
	 *
	 * @return Date of creation(latest) from hsa xml files
	 */
	Date getHsaFileCreationDate();

	/**
	 * Returns number of unique elements in HSA caches (cache difference)
	 * @param cache for compare
	 * @return cache difference
	 */

	int calculateHSACacheDiff(HsaCache cache);
}
