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
import javax.servlet.*;
import javax.servlet.http.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;


public class XSLTFilter implements Filter
{
  private String xsltUrl;
  private String contentType;

  public void init( FilterConfig config )
    throws ServletException
  {
    this.xsltUrl = config.getInitParameter( "xsltUrl" );

    if ( this.xsltUrl != null )
      {
        this.xsltUrl = this.xsltUrl.trim( );
        
        if ( this.xsltUrl.length( ) == 0 )
          {
            this.xsltUrl = null;
          }
      }

    this.contentType = config.getInitParameter( "contentType" );

    if ( this.contentType != null )
      {
        this.contentType = this.contentType.trim( );
        
        if ( this.contentType.length( ) == 0 )
          {
            this.contentType = null;
          }
      }

    if ( this.contentType == null )
      {
        this.contentType = "application/xml";
      }
  }

  public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
    throws IOException, ServletException 
  {
    if ( this.xsltUrl != null )
      {
        ByteArrayOutputStream baos = new ByteArrayOutputStream( 8 * 1024 );

        HttpServletResponseInterceptor capturedResponse = new HttpServletResponseInterceptor( (HttpServletResponse) response, baos );
        
        chain.doFilter( request, capturedResponse );
        
        byte output[] = baos.toByteArray( );
        
        try
          {
            Source      xsltSource    = new StreamSource( xsltUrl );
            Templates   xsltTemplates = TransformerFactory.newInstance( ).newTemplates( xsltSource );
            Transformer transformer   = xsltTemplates.newTransformer( );
            
            StreamSource source = new StreamSource( new ByteArrayInputStream( output ) );
            StreamResult result = new StreamResult( response.getOutputStream( ) );
            
            // Enforce XML content-type in the response.
            response.setContentType( this.contentType );
            
            transformer.transform( source, result );
          }
        catch ( javax.xml.transform.TransformerConfigurationException tce )
          {
            // TODO: Re-throw, or log it and eat it?
          }
        catch( javax.xml.transform.TransformerException te )
          {
            // TODO: Re-throw, or log it and eat it?
          }
      }
    else
      {
        chain.doFilter( request, response );
      }
  }

  public void destroy()
  {

  }

}


class HttpServletResponseInterceptor extends HttpServletResponseWrapper
{
  private OutputStream os;

  HttpServletResponseInterceptor( HttpServletResponse response, OutputStream os )
  {
    super( response );
    
    this.os = os;
  }

  public ServletOutputStream getOutputStream() 
  {
    ServletOutputStream sos = new ServletOutputStream( )
      {
        public void write( int b )
          throws java.io.IOException
        {
          HttpServletResponseInterceptor.this.os.write( b );
        }
      };
    
    return sos;
  }

  public PrintWriter getWriter( )
  {
    PrintWriter pw = new PrintWriter( this.os );

    return pw;
  }

}