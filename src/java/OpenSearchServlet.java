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

import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.*;
//import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.search.highlight.*;


public class OpenSearchServlet extends HttpServlet
{
  public static final String NS_OPENSEARCH = "http://a9.com/-/spec/opensearchrss/1.0/";
  public static final String NS_ARCHIVE   = "http://web.archive.org/-/spec/opensearchrss/1.0/";
  public static final String[] EMPTY_STRINGS = { };
  public static final String[] ALL_INDEXES   = { "" };

  private int indexDivisor = 10;
  private int hitsPerPage  = 10;
  private int hitsPerSite  = 1;

  DefaultQueryTranslator translator;
  Search searcher;
  DocumentBuilderFactory factory;
  
  public void init( ServletConfig config )
    throws ServletException
  {
    String divisorString = config.getInitParameter( "indexDivisor" );
    divisorString = divisorString == null ? "" : divisorString.trim( );
    if ( divisorString.length() != 0 )
      {
        try
          {
            int divisor = Integer.parseInt( config.getInitParameter( "indexDivisor" ) );

            if ( divisor <= 0 )
              {
                throw new ServletException( "Error: 'indexDivisor' must be >= 0, specified value: " + divisor );
              }

            this.indexDivisor = divisor;

            System.err.println( "indexDivisor: using configuration value: " + this.indexDivisor );
          }
        catch ( NumberFormatException nfe )
          {
            throw new ServletException( "Error: bad value for 'indexDivisor' in servlet config: " + divisorString ); 
          }
      }
    else
      {
        System.err.println( "indexDivisor: using built-in default value: " + this.indexDivisor );
      }

    String indexPath = config.getInitParameter( "index" );
    indexPath = indexPath == null ? "" : indexPath.trim( );

    if ( indexPath.length( ) == 0 )
      {
        throw new ServletException( "No index specified." );
      }
    
    try
      {
        this.searcher = new Search( IndexOpener.open( indexPath, indexDivisor ) );
      }
    catch ( IOException ioe )
      {
        throw new ServletException( ioe );
      }
    
    this.factory = DocumentBuilderFactory.newInstance();
    this.factory.setNamespaceAware(true);

    this.translator = new DefaultQueryTranslator( );
  }

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
  {
    try
      {
        long responseTime = System.nanoTime( );
        
        String p;  // Temp variable for getting URL parameter values
        
        request.setCharacterEncoding("UTF-8");
        
        String   query      = getParam( request, "q", "" );
        int      start      = getParam( request, "p", 0 );
        int      hitsPerPage= getParam( request, "n", this.hitsPerPage );
        int      hitsPerSite= getParam( request, "h", 1 );
        String[] sites      = getParam( request, "s", EMPTY_STRINGS );
        String[] indexNames = getParam( request, "i", ALL_INDEXES );
        String[] collections= getParam( request, "c", EMPTY_STRINGS );
        String[] types      = getParam( request, "t", EMPTY_STRINGS );
        
        BooleanQuery q = this.translator.translate( query );
        System.out.println( "query: " + query );
        System.out.println( "trans: " + q );
        this.translator.addGroup( q, "site", sites );
        this.translator.addGroup( q, "type", types );
        this.translator.addGroup( q, "collection", collections );
        System.out.println( "tran2: " + q );

        Search.Result result = this.searcher.search( indexNames, q, start + (hitsPerPage*3), hitsPerSite );
        // responseTime = System.nanoTime( ) - responseTime;

        // The 'end' is usually just the end of the current page
        // (start+hitsPerPage); but if we are on the last page
        // of de-duped results, then the end is hits.getLength().
        int end = Math.min( result.hits.length, start + hitsPerPage );
        
        // The length is usually just (end-start), unless the start
        // position is past the end of the results -- which is common when
        // de-duping.  The user could easily jump past the true end of the
        // de-dup'd results.  If the start is past the end, we use a
        // length of '0' to produce an empty results page.
        int length = Math.max( end-start, 0 );
        
        // Usually, the total results is the total number of non-de-duped
        // results.  Howerver, if we are on last page of de-duped results,
        // then we know our de-dup'd total is result.hits.length.
        long totalResults = result.hits.length < (start+hitsPerPage) ? result.hits.length : result.numRawHits; 
                                                                                                     
        // Write the OpenSearch header
        Document doc = factory.newDocumentBuilder().newDocument();
 
        Element rss = addNode(doc, doc, "rss");
        addAttribute(doc, rss, "version", "2.0");

        Element channel = addNode(doc, rss, "channel");
        
        addNode(doc, channel, "title", "Search: " + query );
        addNode(doc, channel, "description", "Archival search results for query: " + query);
        addNode(doc, channel, "link", "" );
        
        addNode(doc, channel, NS_OPENSEARCH, "totalResults", ""+totalResults );
        addNode(doc, channel, NS_OPENSEARCH, "startIndex",   ""+start        );
        addNode(doc, channel, NS_OPENSEARCH, "itemsPerPage", ""+hitsPerPage  );
        
        addNode(doc, channel, NS_ARCHIVE, "query", query );
        addNode(doc, channel, NS_ARCHIVE, "luceneQuery", q.toString() );

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

        // Add hits to XML Document
        for ( int i = start ; i < end ; i++ )
          {
            org.apache.lucene.document.Document hit = result.searcher.doc( result.hits[i].id );

            Element item = addNode( doc, channel, "item" );

            addNode( doc, item, "title" , hit.get( "title"  ) );
            addNode( doc, item, "link"  , hit.get( "url"    ) );
            addNode( doc, item, NS_ARCHIVE, "docId",  String.valueOf( result.hits[i].id    ) );
            addNode( doc, item, NS_ARCHIVE, "score",  String.valueOf( result.hits[i].score ) );
            addNode( doc, item, NS_ARCHIVE, "site",   result.hits[i].site  );
            addNode( doc, item, NS_ARCHIVE, "length", hit.get( "length" ) );
            addNode( doc, item, NS_ARCHIVE, "type",   hit.get( "type"   ) );
            addNode( doc, item, NS_ARCHIVE, "collection", hit.get( "collection" ) );
            
            for ( String date : hit.getValues( "date" ) )
              {
                addNode( doc, item, "date", date );
              }

            Highlighter highlighter = new Highlighter( new QueryScorer( q, "content" ) );
            
            StringBuffer buf = new StringBuffer( 100 );
            String       raw = hit.get( "content" );
            raw = raw == null ? "" : raw;
            for ( String snippet : highlighter.getBestFragments( new SimpleAnalyzer( ), "content", raw, 8 ) )
              {
                buf.append( snippet );
                buf.append( "..." );
              }

            addNode( doc, item, "description", buf.toString( ) );
          }

        responseTime = System.nanoTime( ) - responseTime;
        addNode(doc, channel, NS_ARCHIVE, "responseTime", Double.toString( ((long) responseTime / 1000 / 1000 ) / 1000.0 ) );

        DOMSource source = new DOMSource(doc);
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        transformer.setOutputProperty( javax.xml.transform.OutputKeys.ENCODING, "UTF-8" );
        StreamResult sresult = new StreamResult(response.getOutputStream());
        response.setContentType("application/rss+xml");
        transformer.transform(source, sresult);
      }
    catch ( Exception e )
      {
        throw new ServletException( e );
      }
  }


  private String getParam( HttpServletRequest request, String name, String defaultValue )
  {
    String v = request.getParameter( name );
    
    v = v == null ? defaultValue : v.trim();

    return v;
  }

  private int getParam( HttpServletRequest request, String name, int defaultValue )
  {
    String v = request.getParameter( name );
    
    v = v == null ? "" : v.trim();

    if ( v.length( ) == 0 ) return defaultValue;

    try
      {
        int i = Integer.parseInt( v );

        if ( i < 0 ) return defaultValue;
        
        return i;
      }
    catch ( NumberFormatException nfe )
      {
        return defaultValue;
      }
  }

  private String[] getParam( HttpServletRequest request, String name, String[] defaultValue )
  {
    String[] v = request.getParameterValues( name );
    
    v = v == null ? defaultValue : v;

    return v;
  }
  
  private static Element addNode(Document doc, Node parent, String name)
  {
    Element child = doc.createElement(name);
    parent.appendChild(child);
    return child;
  }
  
  private static Element addNode(Document doc, Node parent, String name, String text)
  {
    if ( text == null ) text = "";
    Element child = doc.createElement(name);
    child.appendChild(doc.createTextNode(getLegalXml(text)));
    parent.appendChild(child);
    return child;
  }
  
  private static Element addNode(Document doc, Node parent, String ns, String name, String text)
  {
    if ( text == null ) text = "";
    Element child = doc.createElementNS( ns, name);
    child.appendChild( doc.createTextNode( getLegalXml(text) ) );
    parent.appendChild( child );
    return child;
  }

  private static Node addAttribute(Document doc, Element node, String name, String value)
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
 
  private static boolean isLegalXml(final char c) {
    return c == 0x9 || c == 0xa || c == 0xd || (c >= 0x20 && c <= 0xd7ff)
        || (c >= 0xe000 && c <= 0xfffd) || (c >= 0x10000 && c <= 0x10ffff);
  }

}
