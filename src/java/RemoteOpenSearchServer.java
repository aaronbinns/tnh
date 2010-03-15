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
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;


class RemoteOpenSearchServer
{
  //public static final Log LOG = LogFactory.getLog( RemoteOpenSearchServer.class );

  private String urlTemplate;

  public RemoteOpenSearchServer( String urlTemplate )
  {
    this.urlTemplate = urlTemplate;
  }

  public Document query( String query, int startIndex, int requestedNumResults, int hitsPerSite )
    throws Exception
  {
    URL url = buildRequestUrl( query, startIndex, requestedNumResults, hitsPerSite );
    
    InputStream is = null;
    try
      {
        //LOG.info( "Querying slave: " + url );

        is = getInputStream( url );
        
        Document doc = (new SAXBuilder()).build( is );

        doc = validate( doc );

        return doc;
      }
    catch ( Exception e )
      {
        //LOG.error( url.toString(), e );
        throw e;
      }
    finally
      {
        // Ensure the InputStream is closed, which should trigger the
        // underlying HTTP connection to be cleaned-up.
        try { if ( is != null ) is.close( ); } catch ( IOException ioe ) { } // Not much we can do
      }
  }

  private Document validate( Document doc )
    throws Exception
  {
    if ( doc.getRootElement( ) == null ) throw new Exception( "Invalid OpenSearch response: missing /rss" );
    Element root = doc.getRootElement( );
    
    if ( ! "rss".equals( root.getName( ) ) ) throw new Exception( "Invalid OpenSearch response: missing /rss" );
    Element channel = root.getChild( "channel" );
    
    if ( channel == null ) throw new Exception( "Invalid OpenSearch response: missing /rss/channel" );

    for ( Element item : (List<Element>) channel.getChildren( "item" ) )
      {
        Element site = item.getChild( "site", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) );
        if ( site == null )
          {
            item.addContent( new Element( "site", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) ) );
          }
        
        Element score = item.getChild( "score", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) );
        if ( score == null )
          {
            item.addContent( new Element( "score", Namespace.getNamespace( "http://www.nutch.org/opensearchrss/1.0/" ) ) );
          }
      }

    return doc;
  }

  /**
   * 
   */
  public URL buildRequestUrl( String query, int startIndex, int requestedNumResults, int hitsPerSite )
    throws MalformedURLException, UnsupportedEncodingException
  {
    String url = this.urlTemplate;
    
    // Note about replaceAll: In the Java regex library, the replacement string has a few
    // special characters: \ and $.  Forunately, since we URL-encode the replacement string,
    // any occurance of \ or $ is converted to %xy form.  So we don't have to worry about it. :)
    url = url.replaceAll( "[{]searchTerms[}]", URLEncoder.encode( query, "utf-8" ) );
    url = url.replaceAll( "[{]count[}]"      , String.valueOf( requestedNumResults ) );
    url = url.replaceAll( "[{]startIndex[}]" , String.valueOf( startIndex ) );
    url = url.replaceAll( "[{]hitsPerSite[}]", String.valueOf( hitsPerSite ) );

    // We don't know about any optional parameters, so we remove them (per the OpenSearch spec.)
    url = url.replaceAll( "[{][^}]+[?][}]", "" );
    
    return new URL( url );
  }


  public InputStream getInputStream( URL url )
    throws IOException
  {
    URLConnection connection = url.openConnection( );
    connection.setDoOutput( false );
    connection.setRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; NutchWAX OpenSearchMaster)" );
    connection.connect( );

    if ( connection instanceof HttpURLConnection )
      {
        HttpURLConnection hc = (HttpURLConnection) connection;

        switch ( hc.getResponseCode( ) )
          {
          case 200:
            // All good.
            break;
          default:
            // Problems!  Bail out.
            throw new IOException( "HTTP error from " + url + ": " + hc.getResponseMessage( ) );
          }
      }

    InputStream is = connection.getInputStream( );

    return is;
  }

  public String toString()
  {
    return this.urlTemplate;
  }

  public static void main( String args[] )
    throws Exception
  {
    String usage = "RemoteOpenSearchServer [OPTIONS] urlTemplate query"
      + "\n\t-h <n>   Hits per site"
      + "\n\t-n <n>   Number of results"
      + "\n";

    if ( args.length < 2 )
      {
        System.err.println( usage );
        System.exit( 1 );
      }

    String urlTemplate = args[args.length - 2];
    String query       = args[args.length - 1];

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
          }
        catch ( NumberFormatException nfe ) 
          {
            System.err.println( "Error: not a numeric value: " + args[i] );
            System.err.println( usage );
            System.exit( 1 );
          }
      }

    RemoteOpenSearchServer ross = new RemoteOpenSearchServer( urlTemplate );
    
    Document doc = ross.query( query, 0, numHits, hitsPerSite );

    (new XMLOutputter()).output( doc, System.out );
  }

}
