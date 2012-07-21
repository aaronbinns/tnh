/**
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.MMapDirectory;


public class FieldCachePreAllocated implements FieldCache
{
  final String   fieldName;
  final String[] fieldValueByDocId;

  // For sanity checking.
  int expectedNumberOfReaders;
  Map<IndexReader,Integer> readerDocBases;

  public FieldCachePreAllocated( String fieldName, int expectedNumberOfReaders, int totalNumberOfDocuments )
  {
    //this.fieldName = fieldName.intern();
    this.fieldName   = fieldName;
    this.expectedNumberOfReaders = expectedNumberOfReaders;

    // This is the big memory alloc as totalNumberOfDocuments can be > 250 million.
    this.fieldValueByDocId  = new String[totalNumberOfDocuments];

    this.readerDocBases = Collections.synchronizedMap( new HashMap<IndexReader,Integer>( expectedNumberOfReaders ) );
  }

  public String[] getFieldCache( IndexReader reader, int docBase )
    throws IOException
  {
    if ( this.readerDocBases.size() > this.expectedNumberOfReaders )
      {
        // TODO: Sanity check.  More IndexReaders than we expect.
      }

    synchronized ( this )
      {
        Integer expectedDocBase = this.readerDocBases.get( reader );
        if ( expectedDocBase == null )
          {
            // Load the terms and fill the array.
            TermDocs termDocs = reader.termDocs();
            TermEnum termEnum = reader.terms( new Term(this.fieldName) );
            try
              {
                do 
                  {
                    Term term = termEnum.term();
                    if ( term==null || !this.fieldName.equals( term.field() ) ) break;
                    //String termval = term.text().intern();
                    String termval = term.text();
                    termDocs.seek( termEnum );
                    while ( termDocs.next() )
                      {
                        this.fieldValueByDocId[termDocs.doc() + docBase] = termval;
                      }
                  }
                while (termEnum.next());
              }
            finally
              {
                termDocs.close();
                termEnum.close();
              }
            
            expectedDocBase = docBase;
            
            this.readerDocBases.put( reader, docBase );
          }

        if ( expectedDocBase != docBase )
          {
            // TODO: Sanity check.  The docBase has changed.
          }
      }
    
    return this.fieldValueByDocId;
  }

  public String getValue( IndexReader reader, int docBase, int docId )
    throws IOException
  {
    String[] cache = this.getFieldCache( reader, docBase );

    return cache[docBase+docId];
  }

  public static void main( String args[] )
    throws Exception
  {
    if ( args.length == 0 )
      {
        System.err.println( "FieldCachePreAllocated: <index...>" );
        System.exit( 1 );
      }

    java.util.ArrayList<IndexReader> readers = new java.util.ArrayList<IndexReader>( args.length );
    int totalNumDocuments = 0;
    for ( String arg : args )
      {
        try
          {
            IndexReader reader = IndexReader.open( new MMapDirectory( new File( arg ) ), true );

            totalNumDocuments += reader.numDocs( );

            readers.add( reader );
          }
        catch ( IOException ioe )
          {
            System.err.println( "Error reading: " + arg );
          }
      }

    FieldCachePreAllocated siteCache = new FieldCachePreAllocated( "site", readers.size(), totalNumDocuments );

    int docBase = 0;
    for ( IndexReader reader : readers )
      {
        siteCache.getFieldCache( reader, docBase );

        docBase += reader.numDocs( );
      }

    for ( Map.Entry<IndexReader,Integer> e : siteCache.readerDocBases.entrySet( ) )
      {
        IndexReader reader  = e.getKey( );
                    docBase = e.getValue( );
        int         numDocs = reader.numDocs( );

        System.out.println( "Index: " + reader );
        System.out.println( "  numDocs: " + numDocs );
        System.out.println( "  docBase: " + docBase );
        
        String[] sitePerDoc = siteCache.getFieldCache( reader, docBase );
        for ( int i = 0; i < numDocs ; i++ )
          {
            System.out.println( "  doc[" + i + "]: " + sitePerDoc[i+docBase] );
          }
      }
  }
 
}
