/*
 * Copyright 2010 Internet Archive
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.archive.tnh.servlet;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;
import org.jdom.output.XMLOutputter;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;

import org.archive.tnh.*;
import org.archive.tnh.nutch.*;


public class OpenSearchServlet extends HttpServlet
{
  public static final Logger LOG = Logger.getLogger( OpenSearchServlet.class.getName() );

  public int     hitsPerSite;
  public int     hitsPerPage;
  public int     hitsPerPageMax;
  public int     positionMax;

  public int     indexDivisor;
  public String  indexPath;
  public String  segmentPath;
  public boolean foldAccents;
  public boolean explain;
  public Search  searcher;
  
  public DefaultQueryTranslator translator;
  public Segments segments;
  
  public void init( ServletConfig config )
    throws ServletException
  {
    this.hitsPerSite    = ServletHelper.getInitParameter( config, "hitsPerSite",    1, 0 );
    this.hitsPerPage    = ServletHelper.getInitParameter( config, "hitsPerPage",    10, this.hitsPerPageMax );
    this.hitsPerPageMax = ServletHelper.getInitParameter( config, "hitsPerPageMax", Integer.MAX_VALUE, 1 );
    this.positionMax    = ServletHelper.getInitParameter( config, "positionMax",    Integer.MAX_VALUE, 1 );

    this.indexDivisor   = ServletHelper.getInitParameter( config, "indexDivisor",   1, 1 );
    this.indexPath      = ServletHelper.getInitParameter( config, "index",          false );
    this.segmentPath    = ServletHelper.getInitParameter( config, "segments",       true );

    this.foldAccents    = ServletHelper.getInitParameter( config, "foldAccents",    Boolean.TRUE );
    this.explain        = ServletHelper.getInitParameter( config, "explain",        Boolean.FALSE );

    try
      {
        this.searcher = new Search( IndexOpener.open( indexPath, indexDivisor ) );

        if ( this.segmentPath.length() != 0 )
          {
            this.segments = new Segments( this.segmentPath );
          }
      }
    catch ( IOException ioe )
      {
        throw new ServletException( ioe );
      }
    
    config.getServletContext().setAttribute( "tnh.search", this.searcher );

    this.translator = new DefaultQueryTranslator( );
  }

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
  {
    try
      {
        long responseTime = System.nanoTime( );
        
        QueryParameters p = (QueryParameters) request.getAttribute( OpenSearchHelper.PARAMS_KEY );
        if ( p == null )
          {
            p = getQueryParameters( request );
          }

        BooleanQuery q = this.translator.translate( p.query, this.foldAccents );

        this.translator.addFilterGroup( q, "site", p.sites );
        this.translator.addFilterGroup( q, "type", p.types );
        this.translator.addFilterGroup( q, "collection", p.collections );
        this.translator.addFilterGroup( q, "date", p.dates );

        long parseQueryTime = System.nanoTime();

        if ( Arrays.equals( p.indexNames, QueryParameters.ALL_INDEXES ) )
          {
            if ( p.excludes.length > 0 )
              {
                // If there are indexes to exclude, exclude them.
                p.indexNames = removeExcludes( p.excludes );
              }
          }
        else
          {
            // There are explicitly named indexes.  Weed out any unknown names.
            p.indexNames = removeUnknownIndexNames( p.indexNames );
          }

        Search.Result result;
        if ( p.indexNames.length == 0 )
          {
            result = new Search.Result( );
            result.hits = new Hit[0];
          }
        else
          {
            result = this.searcher.search( p.indexNames, q, p.start + (p.hitsPerPage*3), p.hitsPerSite );
          }

        long executeQueryTime = System.nanoTime();

        // The 'end' is usually just the end of the current page
        // (start+hitsPerPage); but if we are on the last page
        // of de-duped results, then the end is hits.getLength().
        int end = Math.min( result.hits.length, p.start + p.hitsPerPage );
        
        // The length is usually just (end-start), unless the start
        // position is past the end of the results -- which is common when
        // de-duping.  The user could easily jump past the true end of the
        // de-dup'd results.  If the start is past the end, we use a
        // length of '0' to produce an empty results page.
        int length = Math.max( end-p.start, 0 );
        
        // Usually, the total results is the total number of non-de-duped
        // results.  Howerver, if we are on last page of de-duped results,
        // then we know our de-dup'd total is result.hits.length.
        long totalResults = result.hits.length < (p.start+p.hitsPerPage) ? result.hits.length : result.numRawHits; 

        Document doc = new Document( );

        Element channel = OpenSearchHelper.startResponse( doc, p, request, totalResults );
        
        // Add hits to XML Document
        for ( int i = p.start ; i < end ; i++ )
          {
            org.apache.lucene.document.Document hit = result.searcher.doc( result.hits[i].id );
            
            Element item = JDOMHelper.add( channel, "item" );

            // Replace & and < with their XML entity counterparts to
            // ensure that any HTML markup in the snippet is escaped
            // before we do the highlighting.
            String title = hit.get( "title" );
            if ( title != null )
              {
                title = title.replaceAll( "[&]", "&amp;" );
                title = title.replaceAll( "[<]", "&lt;"  );
              }
            JDOMHelper.add( item, "title" , title );

            JDOMHelper.add( item, "link"  , hit.get( "url" ) );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "docId",      String.valueOf( result.hits[i].id    ) );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "score",      String.valueOf( result.hits[i].score ) );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "site",       result.hits[i].site  );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "length",     hit.get( "length"     ) );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "type",       hit.get( "type"       ) );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "boost",      hit.get( "boost"      ) );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "collection", hit.get( "collection" ) );

            String indexName = this.searcher.resolveIndexName( result.searcher, result.hits[i].id );
            JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "index",      indexName );

            for ( String date : hit.getValues( "date" ) )
              {
                JDOMHelper.add( item, "date", date );
              }

            String raw = getContent( hit );

            StringBuilder buf = new StringBuilder( 100 );

            Highlighter highlighter = new Highlighter( new SimpleHTMLFormatter(), 
                                                       new NonBrokenHTMLEncoder(), 
                                                       new QueryScorer( q, "content" ) );
            
            CustomAnalyzer analyzer = new CustomAnalyzer( );
            analyzer.setFoldAccents( this.foldAccents );

            for ( String snippet : highlighter.getBestFragments( analyzer, "content", raw, 8 ) )
              {
                buf.append( snippet );
                buf.append( "..." );
              }

            JDOMHelper.add( item, "description", buf.toString( ) );

            // Last, but not least, add a hit explanation, if enabled
            if ( explain )
              {
                JDOMHelper.add( item, OpenSearchHelper.NS_ARCHIVE, "explain", result.searcher.explain( q, result.hits[i].id ).toHtml() );
              }
          }

        OpenSearchHelper.addResponseTime( channel, System.nanoTime( ) - responseTime );
        
        long buildResultsTime = System.nanoTime();

        OpenSearchHelper.writeResponse( doc, response, "application/rss+xml" );

        long writeResponseTime = System.nanoTime();

        LOG.info( "S: " 
                  + ((parseQueryTime-responseTime)/1000/1000)
                  + " " 
                  + ((executeQueryTime-parseQueryTime)/1000/1000)
                  + " " 
                  + ((buildResultsTime-executeQueryTime)/1000/1000)
                  + " " 
                  + ((writeResponseTime-buildResultsTime)/1000/1000)
                  + " " 
                  + p.query );
      }
    catch ( Exception e )
      {
        throw new ServletException( e );
      }
  }

  public String[] removeUnknownIndexNames( String[] names )
  {
    Set<String> known = new HashSet( names.length );

    for ( int i = 0; i < names.length ; i++ )
      {
        if ( this.searcher.hasIndex( names[i] ) ) known.add( names[i] );
      }

    return known.toArray( new String[known.size()] );
  }

  public String[] removeExcludes( String[] excludes )
  {
    // No explicit indexes requested, but if there are
    // excludes, then create a new list of indexes with all
    // the names except those to be excluded.
    Set<String> names = new HashSet<String>( this.searcher.getIndexNames( ) );
    
    // First, remove the magic "all indexes" name.
    names.remove( "" );
    
    // Then, remove all the names in the exclude list.
    for ( int i = 0 ; i < excludes.length ; i++ )
      {
        names.remove( excludes[i] );
      }
    
    // Lastly, keep the new list of index names.
    return names.toArray( new String[names.size()] );
  }

  public QueryParameters getQueryParameters( HttpServletRequest request )
  {
    QueryParameters p = new QueryParameters( );
    
    p.query      = ServletHelper.getParam( request, "q",  "" );
    p.start      = ServletHelper.getParam( request, "p",  0 );
    p.hitsPerPage= ServletHelper.getParam( request, "n",  this.hitsPerPage );
    p.hitsPerSite= ServletHelper.getParam( request, "h",  this.hitsPerSite );
    p.sites      = ServletHelper.getParam( request, "s",  QueryParameters.EMPTY_STRINGS );
    p.indexNames = ServletHelper.getParam( request, "i",  QueryParameters.ALL_INDEXES   );
    p.excludes   = ServletHelper.getParam( request, "x",  QueryParameters.EMPTY_STRINGS );
    p.collections= ServletHelper.getParam( request, "c",  QueryParameters.EMPTY_STRINGS );
    p.types      = ServletHelper.getParam( request, "t",  QueryParameters.EMPTY_STRINGS );
    p.dates      = ServletHelper.getParam( request, "d",  QueryParameters.EMPTY_STRINGS );

    if ( p.start > this.positionMax )
      {
        p.start = this.positionMax;
      }

    if ( p.hitsPerPage > this.hitsPerPageMax )
      {
        p.hitsPerPage = this.hitsPerPageMax;
      }

    return p;
  }

  /*
   * Nasty bit of hackery to obtain the "content" from one of a few
   * possible places:
   *   1. In "content" field as a byte[], then uncompress.
   *   2. In "content" field of the Lucene document as a String.
   *   3. In NutchWAX segment directory.
   *
   * Lucene indices built with NutchWAX 0.12 don't store the content
   * field, we have to find the value in the segment (#3).
   *
   * Indices built with a hacked version of NutchWAX 0.13 using Lucene
   * 2.x have the "content" stored in compressed form.  The Lucene
   * library auto-gunzips it for us.  It's totally transparent. (#2)
   *
   * Indices built with NutchWAX using Lucene 3.0.x do the
   * compression/decompression manually using Lucene's
   * CompressionTools class.  The Lucene dev team took out the
   * automagic compressing when indexing, so we have to do it
   * ourselves. (#1).
   */
  public String getContent( org.apache.lucene.document.Document hit )
    throws IOException
  {
    // If the 'content' field is stored as a binary value, we assume
    // that it is a compressed String and uncompress it.
    byte[] rawbytes = hit.getBinaryValue( "content" );

    if ( rawbytes != null )
      {
        try
          {
            String raw = CompressionTools.decompressString( rawbytes );

            return raw;
          }
        catch ( java.util.zip.DataFormatException dfe )
          {
            // If the format isn't valid, continue looking in the
            // other places.
          }
      }

    // Not a binary, if we can get it as a String, then do that.
    String raw = hit.get( "content" );

    if ( raw == null )
      {
        // No string value, try the segment.
        raw = getContentFromSegment( hit );
      }

    // Never return null, return empty string instead.
    raw = raw == null ? "" : raw;
    
    return raw;
  }

  public String getContentFromSegment( org.apache.lucene.document.Document hit )
  {
    if ( this.segments == null ) return "";

    String c = hit.get( "collection" );
    String s = hit.get( "segment"    );
    String u = hit.get( "url"        );
    String d = hit.get( "digest"     );

    return this.segments.getParseText( c, s, u + " " + d );
  }
}
