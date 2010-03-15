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

import java.io.IOException;
import java.io.File;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.NIOFSDirectory;



public class TermDumper
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 2 )
      {
        System.err.println( "TermDumper field <index...>" );
        System.exit( 1 );
      }
    
    String field = args[0];

    java.util.ArrayList<IndexReader> readers = new java.util.ArrayList<IndexReader>( args.length - 1);
    for ( int i = 1 ; i < args.length ; i++ )
      {
        String arg = args[i];
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

    for ( IndexReader reader : readers )
      {
        TermDocs termDocs = reader.termDocs();
        TermEnum termEnum = reader.terms (new Term (field));

        try
          {
            do 
              {
                Term term = termEnum.term();

                if ( term==null || ! field.equals( term.field() ) ) break;

                String termval = term.text();

                termDocs.seek( termEnum );

                System.out.println( termval );
              }
            while (termEnum.next());
          }
        finally
          {
            termDocs.close();
            termEnum.close();
          }

      }

    
  }

}