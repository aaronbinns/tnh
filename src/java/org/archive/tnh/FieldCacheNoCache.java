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
import java.io.IOException;
import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.document.Document;

public class FieldCacheNoCache implements FieldCache
{
  private String fieldName;

  private FieldSelector FIELD_ONLY = new FieldSelector( )
    {
      public static final long serialVersionUID = 0L;

      public FieldSelectorResult accept( String fieldName )
      {
        if ( FieldCacheNoCache.this.fieldName.equals( fieldName ) )
          {
            return FieldSelectorResult.LOAD_AND_BREAK;
          }
        return FieldSelectorResult.NO_LOAD;
      }
    };

  public FieldCacheNoCache( String fieldName )
  {
    this.fieldName = fieldName;
  }

  /**
   *
   */
  public String getValue( IndexReader reader, int docBase, int docId )
    throws IOException
  {
    Document doc = reader.document( docId, FIELD_ONLY );

    String value = doc.get( this.fieldName );

    return value;
  }


}
