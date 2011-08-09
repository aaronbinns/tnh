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

package org.archive.tnh;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;


/**
 * Performs meta-search/federated search against multiple OpenSearch
 * services.
 *
 * Instantiate with a path to a text file containing a list of
 * OpenSearch templates.  These OpenSearch servers will be queried by
 * the MetaOpenSearch object.
 *
 * Results from the many remote OpenSearch servers are combined and
 * sorted by score.  The scores are exactly as provided by the remote
 * servers.  Multiple hits from the same site are collapsed to just
 * the top N hits, where N is passed in as part of the
 * QueryParameters.
 */
public class MetaOpenSearch
{
  public static final Logger LOG = Logger.getLogger( MetaOpenSearch.class.getName() );

  List<RemoteOpenSearchServer> remotes = new ArrayList<RemoteOpenSearchServer>( );
  long timeout = 0;

  public MetaOpenSearch( URL templates, long timeout )
    throws IOException
  {
    this( templates );
    this.timeout = timeout;
  }

  public MetaOpenSearch( URL templates )
    throws IOException
  {
    BufferedReader r = null;
    try
      {
        r = new BufferedReader( new InputStreamReader( templates.openStream(), "utf-8" ) );

        String line;
        while ( (line = r.readLine()) != null )
          {
            line = line.trim();
            if ( line.length() == 0 || line.charAt( 0 ) == '#' )
              {
                // Ignore it.
                continue ;
              }

            RemoteOpenSearchServer ross = new RemoteOpenSearchServer( line );

            this.remotes.add( ross );
          }
      }
    finally
      {
        try { if ( r != null ) r.close(); } catch ( IOException ioe ) { }
      }
    
  }

  public Document query( QueryParameters p )
  {
    Map<String,String[]> r = Collections.emptyMap( );

    return this.query( p, r );
  }

  public Document query( QueryParameters p, Map<String,String[]> requestParams )
  {
    long startTime = System.nanoTime( );

    // For the remote servers, start at position 0 and ask for enough
    // results to handle the start offset on the metasearch side.
    QueryParameters rp = new QueryParameters( p );
    rp.start = 0;
    rp.hitsPerPage += p.start;

    List<RemoteQueryThread> remoteThreads = new ArrayList<RemoteQueryThread>( this.remotes.size() );

    for ( RemoteOpenSearchServer remote : this.remotes )
      {
        RemoteQueryThread sqt = new RemoteQueryThread( remote, rp );

        sqt.start( );

        remoteThreads.add( sqt );
      }

    waitForThreads( remoteThreads, this.timeout );

    LinkedList<Element> items = new LinkedList<Element>( );
    long totalResults = 0;

    for ( RemoteQueryThread sqt : remoteThreads )
      {
        if ( sqt.throwable != null )
          {
            continue ;
          }

        try
          {
            // Dump all the results ("item" elements) into a single list.
            Element channel = sqt.response.getRootElement( ).getChild( "channel" );
            items.addAll( (List<Element>) channel.getChildren( "item" ) );
            channel.removeChildren( "item" );

            totalResults += Integer.parseInt( channel.getChild( "totalResults", Namespace.getNamespace( OpenSearchHelper.NS_OPENSEARCH ) ).getTextTrim( ) );
          }
        catch ( Exception e )
          {
            LOG.log( Level.SEVERE, "Error processing response from: " + sqt.remote, e );
          }

      }

    if ( items.size( ) > 0 && p.hitsPerSite > 0 )
      {
        Collections.sort( items, new ElementSiteThenScoreComparator( ) );

        LinkedList<Element> collapsed = new LinkedList<Element>( );

        collapsed.add( items.removeFirst( ) );

        int count = 1;
        for ( Element item : items )
          {
            String lastSite = collapsed.getLast( ).getChild( "site", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) ).getTextTrim( );

            if ( lastSite.length( ) == 0 ||
                 !lastSite.equals( item.getChild( "site", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) ).getTextTrim( ) ) )
              {
                collapsed.add( item );
                count = 1;
              }
            else if ( count < p.hitsPerSite )
              {
                collapsed.add( item );
                count++;
              }
          }

        // Replace the list of items with the collapsed list.
        items = collapsed;
      }

    Collections.sort( items, new ElementScoreComparator( ) );

    // Build the final results OpenSearch XML document.
    Document doc = new Document( );

    Element channel = OpenSearchHelper.startResponse( doc, p, requestParams, totalResults );

    // Get a sub-list of only the items we want: [startIndex,(startIndex+numResults)]
    List<Element> subList = items.subList( Math.min(  p.start,                items.size( ) ),
                                           Math.min( (p.start+p.hitsPerPage), items.size( ) ) );
    channel.addContent( subList );

    OpenSearchHelper.addResponseTime( channel, System.nanoTime() - startTime );

    return doc;
  }


  /**
   * Convenience method to wait for a collection of threads to complete,
   * or until a timeout after a startTime expires.
   */
  private void waitForThreads( List<RemoteQueryThread> threads, long timeout )
  {
    for ( Thread t : threads )
      {
        try
          {
            t.join( timeout );
          }
        catch ( InterruptedException ie )
          {
            break;
          }
      }
  }


  public static void main( String args[] )
    throws Exception
  {
    String usage = "MetaOpenSearch [OPTIONS] REMOTES.txt query"
      + "\n\t-h <n>    Hits per site"
      + "\n\t-n <n>    Number of results"
      + "\n\t-s <n>    Start index"
      + "\n";

    if ( args.length < 2 )
      {
        System.err.println( usage );
        System.exit( 1 );
      }

    String templates = args[args.length - 2];
    String query     = args[args.length - 1];

    int startIndex  = 0;
    int hitsPerSite = 0;
    int numHits     = 10;
    for ( int i = 0 ; i < args.length - 2 ; i++ )
      {
        try
          {
            if ( "-h".equals( args[i] ) )
              {
                i++;
                hitsPerSite = Integer.parseInt( args[i] );
              }
            if ( "-n".equals( args[i] ) )
              {
                i++;
                numHits = Integer.parseInt( args[i] );
              }
            if ( "-s".equals( args[i] ) )
              {
                i++;
                startIndex = Integer.parseInt( args[i] );
              }
          }
        catch ( NumberFormatException nfe )
          {
            System.err.println( "Error: not a numeric value: " + args[i] );
            System.err.println( usage );
            System.exit( 1 );
          }
      }

    MetaOpenSearch meta = new MetaOpenSearch( new URL( templates ) );

    QueryParameters p = new QueryParameters( );
    p.query = query;
    p.start = startIndex;
    p.hitsPerPage = numHits;
    p.hitsPerSite = hitsPerSite;

    Document doc = meta.query( p );

    (new XMLOutputter()).output( doc, System.out );
  }

}


/**
 * Simple thread wrapper which queries the remote server with the
 * given parameters and stores the response.  If any exception or
 * error occurs in the thread, it is captured as well.
 */
class RemoteQueryThread extends Thread
{
  RemoteOpenSearchServer remote;

  QueryParameters p;
  Document        response;
  Throwable       throwable;


  RemoteQueryThread( RemoteOpenSearchServer remote, QueryParameters p )
  {
    this.remote      = remote;
    this.p           = p;
  }

  public void run( )
  {
    try
      {
        this.response = this.remote.query( this.p );
      }
    catch ( Throwable t )
      {
        this.throwable = t;
      }
  }
}


class ElementScoreComparator implements Comparator<Element>
{
  public int compare( Element e1, Element e2 )
  {
    if ( e1 == e2 )   return 0;
    if ( e1 == null ) return 1;
    if ( e2 == null ) return -1;

    Element score1 = e1.getChild( "score", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) );
    Element score2 = e2.getChild( "score", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE )  );

    if ( score1 == score2 ) return 0;
    if ( score1 == null )   return 1;
    if ( score2 == null )   return -1;

    String text1 = score1.getText().trim();
    String text2 = score2.getText().trim();

    float value1 = 0.0f;
    float value2 = 0.0f;

    try { value1 = Float.parseFloat( text1 ); } catch ( NumberFormatException nfe ) { }
    try { value2 = Float.parseFloat( text2 ); } catch ( NumberFormatException nfe ) { }

    if ( value1 == value2 ) return 0;

    return value1 > value2 ? -1 : 1;
  }
}

class ElementSiteThenScoreComparator extends ElementScoreComparator
{
  public int compare( Element e1, Element e2 )
  {
    if ( e1 == e2 )   return 0;
    if ( e1 == null ) return 1;
    if ( e2 == null ) return -1;

    String site1 = e1.getChild( "site", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) ).getTextTrim();
    String site2 = e2.getChild( "site", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) ).getTextTrim();

    if ( site1.equals( site2 ) )
      {
        // Sites are equal, then compare scores.
        return super.compare( e1, e2 );
      }

    return site1.compareTo( site2 );
  }
}
