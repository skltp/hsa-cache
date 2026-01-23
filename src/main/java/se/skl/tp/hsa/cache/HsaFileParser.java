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

import java.io.*;
import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Parser for XML file with HsaObjects
 * 
 * @author par.wenaker@callistaenterprise.se
 *
 */
public class HsaFileParser {
 
	private static final String ELEMENT_NAME_HSA_IDENTITY = "hsaIdentity";
	private static final String ELEMENT_NAME_DN = "DN";
	private static final String ELEMENT_NAME_NAME = "name";
	private static final String ELEMENT_NAME_HSA_OBJECT = "hsaUnit";
	private static final String ELEMENT_NAME_CREATION_DATE = "endDate";

	private static final Logger log = LoggerFactory.getLogger(HsaFileParser.class);
	
	/**
	 * Parses XML file
	 * 
	 * @param filename file name
	 * 
	 * @return Map from {@link Dn} to {@link HsaNode}
	 * 
	 * @throws XMLStreamException thrown on XML parse exception.
	 * @throws IOException thrown if file cannot be read
	 */
	public HSAData parse(String filename) throws XMLStreamException, IOException {
		return parse(new FileInputStream(filename));
	}
	
	/**
	 * Parse XML from inputstream
	 * 
	 * @param is inputstream
	 * 
	 * @return Map from {@link Dn} to {@link HsaNode}
	 * 
	 * @throws XMLStreamException thrown on XML parse exception.
	 * @throws IOException thrown if file cannot be read
	 */
	public HSAData parse(InputStream is) throws XMLStreamException, IOException {
		return doParseFile(new BufferedInputStream(is));
	}

	/**
	 * Does the parsing
	 * 
	 * @param in inputstream
	 * 
	 * @return Map from {@link Dn} to {@link HsaNode}
	 * 
	 * @throws XMLStreamException thrown on XML parse exception.
	 * @throws IOException thrown if file cannot be read
	 */
	protected HSAData doParseFile(InputStream in) throws XMLStreamException, IOException {
		HSAData hsaDataFromFile = new HSAData();

		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Prevent XXE attacks
			inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
			inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);

			XMLEventReader eventReader = inputFactory.createXMLEventReader(in, StandardCharsets.UTF_8.toString());
	
			State state = new State();

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				boolean tagFound = false;
				if (event.isStartElement()) {
					tagFound = processStartElement(event, hsaDataFromFile, eventReader, state);
				}
				if (!tagFound && event.isEndElement()) {
					processEndElement(event, state, hsaDataFromFile);
				}
			}
		} finally {
			if(in != null) {
				in.close();
			}
		}
		return hsaDataFromFile;
	}

	private boolean processStartElement(XMLEvent event, HSAData hsaDataFromFile, XMLEventReader eventReader, State state) throws XMLStreamException {
		StartElement startElement = event.asStartElement();
		String tagName = startElement.getName().getLocalPart();

		// When we hit a <endDate> tag
		if (ELEMENT_NAME_CREATION_DATE.equals(tagName) && hsaDataFromFile.getHsaFileCreationDate() == null) {
			hsaDataFromFile.setHsaFileCreationDate(getCharactersToEndTag(eventReader, ELEMENT_NAME_CREATION_DATE));
			return true;
		}
		if (ELEMENT_NAME_HSA_OBJECT.equals(tagName)) { // When we hit a <hsaUnit> tag
			state.startRow = startElement.getLocation().getLineNumber();
			state.entry = new HsaNode(state.startRow);
			return true;
		}
		if (ELEMENT_NAME_DN.equals(tagName)) { // When we hit a <DN> tag
			if (state.entry != null) {
				state.entry.setDn(getCharactersToEndTag(eventReader, ELEMENT_NAME_DN));
			}
			return true;
		}
		if (ELEMENT_NAME_HSA_IDENTITY.equals(tagName)) { // When we hit a <hsaIdentity> tag
			if (state.entry != null) {
				state.entry.setHsaId(getCharactersToEndTag(eventReader, ELEMENT_NAME_HSA_IDENTITY));
			}
			return true;
		}
		if (ELEMENT_NAME_NAME.equals(tagName)) { // When we hit a <name> tag
			if (state.entry != null) {
				state.entry.setName(getCharactersToEndTag(eventReader, ELEMENT_NAME_NAME));
			}
			return true;
		}
		return false;
	}

	private void processEndElement(XMLEvent event, State state, HSAData hsaDataFromFile) {
		EndElement endElement = event.asEndElement();
		if (ELEMENT_NAME_HSA_OBJECT.equals(endElement.getName().getLocalPart())) {
			if (state.entry != null && state.entry.isValid()) {
				HsaNode previous = hsaDataFromFile.getCache().put(state.entry.getDn(), state.entry);
				if (previous != null) {
					throw new IllegalStateException("HsaObject entry invalid @ LineNo:" + state.startRow + ", Duplicate with: " + previous);
				}
			} else {
				logError("HsaObject entry invalid @ LineNo:" + state.startRow + ", entry: " + state.entry);
			}
		}
	}

	private String getCharactersToEndTag(XMLEventReader eventReader, String endTagName) throws XMLStreamException {
		StringBuilder result = new StringBuilder();
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();

			if(event.isCharacters()){
				result.append(event.asCharacters().getData());
			}

			if(event.isEndElement()) {
				if (endTagName.equals(event.asEndElement().getName().getLocalPart())) {
					return result.toString();
				}else{
					throw new XMLStreamException("Wrong endTag found, expected " + endTagName+ " but found "+ event.asEndElement().getName().getLocalPart());
				}
			}
		}

		return result.toString();
	}

	/**
	 * Log errors
	 */
	protected void logError(String msg) {
		log.error(msg);
	}

	private static final class State {
		HsaNode entry = null;
		long startRow = 0;
	}
}
