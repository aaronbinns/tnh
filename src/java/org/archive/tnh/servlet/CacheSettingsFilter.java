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
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Simple servlet filter that enforces cache settings on the HTTP
 * response passed through it.
 */
public class CacheSettingsFilter implements Filter
{
  private String maxAge;

  public void init( FilterConfig config )
    throws ServletException
  {
    this.maxAge = "max-age=" + ServletHelper.getInitParameter( config, "max-age", false );
  }

  public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
    throws IOException, ServletException 
  {
    HttpServletResponse res = (HttpServletResponse) response;
    
    res.setDateHeader( "Date", System.currentTimeMillis( ) );

    if ( this.maxAge != null )
      {
        res.addHeader( "Cache-Control", this.maxAge );
      }

    chain.doFilter( request, res );
  }

  public void destroy()
  {

  }

}
