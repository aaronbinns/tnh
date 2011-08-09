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
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;


public class OpenSearchHelper
{
  public static final Logger LOG = Logger.getLogger( OpenSearchHelper.class.getName() );

  public static final String PARAMS_KEY = "opensearch.parameters";
  
  public static final String NS_OPENSEARCH = "http://a9.com/-/spec/opensearchrss/1.0/";
  public static final String NS_ARCHIVE    = "http://web.archive.org/-/spec/opensearchrss/1.0/";


  public static Element startResponse( Document doc, QueryParameters p, HttpServletRequest request, long totalResults )
  {
    return startResponse( doc, p, (Map<String,String[]>) request.getParameterMap( ), totalResults );
  }

  /**
   * Helper function to populate the given JDOM Document with the
   * OpenSearch header information.
   */
  public static Element startResponse( Document doc, QueryParameters p, Map<String,String[]> requestParams, long totalResults )
  {
    Element rss = new Element( "rss" );
    rss.setAttribute( "version", "2.0" );

    doc.addContent( rss );
    
    Element channel = new Element( "channel" );
    rss.addContent( channel );

    JDOMHelper.add( channel, "title",       p.query );
    JDOMHelper.add( channel, "description", p.query );
    JDOMHelper.add( channel, "link" );

    JDOMHelper.add( channel, NS_OPENSEARCH, "totalResults", Long   .toString( totalResults  ) );
    JDOMHelper.add( channel, NS_OPENSEARCH, "startIndex",   Integer.toString( p.start       ) );
    JDOMHelper.add( channel, NS_OPENSEARCH, "itemsPerPage", Integer.toString( p.hitsPerPage ) );
    JDOMHelper.add( channel, NS_ARCHIVE,    "query",        p.query );

    for ( String i : p.indexNames )
      {
        JDOMHelper.add( channel, NS_ARCHIVE, "index", i );
      }

    // Add a <urlParams> element containing a list of all the URL parameters.
    Element urlParams = new Element( "urlParams", Namespace.getNamespace( NS_ARCHIVE ) );
    channel.addContent( urlParams );
    
    for ( Map.Entry<String,String[]> param : requestParams.entrySet() )
      {
        String key = param.getKey( );
        for ( String value : param.getValue( ) )
          {
            Element urlParam = new Element( "param", Namespace.getNamespace( NS_ARCHIVE ) );
            urlParam.setAttribute( "name",  key          );
            urlParam.setAttribute( "value", value.trim() );

            urlParams.addContent( urlParam );
          }
      }

    return channel;
  }

  /**
   * Helper function to add the response time to the given JDOM Element.
   */
  public static void addResponseTime( Element channel, long nanos )
  {
    JDOMHelper.add( channel, NS_ARCHIVE, "responseTime" , Double.toString( (nanos / 1000 / 1000) / 1000.0 ) );
  }

  /**
   * Helper function to serialize out the JDOM Document to the
   * reponse.  Default Content-Type is "application/xml".
   */
  public static void writeResponse( Document doc, HttpServletResponse response )
    throws IOException
  {
    writeResponse( doc, response, "application/xml" );
  }

  /**
   * Helper function to serialize out the JDOM Document to the reponse
   * along with explicit Content-Type.
   */
  public static void writeResponse( Document doc, HttpServletResponse response, String contentType )
    throws IOException
  {
    // Enforce the Content-Type in the HTTP response.
    response.setContentType( contentType );

    // TODO: Is creating a new XMLOutputter every time a good idea?
    (new XMLOutputter()).output( doc, response.getOutputStream( ) );
  }


}
