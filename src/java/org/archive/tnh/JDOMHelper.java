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

import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;


public class JDOMHelper
{
  public static final Logger LOG = Logger.getLogger( JDOMHelper.class.getName() );

  public static final String PARAMS_KEY = "opensearch.parameters";
  
  public static final String NS_OPENSEARCH = "http://a9.com/-/spec/opensearchrss/1.0/";
  public static final String NS_ARCHIVE    = "http://web.archive.org/-/spec/opensearchrss/1.0/";

  /** 
   * Convenience function to create a new JDOM Element and add it to the parent.
   */
  public static Element add( Element parent, String name )
  {
    Element child = new Element( name );
    parent.addContent( child );

    return child;
  }

  /** 
   * Convenience function to create a new JDOM Element and add it to the parent.
   */
  public static Element add( Element parent, String name, String value )
  {
    Element child = new Element( name );

    child.addContent( getLegalXml( value ) );

    parent.addContent( child );

    return child;
  }

  /** 
   * Convenience function to create a new JDOM Element and add it to the parent.
   */
  public static Element add( Element parent, String namespaceUri, String name, String value )
  {
    Element child = new Element( name, Namespace.getNamespace( namespaceUri ) );
    child.addContent( getLegalXml( value ) );

    parent.addContent( child );

    return child;
  }

  /** 
   * Convenience function to create a new JDOM Element and add it to the parent.
   */
  public static Element add( Element parent, Namespace ns, String name, String value )
  {
    Element child = new Element( name, ns );
    child.addContent( getLegalXml( value ) );

    parent.addContent( child );

    return child;
  }

  /*
   * Ensure string is legal xml.
   * @param text String to verify.
   * @return Passed <code>text</code> or a new string with illegal
   * characters removed if any found in <code>text</code>.
   * @see http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char
   */
  public static String getLegalXml( final String text ) 
  {
    if ( text == null ) return "";

    StringBuilder buffer = null;
    for (int i = 0; i < text.length(); i++) 
      {
        char c = text.charAt(i);
        if ( !isLegalXml(c) ) 
          {
            if (buffer == null)
              {
                // Start up a buffer.  Copy characters here from now on
                // now we've found at least one bad character in original.
                buffer = new StringBuilder(text.length());
                buffer.append(text.substring(0, i));
              }
          }
        else
          {
            if (buffer != null)
              {
                buffer.append(c);
              }
          }
      }
    return (buffer != null)? buffer.toString(): text;
  }
 
  public static boolean isLegalXml(final char c)
  {
    return c == 0x9 || c == 0xa || c == 0xd || (c >= 0x20 && c <= 0xd7ff)
      || (c >= 0xe000 && c <= 0xfffd) || (c >= 0x10000 && c <= 0x10ffff);
  }

}
