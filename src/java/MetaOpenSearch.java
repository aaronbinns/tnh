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
import java.util.logging.*;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.XMLOutputter;


/** 
 * 
 */   
public class MetaOpenSearch
{
  //  public static final Logger LOG = Logger.getLogger( MetaOpenSearch.class );

  List<RemoteOpenSearchServer> slaves = new ArrayList<RemoteOpenSearchServer>( );
  long timeout = 0;

  public MetaOpenSearch( String slavesFile, long timeout )
    throws IOException
  {
    this( slavesFile );
    this.timeout = timeout;
  }

  public MetaOpenSearch( String slavesFile )
    throws IOException
  {
    BufferedReader r = null;
    try
      {
        r = new BufferedReader( new InputStreamReader( new FileInputStream( slavesFile ), "utf-8" ) );

        String line;
        while ( (line = r.readLine()) != null )
          {
            line = line.trim();
            if ( line.length() == 0 || line.charAt( 0 ) == '#' )
              {
                // Ignore it.
                continue ;
              }

            RemoteOpenSearchServer slave = new RemoteOpenSearchServer( line );

            this.slaves.add( slave );            
          }
      }
    finally
      {
        try { if ( r != null ) r.close(); } catch ( IOException ioe ) { }
      }
    
  }

  public Document query( String query, int startIndex, int numResults, int hitsPerSite )
  {
    long startTime = System.currentTimeMillis( );
    
    List<SlaveQueryThread> slaveThreads = new ArrayList<SlaveQueryThread>( this.slaves.size() );

    for ( RemoteOpenSearchServer slave : this.slaves )
      {
        SlaveQueryThread sqt = new SlaveQueryThread( slave, query, 0, (startIndex+numResults), hitsPerSite );

        sqt.start( );

        slaveThreads.add( sqt );        
      }

    waitForThreads( slaveThreads, this.timeout );

    LinkedList<Element> items = new LinkedList<Element>( );
    long totalResults = 0;

    for ( SlaveQueryThread sqt : slaveThreads )
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
            
            totalResults += Integer.parseInt( channel.getChild( "totalResults", Namespace.getNamespace( "http://a9.com/-/spec/opensearchrss/1.0/" ) ).getTextTrim( ) );
          }
        catch ( Exception e ) 
          {
            //            LOG.log( Level.SEVERE, "Error processing response from slave: " + sqt.slave, e );
          }
        
      }

    if ( items.size( ) > 0 && hitsPerSite > 0 )
      {
        Collections.sort( items, new ElementSiteThenScoreComparator( ) );

        LinkedList<Element> collapsed = new LinkedList<Element>( );
        
        collapsed.add( items.removeFirst( ) );
        
        int count = 1;
        for ( Element item : items )
          {
            String lastSite = collapsed.getLast( ).getChild( "site", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) ).getTextTrim( );

            if ( lastSite.length( ) == 0 ||
                 !lastSite.equals( item.getChild( "site", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) ).getTextTrim( ) ) )
              {
                collapsed.add( item );
                count = 1;                
              }
            else if ( count < hitsPerSite )
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
    Element channel = new Element( "channel" );
    channel.addContent( new Element( "title"       ) );
    channel.addContent( new Element( "description" ) );
    channel.addContent( new Element( "link"        ) );

    Element eTotalResults = new Element( "totalResults", Namespace.getNamespace( "http://a9.com/-/spec/opensearchrss/1.0/" ) );
    Element eStartIndex   = new Element( "startIndex",   Namespace.getNamespace( "http://a9.com/-/spec/opensearchrss/1.0/" ) );
    Element eItemsPerPage = new Element( "itemsPerPage", Namespace.getNamespace( "http://a9.com/-/spec/opensearchrss/1.0/" ) );

    eTotalResults.setText( Long.toString( totalResults ) );
    eStartIndex.  setText( Long.toString( startIndex   ) );
    eItemsPerPage.setText( Long.toString( numResults   ) );

    channel.addContent( eTotalResults );
    channel.addContent( eStartIndex   );
    channel.addContent( eItemsPerPage );

    // Get a sub-list of only the items we want: [startIndex,(startIndex+numResults)]
    List<Element> subList = items.subList( Math.min(  startIndex,             items.size( ) ),
                                           Math.min( (startIndex+numResults), items.size( ) ) );
    channel.addContent( subList );

    Element rss = new Element( "rss" );
    rss.addContent( channel );

    return new Document( rss );
  }


  /**
   * Convenience method to wait for a collection of threads to complete,
   * or until a timeout after a startTime expires.
   */
  private void waitForThreads( List<SlaveQueryThread> threads, long timeout )
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
    String usage = "MetaOpenSearch [OPTIONS] SLAVES.txt query"
      + "\n\t-h <n>    Hits per site"
      + "\n\t-n <n>    Number of results"
      + "\n\t-s <n>    Start index"
      + "\n";
    
    if ( args.length < 2 )
      {
        System.err.println( usage );
        System.exit( 1 );
      }

    String slavesFile = args[args.length - 2];
    String query      = args[args.length - 1];
    
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

    MetaOpenSearch master = new MetaOpenSearch( slavesFile );

    Document doc = master.query( query, startIndex, numHits, hitsPerSite );

    (new XMLOutputter()).output( doc, System.out );
  }

}


class SlaveQueryThread extends Thread
{
  RemoteOpenSearchServer slave;

  String query;
  int    startIndex;
  int    numResults;
  int    hitsPerSite;

  Document        response;
  Throwable       throwable;


  SlaveQueryThread( RemoteOpenSearchServer slave, String query, int startIndex, int numResults, int hitsPerSite )
  {
    this.slave       = slave;
    this.query       = query;
    this.startIndex  = startIndex;
    this.numResults  = numResults;
    this.hitsPerSite = hitsPerSite;
  }

  public void run( )
  {
    try
      {
        this.response = this.slave.query( this.query, this.startIndex, this.numResults, this.hitsPerSite );
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

    Element score1 = e1.getChild( "score", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" )  );
    Element score2 = e2.getChild( "score", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" )  );

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

    String site1 = e1.getChild( "site", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) ).getTextTrim();
    String site2 = e2.getChild( "site", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) ).getTextTrim();
    
    if ( site1.equals( site2 ) )
      {
        // Sites are equal, then compare scores.
        return super.compare( e1, e2 );
      }

    return site1.compareTo( site2 );
  }
}


