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
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import org.archive.tnh.*;

public class QueryParametersFilter implements Filter
{
  public static final Logger LOG = Logger.getLogger( QueryParametersFilter.class.getName() );

  public int hitsPerPage;
  public int hitsPerSite;

  public void init( FilterConfig config )
    throws ServletException
  {
    this.hitsPerPage = ServletHelper.getInitParameter( config, "hitsPerPage", 10, 1 );
    this.hitsPerSite = ServletHelper.getInitParameter( config, "hitsPerSite", 1,  0 );
  }

  public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
    throws IOException, ServletException 
  {
    QueryParameters p = new QueryParameters( );
    
    p.query      = ServletHelper.getParam( request, "q", "" );
    p.start      = ServletHelper.getParam( request, "p", 0 );
    p.hitsPerPage= ServletHelper.getParam( request, "n", this.hitsPerPage );
    p.hitsPerSite= ServletHelper.getParam( request, "h", this.hitsPerSite );
    p.sites      = ServletHelper.getParam( request, "s", QueryParameters.EMPTY_STRINGS );
    p.indexNames = ServletHelper.getParam( request, "i", QueryParameters.ALL_INDEXES   );
    p.excludes   = ServletHelper.getParam( request, "x", QueryParameters.EMPTY_STRINGS );
    p.collections= ServletHelper.getParam( request, "c", QueryParameters.EMPTY_STRINGS );
    p.types      = ServletHelper.getParam( request, "t", QueryParameters.EMPTY_STRINGS );
    
    request.setAttribute( OpenSearchHelper.PARAMS_KEY, p );

    chain.doFilter( request, response );
  }

  public void destroy()
  {

  }

}
