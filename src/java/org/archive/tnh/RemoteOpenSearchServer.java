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
import java.util.regex.*;
import java.util.logging.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;

/**
 * Represents, and queries a remote OpenSearch server.  Instantiate
 * with an OpenSearch template, then use <code>query()</code> to
 * connect to the remote server, submit the query and return the
 * results as an XML document.
 */
class RemoteOpenSearchServer
{
  public static final Logger LOG = Logger.getLogger( RemoteOpenSearchServer.class.getName() );

  private String urlTemplate;

  public RemoteOpenSearchServer( String urlTemplate )
  {
    this.urlTemplate = urlTemplate;
  }

  public Document query( QueryParameters p )
    throws Exception
  {
    URL url = buildRequestUrl( p );
    
    InputStream is = null;
    try
      {
        LOG.info( "Querying remote: " + url );

        is = getInputStream( url );
        
        Document doc = (new SAXBuilder()).build( is );

        doc = validate( doc );

        return doc;
      }
    catch ( Exception e )
      {
        LOG.log( Level.SEVERE, url.toString(), e );
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
        Element site = item.getChild( "site", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) );
        if ( site == null )
          {
            item.addContent( new Element( "site", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) ) );
          }
        
        Element score = item.getChild( "score", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) );
        if ( score == null )
          {
            item.addContent( new Element( "score", Namespace.getNamespace( OpenSearchHelper.NS_ARCHIVE ) ) );
          }
      }

    return doc;
  }

  /**
   * 
   */
  public URL buildRequestUrl( QueryParameters p )
    throws MalformedURLException, UnsupportedEncodingException
  {
    String url = this.urlTemplate;
    
    // Note about replaceAll: In the Java regex library, the replacement string has a few
    // special characters: \ and $.  Forunately, since we URL-encode the replacement string,
    // any occurance of \ or $ is converted to %xy form.  So we don't have to worry about it. :)
    url = url.replaceAll( "[{]searchTerms[}]", URLEncoder.encode( p.query, "utf-8" ) );
    url = url.replaceAll( "[{]startIndex[}]" , String.valueOf( p.start ) );
    url = url.replaceAll( "[{]count[}]"      , String.valueOf( p.hitsPerPage ) );
    url = url.replaceAll( "[{]hitsPerSite[}]", String.valueOf( p.hitsPerSite ) );

    url = replaceMultiple( url, "sites",       p.sites       );
    url = replaceMultiple( url, "indexNames",  p.indexNames  );
    url = replaceMultiple( url, "collections", p.collections );
    url = replaceMultiple( url, "types",       p.types       );
    url = replaceMultiple( url, "dates",       p.dates       );
    url = replaceMultiple( url, "excludes",    p.excludes    );

    // We don't know about any optional parameters, so we remove them (per the OpenSearch spec.)
    url = url.replaceAll( "[{][^}]+?[}]", "" );

    // Collapse multiple '&' into one, e.g. "&&&" -> "&"
    url = url.replaceAll( "[&]{2,}+", "&" );

    // Lastly, remove any extra '&' left-over from the previous substitutions.
    url = url.replaceAll( "[&]+$", "" );

    return new URL( url );
  }

  /**
   * Extenstion of OpenSearch template syntax to allow us to specify where multiple 
   * "p=v" params should go.  For example, "{x=sites}" might be transformed into
   * "x=site1&x=site2&x=site3".
   */
  public String replaceMultiple( String url, String name, String[] values )
    throws UnsupportedEncodingException
  {
    String key = "=" + name + "}";

    int paramEnd = url.indexOf( key );
    if ( paramEnd > 0 )
      {
        for ( int i = paramEnd ; i >= 0 ; i-- )
        {
          if ( url.charAt( i ) == '{' )
            {
              String param = url.substring( i + 1, paramEnd );

              StringBuilder buf = new StringBuilder( );
              for ( int v = 0 ; v < values.length ; v++ )
                {
                  if ( v > 0 ) buf.append( '&' );
                  buf.append( param );
                  buf.append( '=' );
                  buf.append( URLEncoder.encode( values[v], "utf-8" ) );
                }
              
              url = url.substring( 0, i ) + buf.toString( ) + url.substring( paramEnd + key.length( ) );

              return url;
            }
        }
      }

    return url;
  }

  public InputStream getInputStream( URL url )
    throws IOException
  {
    URLConnection connection = url.openConnection( );
    connection.setDoOutput( false );
    connection.setRequestProperty( "User-Agent", "Mozilla/4.0 (compatible; Internet Archive RemoteOpenSearchServer)" );
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
    
    QueryParameters p = new QueryParameters( );
    p.query = query;
    p.start = 0;
    p.hitsPerPage = numHits;
    p.hitsPerSite = hitsPerSite;
    p.sites = new String[] { "clinton.senate.gov", "house.gov" };
    
    Document doc = ross.query( p );

    (new XMLOutputter()).output( doc, System.out );
  }

}
