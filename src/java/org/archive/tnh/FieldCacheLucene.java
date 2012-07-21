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
import org.apache.lucene.store.MMapDirectory;


public class FieldCacheLucene implements FieldCache
{
  String fieldName;

  public FieldCacheLucene( String fieldName )
  {
    this.fieldName = fieldName;
  }

  /**
   *
   */
  public String getValue( IndexReader reader, int docBase, int docId )
    throws IOException
  {
    String[] cache = org.apache.lucene.search.FieldCache.DEFAULT.getStrings( reader, this.fieldName );

    return cache[docId];
  }


  public static void main( String args[] )
    throws Exception
  {
    if ( args.length == 0 )
      {
        System.err.println( "FieldCacheLucene: <index...>" );
        System.exit( 1 );
      }

    java.util.ArrayList<IndexReader> readers = new java.util.ArrayList<IndexReader>( args.length );
    for ( String arg : args )
      {
        try
          {
            IndexReader reader = IndexReader.open( new MMapDirectory( new File( arg ) ), true );
            
            readers.add( reader );
          }
        catch ( IOException ioe )
          {
            System.err.println( "Error reading: " + arg );
          }
      }

    FieldCacheLucene cache = new FieldCacheLucene( "site" );

    for ( IndexReader reader : readers )
      {
        int numDocs = reader.numDocs( );

        System.out.println( "Index: " + reader );
        System.out.println( "  numDocs: " + reader.numDocs( ) );
        System.out.println( "  docBase: -1" );

        for ( int i = 0; i < numDocs ; i++ )
          {
            System.out.println( "  doc[" + i + "]: " + cache.getValue( reader, -1, i ) );
          }

      }

    
  }
 
}
