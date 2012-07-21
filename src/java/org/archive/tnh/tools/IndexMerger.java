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
 * offline, in one shot.  The termIndexInterval is as in the
 * IndexWriterConfig, unless over-ridden via command-line option.
 */
public class IndexMerger
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 2 )
      {
        System.err.println( "IndexMerger [-v|-o|-f|-b <size>|-i <value>] <dest> <source>..." );
        System.exit( 1 );
      }

    boolean verbose  = false;
    boolean optimize = false;
    boolean force    = false;

    // Default size of RAM buffer (in MB) for merging indexes is max
    // memory - 12MB.  The Lucene library aborts if the value is not
    // "comfortably below 2GB" (whatever that means), so we employ a
    // limit of 1800MB to prevent the Lucene library from aborting.
    double bufsize = Math.min( (Runtime.getRuntime().maxMemory() / 1024 / 1024) - 12, 1800 );

    int termIndexInterval = IndexWriterConfig.DEFAULT_TERM_INDEX_INTERVAL;

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
        else if ( "-b".equals( args[i] ) )
          {
            if ( ++i >= args.length )
              {
                System.err.println( "Missing ram buffer size, after -b" );
                System.exit(1);
              }
            try
              {
                bufsize = Double.parseDouble( args[i] );
              }
            catch ( NumberFormatException nfe )
              {
                System.err.println( "Invalid parameter, after -b: " + args[i] );
                System.exit(1);
              }
          }
        else if ( "-i".equals( args[i] ) )
          {
            if ( ++i >= args.length )
              {
                System.err.println( "Missing term index interval value, after -i" );
                System.exit(1);
              }
            try
              {
                termIndexInterval = Integer.parseInt( args[i] );
              }
            catch ( NumberFormatException nfe )
              {
                System.err.println( "Invalid parameter, after -i: " + args[i] );
                System.exit(1);
              }
          }
        else
          {
            break ;
          }
      }
    
    if ( (args.length - i) < (2 - (optimize ? 1 : 0)) )
      {
        System.err.println( "Erorr: no source files!" );
        System.err.println( "IndexMerger [-v|-o|-f|-b <size>|-i <value>] <dest> <source>..." );
        System.exit( 1 );
      }

    File dest = new File( args[i++] );

    if ( ! force && dest.exists( ) )
      {
        System.err.println( "Destination exits, use -f to force merging into existing index: " + dest );

        System.exit( 2 );
      }
    
    Directory d[] = new Directory[args.length-i];
    for ( int j = i ; j < args.length ; j++ )
      {
        d[j-i] = new MMapDirectory( new File( args[j] ) );
      }

    IndexWriter w = null;
    try
      {
        // Allow for all segments to be merged at once by setting the
        // max suitably high.  Don't use Integer.MAX_VALUE because the
        // TieredMergePolicy code does arithematic with this value and
        // will overflow if set to Integer.MAX_VALUE.
        TieredMergePolicy mergePolicy = new TieredMergePolicy();
        mergePolicy.setMaxMergeAtOnceExplicit( 1000000 );
        mergePolicy.setUseCompoundFile( false );

        // Configure the IndexWriter.  Use SerialMergeScheduler so
        // that the merge/optimize is done all at once, not with
        // background threads.  Also set the RAM buffer size and the
        // term interval.
        IndexWriterConfig config = new IndexWriterConfig( Version.LUCENE_33, null );
        config.setMergeScheduler( new SerialMergeScheduler() );
        config.setMergePolicy( mergePolicy );
        config.setRAMBufferSizeMB( bufsize );
        config.setTermIndexInterval( termIndexInterval );
        config.setOpenMode( IndexWriterConfig.OpenMode.CREATE_OR_APPEND );

        w = new IndexWriter( new MMapDirectory( dest ), config );
        
        if ( verbose )
          {
            System.err.println( "bufsize = " + bufsize );
            w.setInfoStream( System.out );
          }

        if ( d.length > 0 )
          {
            w.addIndexes( d );
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
