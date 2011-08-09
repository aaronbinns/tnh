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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;
import java.io.IOException;
import java.io.File;

import org.apache.lucene.index.IndexReader;

/**
 * This is a specialized implementation of FieldCacheNoCache that
 * extracts the site from the URL.
 *
 * In many/most of the indexes we build, we don't store the site, but
 * we do store the URL.  So, if we need the site from stored fields,
 * then we get it from the URL.
 *
 * Even though in practice we always store the URL in a field named
 * "url", a different name could be used by passing it to the
 * constructor.
 */
public class UrlSiteCacheNoCache extends FieldCacheNoCache
{

  public UrlSiteCacheNoCache( )
  {
    // Assume we want the "url" field.
    super( "url" );
  }

  public UrlSiteCacheNoCache( String fieldName )
  {
    super( fieldName );
  }

  /**
   * 
   */
  public String getValue( IndexReader reader, int docBase, int docId )
    throws IOException
  {
    String value = super.getValue( reader, docBase, docId );

    try
      {
        String site = new URI( value ).getHost( );

        return site;
      }
    catch ( Exception e ) { return ""; }
  }

}
