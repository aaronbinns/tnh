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

import java.io.*;

import org.apache.lucene.index.*;
import org.apache.lucene.document.*;
import org.apache.lucene.store.MMapDirectory;



public class TermDumper
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 2 )
      {
        System.err.println( "TermDumper [-c|-v value] field <index...>" );
        System.exit( 1 );
      }
    
    boolean count = false;
    String  value = null;
    boolean all   = false;

    int i = 0;
    for ( ; i < args.length ; i++ )
      {
        String arg = args[i];
        
        if ( "-h".equals( arg ) || "--help".equals( arg ) )
          {
            System.err.println( "TermDumper [-c|-v value] field <index...>" );
            System.exit( 1 );
          }
        else if ( "-c".equals( arg ) || "--count".equals( arg ) )
          {
            count = true;
          }
        else if ( "-v".equals( arg ) || "--vaue".equals( arg ) )
          {
            value = args[++i];
          }
        else if ( "-a".equals( arg ) || "--all".equals( arg ) )
          {
            all = true;
          }
        else
          {
            break; 
          }
      }

    String field = args[i++];

    java.util.ArrayList<IndexReader> readers = new java.util.ArrayList<IndexReader>( args.length - 1);
    for ( ; i < args.length ; i++ )
      {
        String arg = args[i];
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

                
                if ( value == null )
                  {
                    if ( count ) 
                      {
                        termDocs.seek( termEnum );
                        
                        int c = 0;
                        for ( ; termDocs.next() ; c++ ) ;
                        
                        System.out.print( c + " " );
                      }
                    System.out.println( term.text() );
                  }
                else if ( value.equals( term.text( ) ) )
                  {
                    termDocs.seek( termEnum );
                    
                    while ( termDocs.next( ) )
                      {
                        if ( all )
                          {
                            Document d = reader.document( termDocs.doc() );
                            System.out.println( termDocs.doc( ) );
                            for ( Object o : d.getFields() )
                              {
                                Field f = (Field) o;
                                System.out.println( f.name( ) + " " + d.get( f.name( ) ) );
                              }
                          }
                        else
                          {
                            System.out.println( termDocs.doc() + " " + reader.document( termDocs.doc() ).get( "url" ) );
                          }
                      }
                  }
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