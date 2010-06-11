/*
 * Copyright (C) 2008 Internet Archive.
 * 
 * This file is part of the archive-access tools project
 * (http://sourceforge.net/projects/archive-access).
 * 
 * The archive-access tools are free software; you can redistribute them and/or
 * modify them under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or any
 * later version.
 * 
 * The archive-access tools are distributed in the hope that they will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License along with
 * the archive-access tools; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

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
