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
import java.util.*;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.util.Version;

import org.archive.tnh.*;

public class TestSearch
{

 public static void usage( )
  {
    System.err.println( "usage: Search [options] <indexDir> <query>" );
    System.err.println( );
    System.err.println( "  query               query to execute" );
    System.err.println( "" );
    System.err.println( "  Options:" );
    System.err.println( "    -h, --help          this help page" );
    System.err.println( "    -n                  number of results, default 10" );
    System.err.println( "    -d                  index divisor, default 10" );
    System.err.println( "    -s                  hits per site, default 1" );
    System.err.println( "    -i                  index name to search, default \"\"" );
    System.err.println( "    -w                  # warm-up query" );
    System.err.println( "    -r                  # repeat query" );
    System.err.println( "" );
  }

  public static int getInt( String arg, String label, int min )
    throws Exception
  {
    try
      {
        int i = Integer.parseInt( arg );
        
        if ( i < min )
          {
            throw new IllegalArgumentException( "Bad value for " + label + ": " + i + " not > minimum value of " + min );
          }

        return i;
      }
    catch ( NumberFormatException nfe )
      { 
        throw new IllegalArgumentException( "Bad format for " + label + ": " + arg );
      }
  }
   
  public static void main( String args[] )
    throws Exception
  {
    int maxNumResults = 10;
    int hitsPerSite   =  1;
    int indexDivisor  = 10;
    int warmup        =  0;
    int runs          =  1;
    String indexName  = "";

    // Process command-line arguments.
    int i = 0;
    for ( ; i < args.length ; i++ )
      {
        if ( args[i].equalsIgnoreCase( "-h" ) || args[i].equalsIgnoreCase( "-help" ) || args[i].equalsIgnoreCase( "--help" ) )
          {
            usage( );

            System.exit( 1 );
          }
        else if ( args[i].equalsIgnoreCase( "-n" ) )
          {
            maxNumResults = getInt( args[++i], "-n", 0 );
          }
        else if ( args[i].equalsIgnoreCase( "-d" ) )
          {
            indexDivisor = getInt( args[++i], "-d", 1 );
          }
        else if ( args[i].equalsIgnoreCase( "-s" ) )
          {
            hitsPerSite = getInt( args[++i], "-s", 0 );
          }
        else if ( args[i].equalsIgnoreCase( "-w" ) )
          {
            warmup = getInt( args[++i], "-w", 0 );
          }
        else if ( args[i].equalsIgnoreCase( "-r" ) )
          {
            runs = getInt( args[++i], "-r", 1 );
          }
        else if ( args[i].equalsIgnoreCase( "-i" ) )
          {
            indexName = args[++i];
          }
        else
          {
            break;
          }
      }

    if ( args.length - i < 2 )
      {
        System.err.println( "Error, not enough arguments to execute query:" );
        System.err.println( "  index  : " + (i   < args.length ? args[i]   : "MISSING" ));
        System.err.println( "  query  : " + (i+1 < args.length ? args[i+1] : "MISSING" ));

        System.exit( 1 );
      }
    
    String indexDir = args[i++];
    String query    = args[i++];

    long responseTime = System.nanoTime( );

    Map<String,Searcher> searchers = IndexOpener.open( indexDir, indexDivisor );

    Searcher searcher = searchers.get( indexName );
    if ( searcher == null ) throw new IllegalArgumentException( "Specified index not found: \"" + indexName + "\"" );

    Search search = new Search( searchers );

    System.out.println( "searchers   = " + searchers );
    System.out.println( "searcher    = " + searcher  );
    System.out.println( "search      = " + search    );
    System.out.println( "divisor     = " + indexDivisor );
    System.out.println( "query       = " + query );
    System.out.println( "maxResults  = " + maxNumResults );
    System.out.println( "hitsPerSite = " + hitsPerSite );
    System.out.println( "warmup      = " + warmup );
    System.out.println( "runs        = " + runs );

    for ( ; warmup > 0 ; warmup-- )
      {
        System.out.print( "Warm-up (" + warmup + "): " );
        
        responseTime = System.nanoTime( );
        Search.Result result = search.search( searchers.get(""), query, maxNumResults, hitsPerSite );
        responseTime = System.nanoTime( ) - responseTime;
        System.out.println( (responseTime / 1000 / 1000) );
      }

    Search.Result result = null;
    long time = 0;
    for ( int run = 0 ; run < runs ; run++ )
      {
        System.out.print( "Run (" + run + "): " );

        responseTime = System.nanoTime( );
        result = search.search( searcher, query, maxNumResults, hitsPerSite );
        responseTime = System.nanoTime( ) - responseTime;
        time += responseTime;
        System.out.println( (responseTime / 1000 / 1000)  );
        System.gc( );
      }
    System.out.println( "Average time : " + (time / 1000 / 1000 / runs) );
    System.out.println( "Raw hits     : " + result.numRawHits  );
    System.out.println( "Cooked hits  : " + result.hits.length );
    
    // Write out the response
    for ( i = 0 ; i < result.hits.length ; i++ )
      {
        System.out.println( "hit["+i+"] = score: " + result.hits[i].score + " id: " + result.hits[i].id + " site: " + result.hits[i].site );
      }
  } 
}
