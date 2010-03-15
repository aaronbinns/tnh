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
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;


public class ParametersProcessorFilter implements Filter
{
  public static final Logger LOG = Logger.getLogger( ParametersProcessorFilter.class.getName() );

  public static final String[] EMPTY_STRINGS = { };

  public int hitsPerPage;
  public int hitsPerSite;

  public void init( FilterConfig config )
    throws ServletException
  {
    this.hitsPerPage = getInitParamInteger( config, "hitsPerPage", 10, 1 );
    this.hitsPerSite = getInitParamInteger( config, "hitsPerSite", 1,  0 );
  }

  public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
    throws IOException, ServletException 
  {
    OpenSearchBase.Parameters p = new OpenSearchBase.Parameters( );
    
    p.query      = getParam( request, "q", "" );
    p.start      = getParam( request, "p", 0 );
    p.hitsPerPage= getParam( request, "n", this.hitsPerPage );
    p.hitsPerSite= getParam( request, "h", this.hitsPerSite );
    p.sites      = getParam( request, "s", OpenSearchBase.EMPTY_STRINGS );
    p.indexNames = getParam( request, "i", OpenSearchBase.ALL_INDEXES );
    p.collections= getParam( request, "c", OpenSearchBase.EMPTY_STRINGS );
    p.types      = getParam( request, "t", OpenSearchBase.EMPTY_STRINGS );
    
    request.setAttribute( "opensearch.parameters", p );

    chain.doFilter( request, response );
  }

  public void destroy()
  {

  }

  public int getInitParamInteger( FilterConfig config, String name, int defaultValue, int minValue )
    throws ServletException
  {
    String s = config.getInitParameter( name );
    s = s == null ? "" : s.trim( );
    if ( s.length() != 0 )
      {
        try
          {
            int i = Integer.parseInt( s );

            if ( i < minValue )
              {
                throw new ServletException( "Error: '" + name + "' must be >= " + minValue + ", specified value: " + i );
              }

            LOG.info( name + ": using configuration value: " + i  );

            return i;
          }
        catch ( NumberFormatException nfe )
          {
            throw new ServletException( "Error: bad value for '" + name +"' in servlet config: " + s ); 
          }
      }

    LOG.info( name + ": using built-in default value: " + defaultValue );
    
    return defaultValue;
  }

  public String getParam( ServletRequest request, String name, String defaultValue )
  {
    String v = request.getParameter( name );
    
    v = v == null ? defaultValue : v.trim();
    
    return v;
  }
  
  public int getParam( ServletRequest request, String name, int defaultValue )
  {
    String v = request.getParameter( name );
    
    v = v == null ? "" : v.trim();
    
    if ( v.length( ) == 0 ) return defaultValue;
    
    try
      {
        int i = Integer.parseInt( v );
        
        if ( i < 0 ) return defaultValue;
        
        return i;
      }
    catch ( NumberFormatException nfe )
      {
        return defaultValue;
      }
  }
  
  public String[] getParam( ServletRequest request, String name, String[] defaultValue )
  {
    String[] v = request.getParameterValues( name );
    
    v = v == null ? defaultValue : v;
    
    return v;
  } 

}
