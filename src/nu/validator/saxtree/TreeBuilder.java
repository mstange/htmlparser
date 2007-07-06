/*
 * Copyright (c) 2007 Henri Sivonen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package nu.validator.saxtree;

import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class TreeBuilder implements ContentHandler, LexicalHandler {

    private Locator locator;

    private ParentNode current;

    private final boolean retainAttributes;

    private List<PrefixMapping> prefixMappings;
    
    public TreeBuilder() {
        this(false, false);
    }
    
    /**
     * 
     */
    public TreeBuilder(boolean fragment, boolean retainAttributes) {
        if (fragment) {
            current = new DocumentFragment();
        }
        this.retainAttributes = retainAttributes;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        current.appendChild(new Characters(locator, ch, start, length));
    }

    public void endDocument() throws SAXException {
        current.setEndLocator(locator);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        current.setEndLocator(locator);
        current = current.getParentNode();
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        current.appendChild(new IgnorableWhitespace(locator, ch, start, length));
    }

    public void processingInstruction(String target, String data) throws SAXException {
        current.appendChild(new ProcessingInstruction(locator, target, data));
    }

    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public void skippedEntity(String name) throws SAXException {
        current.appendChild(new SkippedEntity(locator, name));
    }

    public void startDocument() throws SAXException {
        current = new Document(locator);
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        current = (ParentNode) current.appendChild(new Element(locator, uri, localName, qName, atts, retainAttributes, prefixMappings));
        prefixMappings = null;
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (prefixMappings == null) {
            prefixMappings = new LinkedList<PrefixMapping>();
        }
        prefixMappings.add(new PrefixMapping(prefix, uri));
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        current.appendChild(new Comment(locator, ch, start, length));
    }

    public void endCDATA() throws SAXException {
        current.setEndLocator(locator);
        current = current.getParentNode();
    }

    public void endDTD() throws SAXException {
        current.setEndLocator(locator);
        current = current.getParentNode();
    }

    public void endEntity(String name) throws SAXException {
        current.setEndLocator(locator);
        current = current.getParentNode();
    }

    public void startCDATA() throws SAXException {
        current = (ParentNode) current.appendChild(new CDATA(locator));        
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        current = (ParentNode) current.appendChild(new DTD(locator, name, publicId, systemId));        
    }

    public void startEntity(String name) throws SAXException {
        current = (ParentNode) current.appendChild(new Entity(locator, name));        
    }

    /**
     * Returns the root.
     * 
     * @return the current
     */
    public ParentNode getRoot() {
        return current;
    }
}