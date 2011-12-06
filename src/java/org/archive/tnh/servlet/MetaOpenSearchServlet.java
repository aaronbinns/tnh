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
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;
import org.jdom.output.XMLOutputter;

import org.archive.tnh.*;

/** 
 * Simple servlet wrapper/interface around MetaOpenSearch.
 */   
public class MetaOpenSearchServlet extends HttpServlet
{
  public static final Logger LOG = Logger.getLogger( MetaOpenSearchServlet.class.getName() );

  MetaOpenSearch meta;

  int timeout;
  int hitsPerSite;
  int hitsPerPage;
  int hitsPerPageMax;
  int positionMax;

  public void init( ServletConfig config )
    throws ServletException 
  {
    this.timeout        = ServletHelper.getInitParameter( config, "timeout",         0,  0 );
    this.hitsPerSite    = ServletHelper.getInitParameter( config, "hitsPerSite",     1,  0 );
    this.hitsPerPage    = ServletHelper.getInitParameter( config, "hitsPerPage",    10,  1 );
    this.hitsPerPageMax = ServletHelper.getInitParameter( config, "hitsPerPageMax", Integer.MAX_VALUE, 1 );
    this.positionMax    = ServletHelper.getInitParameter( config, "positionMax",    Integer.MAX_VALUE, 1 );

    try
      {
        URL remotes = ServletHelper.getResource( ServletHelper.getInitParameter( config, "remotes", false ) );
        
        LOG.info( "Loading remotes: " + remotes );
        
        this.meta = new MetaOpenSearch( remotes, timeout );
      }
    catch ( IOException ioe )
      {
        throw new ServletException( ioe );
      }
    
  }

  public void destroy( )
  {
    
  }

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException 
  {
    long responseTime = System.nanoTime( );
    
    QueryParameters p = (QueryParameters) request.getAttribute( OpenSearchHelper.PARAMS_KEY );
    if ( p == null )
      {
        p = getQueryParameters( request );
      }

    Document doc = meta.query( p, (Map<String,String[]>) request.getParameterMap() );

    OpenSearchHelper.writeResponse( doc, response, "application/rss+xml" );
  }

  public QueryParameters getQueryParameters( HttpServletRequest request )
  {
    QueryParameters p = new QueryParameters( );
    
    p.query      = ServletHelper.getParam( request, "q", "" );
    p.start      = ServletHelper.getParam( request, "p", 0 );
    p.hitsPerPage= ServletHelper.getParam( request, "n", this.hitsPerPage );
    p.hitsPerSite= ServletHelper.getParam( request, "h", this.hitsPerSite );
    p.sites      = ServletHelper.getParam( request, "s", QueryParameters.EMPTY_STRINGS );
    p.indexNames = ServletHelper.getParam( request, "i", QueryParameters.ALL_INDEXES );
    p.excludes   = ServletHelper.getParam( request, "x", QueryParameters.EMPTY_STRINGS );
    p.collections= ServletHelper.getParam( request, "c", QueryParameters.EMPTY_STRINGS );
    p.types      = ServletHelper.getParam( request, "t", QueryParameters.EMPTY_STRINGS );
    p.dates      = ServletHelper.getParam( request, "d", QueryParameters.EMPTY_STRINGS );

    // If the request is for too many hits per page, limit it.
    if ( p.hitsPerPage > this.hitsPerPageMax )
      {
        p.hitsPerPage = this.hitsPerPageMax;
      }

    // If the resquest start position is beyond the limit, roll it
    // back.
    if ( p.start > ( this.positionMax - p.hitsPerPage ) )
      {
        p.start = this.positionMax - p.hitsPerPage;
      }

    return p;
  }

}
