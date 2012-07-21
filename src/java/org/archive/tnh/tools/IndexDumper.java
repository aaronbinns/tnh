/*
 * Copyright (C) 2011 Internet Archive.
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
import java.util.*;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.ArchiveParallelReader;
import org.apache.lucene.store.MMapDirectory;


public class IndexDumper
{
  public enum Action { DUMP, LIST, COUNT };
  
  public static void main( String[] args ) throws Exception
  {
    if ( args.length < 1 )
      {
        usageAndExit( );
      }
    
    boolean includeDocIds = false;
    Action action = Action.DUMP;
    List<String> fields = new ArrayList<String>( );
    List<String> dirs   = new ArrayList<String>( );
    int i = 0;
    for ( ; i < args.length; i++ )
      {
             if ( args[i].equals( "-l" ) ) action = Action.LIST;
        else if ( args[i].equals( "-c" ) ) action = Action.COUNT;
        else if ( args[i].equals( "-d" ) ) includeDocIds = true;
        else if ( args[i].equals( "-f" ) )
          {
            action = Action.DUMP;
            if ( args.length - i < 2 ) 
              {
                System.out.println( "Error: missing argument to -f\n" );
                usageAndExit( );
              }
            String fieldList = args[++i];
            fields.addAll( Arrays.asList( fieldList.split(",") ) );
          }
        else
          {
            break;
          }
      }

    ArchiveParallelReader reader = new ArchiveParallelReader( );
    for ( ; i < args.length ; i++ )
      {
        reader.add( IndexReader.open( new MMapDirectory( new File( args[i] ) ) ) );
      }

    switch ( action )
      {
      case LIST:
        listFields( reader );
        break;

      case COUNT:
        countDocs( reader );
        break;

      case DUMP:
        dumpIndex( reader, fields, includeDocIds );
        break;
      }
  }
  
  private static void dumpIndex( IndexReader reader, List<String> fields, boolean includeDocIds ) throws Exception
  {
    Collection fieldNames = reader.getFieldNames(IndexReader.FieldOption.ALL);

    // If no fields were specified, then dump them all.
    if ( fields.size() == 0 )
      {
        fields.addAll( fieldNames );
      }
    else
      {
        for ( String field : fields )
          {
            if ( ! fieldNames.contains( field ) )
              {
                System.out.println( "Field not in index: " + field);
                System.exit( 2 );
              }
          }
      }

    int numDocs = reader.numDocs();
    
    for ( int i = 0; i < numDocs; i++ )
    {
      if ( includeDocIds )
        {
          System.out.print( i + "\t" );
        }

      for ( String field : fields )
        {
          System.out.print( Arrays.toString( reader.document(i).getValues( field ) ) );
          System.out.print( "\t" );
        }

      System.out.println();
    }
    
  }

  /*
  private static void dumpIndex( IndexReader reader ) throws Exception
  {
    Object[] fieldNames = reader.getFieldNames(IndexReader.FieldOption.ALL).toArray( );
    Arrays.sort( fieldNames );

    for ( int i = 0; i < fieldNames.length; i++ )
    {
      System.out.print( fieldNames[i] + "\t" );
    }

    System.out.println();

    int numDocs = reader.numDocs();
    
    for (int i = 0; i < numDocs; i++)
    {
      for (int j = 0; j < fieldNames.length; j++)
      {
        System.out.print( Arrays.toString( reader.document(i).getValues((String) fieldNames[j])) + "\t" );
      }
      
      System.out.println();
    }
  }
  */
  
  private static void listFields( IndexReader reader ) throws Exception
  {
    Object[] fieldNames = reader.getFieldNames(IndexReader.FieldOption.ALL).toArray( );
    Arrays.sort( fieldNames );

    for ( int i = 0; i < fieldNames.length; i++ )
    {
      System.out.println( fieldNames[i] );
    }
  }
  
  private static void countDocs( IndexReader reader ) throws Exception
  {
    System.out.println( reader.numDocs( ) );
  }
  
  private static void usageAndExit()
  {
    System.out.println( "Usage: IndexDumper [option] index1 ... indexN" );
    System.out.println( "Options:" );
    System.out.println( "  -c            Emit document count" );
    System.out.println( "  -d            Include document numbers in output" );
    System.out.println( "  -f <fields>   List of fields to dump (comma-separated)" );
    System.out.println( "  -l            List fields in index" );
    System.exit(1);
  }
}
