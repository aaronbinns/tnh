/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/*
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.*;
//import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.search.highlight.*;
*/

public class OpenSearchBase extends HttpServlet
{
  public static final long serialVersionUID = 0L;

  public static final String   NS_OPENSEARCH = "http://a9.com/-/spec/opensearchrss/1.0/";
  public static final String   NS_ARCHIVE   = "http://web.archive.org/-/spec/opensearchrss/1.0/";

  public static final String[] EMPTY_STRINGS = { };
  public static final String[] ALL_INDEXES   = { "" };

  public DocumentBuilderFactory factory;
  
  public void init( ServletConfig config )
    throws ServletException
  {
    this.factory = DocumentBuilderFactory.newInstance();
    this.factory.setNamespaceAware(true);
  }

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
  {
    /*
    try
      {
        long responseTime = System.nanoTime( );
        
        Parameters p = processParameters( request );
        
        Document doc = buildResponseDocument( p, request );

        // TODO: Add hits/results to document.  Call abstract method over-ridden by sub-classes

        // Add response time to end of document.
        responseTime = System.nanoTime( ) - responseTime;
        addNode(doc, channel, NS_ARCHIVE, "responseTime", Double.toString( (responseTime / 1000 / 1000) / 1000.0 ) );

        // Write out the XML document.
      }
    catch ( Exception e )
      {
        throw new ServletException( e );
      }
    */
  }

  /*
  public Parameters processParameters( HttpServletRequest request )
  {
    request.setCharacterEncoding("UTF-8");
    
    Parameters p = new Parameters( request );
    
    
  }
  */

  public Document buildResponseDocument( Parameters p, HttpServletRequest request )
    throws Exception
  {
    Document doc = factory.newDocumentBuilder().newDocument();
    
    Element rss = addNode( doc, doc, "rss" );
    addAttribute(doc, rss, "version", "2.0" );
    
    Element channel = addNode( doc, rss, "channel" );
    
    addNode( doc, channel, "title", p.query );
    addNode( doc, channel, "description", p.query);
    addNode( doc, channel, "link", "" );
    
    //    addNode( doc, channel, NS_OPENSEARCH, "totalResults", "" + p.totalResults );
    addNode( doc, channel, NS_OPENSEARCH, "startIndex",   "" + p.start        );
    addNode( doc, channel, NS_OPENSEARCH, "itemsPerPage", "" + p.hitsPerPage  );
    
    addNode( doc, channel, NS_ARCHIVE, "query", p.query );
    
    // Add a <urlParams> element containing a list of all the URL parameters.
    Element urlParams = doc.createElementNS( NS_ARCHIVE, "urlParams" );
    channel.appendChild( urlParams );
    
    for ( Map.Entry<String,String[]> e : ((Map<String,String[]>) request.getParameterMap( )).entrySet( ) )
      {
        String key = e.getKey( );
        for ( String value : e.getValue( ) )
          {
            Element urlParam = doc.createElementNS( NS_ARCHIVE, "param" );
            addAttribute( doc, urlParam, "name",  key   );
            addAttribute( doc, urlParam, "value", value.trim() );
            urlParams.appendChild(urlParam);
          }
      }

    return doc;
  }

  void writeResponseDocument( Document doc, HttpServletResponse response )
    throws Exception
  {
    DOMSource source = new DOMSource(doc);
    TransformerFactory transFactory = TransformerFactory.newInstance();
    Transformer transformer = transFactory.newTransformer();
    transformer.setOutputProperty( javax.xml.transform.OutputKeys.ENCODING, "UTF-8" );
    StreamResult sresult = new StreamResult(response.getOutputStream());
    response.setContentType("application/rss+xml");
    transformer.transform(source, sresult);
  }

  public static Element addNode(Document doc, Node parent, String name)
  {
    Element child = doc.createElement(name);
    parent.appendChild(child);
    return child;
  }
  
  public static Element addNode(Document doc, Node parent, String name, String text)
  {
    if ( text == null ) text = "";
    Element child = doc.createElement(name);
    child.appendChild(doc.createTextNode(getLegalXml(text)));
    parent.appendChild(child);
    return child;
  }
  
  public static Element addNode(Document doc, Node parent, String ns, String name, String text)
  {
    if ( text == null ) text = "";
    Element child = doc.createElementNS( ns, name);
    child.appendChild( doc.createTextNode( getLegalXml(text) ) );
    parent.appendChild( child );
    return child;
  }

  public static Node addAttribute(Document doc, Element node, String name, String value)
  {
    Attr attribute = doc.createAttribute(name);
    attribute.setValue(getLegalXml(value));
    node.getAttributes().setNamedItem(attribute);
    return node;
  }
  
  /*
   * Ensure string is legal xml.
   * @param text String to verify.
   * @return Passed <code>text</code> or a new string with illegal
   * characters removed if any found in <code>text</code>.
   * @see http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char
   */
  protected static String getLegalXml(final String text) {
      if (text == null) {
          return null;
      }
      StringBuffer buffer = null;
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (!isLegalXml(c)) {
	  if (buffer == null) {
              // Start up a buffer.  Copy characters here from now on
              // now we've found at least one bad character in original.
	      buffer = new StringBuffer(text.length());
              buffer.append(text.substring(0, i));
          }
        } else {
           if (buffer != null) {
             buffer.append(c);
           }
        }
      }
      return (buffer != null)? buffer.toString(): text;
  }
 
  public static boolean isLegalXml(final char c)
  {
    return c == 0x9 || c == 0xa || c == 0xd || (c >= 0x20 && c <= 0xd7ff)
      || (c >= 0xe000 && c <= 0xfffd) || (c >= 0x10000 && c <= 0x10ffff);
  }

  static class Parameters
  {
    String   query;
    int      start;
    int      hitsPerPage;
    int      hitsPerSite;
    String[] sites;
    String[] indexNames;
    String[] collections;
    String[] types;
  }

}
