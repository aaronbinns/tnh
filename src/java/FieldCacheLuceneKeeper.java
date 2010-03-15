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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.NIOFSDirectory;


public class FieldCacheLuceneKeeper implements FieldCache
{
  String fieldName;
  Map<IndexReader,String[]> perReaderCache;

  public FieldCacheLuceneKeeper( String fieldName )
  {
    this.fieldName = fieldName;
    this.perReaderCache = new HashMap<IndexReader,String[]>( );
  }

  /**
   * 
   */
  public String[] getFieldCache( IndexReader reader, int docBase )
    throws IOException
  {
    synchronized ( this )
      {
        String[] cache = this.perReaderCache.get( reader );
        
        if ( cache == null )
          {
            cache = org.apache.lucene.search.FieldCache.DEFAULT.getStrings( reader, this.fieldName );
            
            this.perReaderCache.put( reader, cache );
          }
        
        return cache;
      }
  }


  public String getValue( IndexReader reader, int docBase, int docId )
    throws IOException
  {
    String[] cache = this.getFieldCache( reader, docBase );

    return cache[docId];
  }


  public static void main( String args[] )
    throws Exception
  {
    if ( args.length == 0 )
      {
        System.err.println( "FieldCacheLuceneKeeper: <index...>" );
        System.exit( 1 );
      }

    java.util.ArrayList<IndexReader> readers = new java.util.ArrayList<IndexReader>( args.length );
    for ( String arg : args )
      {
        try
          {
            IndexReader reader = IndexReader.open( new NIOFSDirectory( new File( arg ) ), true );
            
            readers.add( reader );
          }
        catch ( IOException ioe )
          {
            System.err.println( "Error reading: " + arg );
          }
      }

    FieldCacheLuceneKeeper cache = new FieldCacheLuceneKeeper( "site" );

    for ( IndexReader reader : readers )
      {
        String[] sitePerDoc = cache.getFieldCache( reader, -1 );

        System.out.println( "Index: " + reader );
        System.out.println( "  numDocs: " + reader.numDocs( ) );
        System.out.println( "  docBase: -1" );

        for ( int i = 0; i < sitePerDoc.length ; i++ )
          {
            System.out.println( "  doc[" + i + "]: " + sitePerDoc[i] );
          }

      }

  }
 
}
