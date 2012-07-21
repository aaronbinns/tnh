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

package org.archive.tnh.tools;

import java.io.IOException;
import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.MMapDirectory;



public class TermDeleter
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 3 )
      {
        System.err.println( "TermValueDocumentDeleter field term <index...>" );
        System.exit( 1 );
      }

    String field = args[0];
    String value = args[1];

    java.util.ArrayList<IndexReader> readers = new java.util.ArrayList<IndexReader>( args.length - 2);
    for ( int i = 2 ; i < args.length ; i++ )
      {
        String arg = args[i];
        try
          {
            IndexReader reader = IndexReader.open( new MMapDirectory( new File( arg ) ), false );

            readers.add( reader );
          }
        catch ( IOException ioe )
          {
            System.err.println( "Error reading: " + arg );
          }
      }

    for ( IndexReader reader : readers )
      {
        Term t = new Term( field, value );

        int n = reader.deleteDocuments( t );
        
        System.out.println( "Deleted: " + n );

        try
          {
            reader.close( );
          }
        catch ( IOException ioe )
          {
            System.out.println( "Error closing the reader: " + ioe );
          }
      }    
  }

}