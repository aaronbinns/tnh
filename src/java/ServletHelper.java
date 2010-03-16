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
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;


public class ServletHelper
{
  public static final Logger LOG = Logger.getLogger( ServletHelper.class.getName() );

  public static final String PARAMS_KEY = "opensearch.parameters";
  
  /**
   * Convenience function to parse a given value as an integer.
   * If it cannot be parsed, the default value is used.
   * If it can be parsed and is not greater than the minValue, a ServletException is thrown.
   */
  public static int getInitParameter( String value, String name, int defaultValue, int minValue )
    throws ServletException
  {
    value = value == null ? "" : value.trim( );

    if ( value.length() != 0 )
      {
        try
          {
            int v = Integer.parseInt( value );

            if ( v < minValue )
              {
                throw new ServletException( "Error: '" + name + "' must be >= " + minValue + ", specified value: " + v );
              }

            LOG.info( name + ": using configuration value: " + v  );

            return v;
          }
        catch ( NumberFormatException nfe )
          {
            throw new ServletException( "Error: bad value for '" + name +"' in servlet config: " + value ); 
          }
      }

    LOG.info( name + ": using default value: " + defaultValue );
    
    return defaultValue;
  }

  /**
   * Convenience function to parse a configuration parameter value as an integer.
   * If it cannot be parsed, the default value is used.
   * If it can be parsed and is not greater than the minValue, a ServletException is thrown.
   */
  public static int getInitParameter( ServletConfig config, String name, int defaultValue, int minValue )
    throws ServletException
  {
    return getInitParameter( config.getInitParameter( name ), name, defaultValue, minValue );
  }

  /**
   * Convenience function to parse a configuration parameter value as an integer.
   * If it cannot be parsed, the default value is used.
   * If it can be parsed and is not greater than the minValue, a ServletException is thrown.
   */
  public static int getInitParameter( FilterConfig config, String name, int defaultValue, int minValue )
    throws ServletException
  {
    return getInitParameter( config.getInitParameter( name ), name, defaultValue, minValue );
  }

  /**
   * Convenience function to check that a given value is non-empty.  If empty
   * values are prohibited a ServletException is thrown.
   */
  public static String getInitParameter( String value, String name, boolean allowEmpty )
    throws ServletException
  {
    value = value == null ? "" : value;

    if ( value.length() == 0 && ! allowEmpty )
      {
        throw new ServletException( "Error: '" + name + "' must have a non-empty value" );
      }

    return value;
  }

  /**
   * Convenience function to check a configuration parameter value is
   * non-empty.  If empty values are prohibited, a ServletException is
   * thrown.
   */
  public static String getInitParameter( ServletConfig config, String name, boolean allowEmpty )
    throws ServletException
  {
    return getInitParameter( config.getInitParameter( name ), name, allowEmpty );
  }


  /**
   * Convenience function to check a configuration parameter value is
   * non-empty.  If empty values are prohibited, a ServletException is
   * thrown.
   */
  public static String getInitParameter( FilterConfig config, String name, boolean allowEmpty )
    throws ServletException
  {
    return getInitParameter( config.getInitParameter( name ), name, allowEmpty );
  }


  /**
   * Convenience function to get a URL parameter String value,
   * returning the default if none found.  The empty string is
   * returned if found, not the default.
   */
  public static String getParam( ServletRequest request, String name, String defaultValue )
  {
    String v = request.getParameter( name );
    
    v = v == null ? defaultValue : v.trim();
    
    return v;
  }
  
  /**
   * Convenience function to get a URL parameter integer value,
   * returning the default if none found, or cannot be parsed.
   */
  public static int getParam( ServletRequest request, String name, int defaultValue )
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
  
  /**
   * Convenience function to get all the URL parameter values as a
   * String[].  If no values found (i.e. null), then the default value
   * is returned.
   */
  public static String[] getParam( ServletRequest request, String name, String[] defaultValue )
  {
    String[] v = request.getParameterValues( name );
    
    v = v == null ? defaultValue : v;
    
    return v;
  } 

}
