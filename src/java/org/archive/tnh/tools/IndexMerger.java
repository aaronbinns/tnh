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

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;
import org.apache.lucene.util.*;

/**
 * Handy command-line tool for merging and/or optimizing Lucene
 * indexes.  The default behavior matches the common
 * merge/optimization policies at IA; that is, indexes are merged
 * offline, in one shot.
 */
public class IndexMerger
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 2 )
      {
        System.err.println( "IndexMerger [-v|-o|-f] <dest> <source>..." );
        System.exit( 1 );
      }

    boolean verbose  = false;
    boolean optimize = false;
    boolean force    = false;

    int i = 0;
    for ( ; i < args.length ; i++ )
      {
        if ( "-o".equals( args[i] ) )
          {
            optimize = true;
          }
        else if ( "-f".equals( args[i] ) )
          {
            force = true;
          }
        else if ( "-v".equals( args[i] ) )
          {
            verbose = true;
          }
        else
          {
            break ;
          }
      }
    
    if ( (args.length - i) < (2 - (optimize ? 1 : 0)) )
      {
        System.err.println( "Erorr: no source files!" );
        System.err.println( "IndexMerger [-v|-o|-f] <dest> <source>..." );
        System.exit( 1 );
      }

    File dest = new File( args[i++] );

    if ( ! force && dest.exists( ) )
      {
        System.err.println( "Destination exits, use -f to force merging into existing index: " + dest );

        System.exit( 2 );
      }
    
    IndexReader ir[] = new IndexReader[args.length-i];
    for ( int j = i ; j < args.length ; j++ )
      {
        ir[j-i] = IndexReader.open( new MMapDirectory( new File( args[j] ) ), true /* read-only */ );
      }

    IndexWriter w = null;
    try
      {
        // Configure the IndexWriter.
        IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_35, null );

        config.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );

        w = new IndexWriter( new MMapDirectory( dest ), config );
        
        if ( verbose )
          {
            w.setInfoStream( System.out );
          }

        if ( ir.length > 0 )
          {
            w.addIndexes( ir );
          }

        if ( optimize )
          {
            w.optimize();
          }
        w.commit( );
        w.close( );
      }
    catch ( IOException ioe )
      {
        System.err.println( "Error: " + args[0] + " " + ioe );

        if ( w != null ) w.close();
      }    
  }
}
