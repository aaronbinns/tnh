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

import java.io.*;
import java.util.*;

import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.index.IndexReader;

import org.apache.lucene.index.ArchiveParallelReader;


public class IndexOpener
{
  public static FileFilter DIR_FILTER = new FileFilter( )
    {
      public boolean accept( File pathname )
      {
        return pathname.isDirectory( );
      }
    };
  
  public static FileFilter PARALLEL_FILTER = new FileFilter( )
    {
      public boolean accept( File pathname )
      {
        return "_parallel".equals( pathname.getName().toLowerCase( ) );
      }
    };
  
  public static Map<String,Searcher> open( String indexPath )
    throws IOException
  {
    return open( indexPath, true, 1 );
  }

  public static Map<String,Searcher> open( String indexPath, boolean trySubDirs )
    throws IOException
  {
    return open( indexPath, trySubDirs, 1 );
  }

  public static Map<String,Searcher> open( String indexPath, int indexDivisor )
    throws IOException
  {
    return open( indexPath, true, indexDivisor );
  }

  public static Map<String,Searcher> open( String indexPath, boolean trySubDirs, int indexDivisor )
    throws IOException
  {
    if ( indexPath == null ) throw new IllegalArgumentException( "indexPath cannot be null" );
    if ( indexDivisor < 1  ) throw new IllegalArgumentException( "indexDivisor must be >= 1" );

    Map<String,Searcher> searchers = new HashMap<String,Searcher>( );

    File indexDir = new File( indexPath );

    try
      {
        IndexSearcher searcher = new IndexSearcher( IndexReader.open( new NIOFSDirectory( indexDir ), true ) );

        searchers.put( "", searcher );
      }
    catch ( IOException ioe )
      {
        if ( ! trySubDirs )
          {
            // If we're not checking for sub-dirs, then throw the original exception.
            throw ioe;
          }

        // Let's try opening sub-dirs as indexes.
        File[] subDirs = indexDir.listFiles( DIR_FILTER );
        
        if ( subDirs == null || subDirs.length == 0 ) throw new IllegalArgumentException( "No sub-dirs for: " + indexPath );
        
        Searchable subSearchers[] = new Searchable[subDirs.length];
        for ( int i = 0 ; i < subDirs.length ; i++ )
          {
            File subDir = subDirs[i];

            // IndexSearcher searcher = new IndexSearcher( IndexReader.open( new NIOFSDirectory( subDir ), true ) );
            Searcher searcher = subsearcher( subDir );
            
            searchers.put( subDir.getName( ), searcher );

            subSearchers[i] = searcher;
          }

        // Finally, we create a single MultiSearcher that spans all
        // the others.  Put it in the map with a key of "".
        MultiSearcher multi = new MultiSearcher( subSearchers );

        searchers.put( "", multi );
      }

    return searchers;
  }

  public static Searcher subsearcher( File directory )
    throws IOException
  {
    if ( ! directory.isDirectory( ) ) throw new IllegalArgumentException( "not a directory: " + directory );

    File[] subDirs = directory.listFiles( DIR_FILTER );

    // If there are no sub-dirs, just open this as an IndexSearcher
    if ( subDirs.length == 0 )
      {
        IndexSearcher searcher = new IndexSearcher( IndexReader.open( new NIOFSDirectory( directory ), true ) );
        
        return searcher;
      }

    // This directory has sub-dirs, and they are parallel.
    if ( directory.listFiles( PARALLEL_FILTER ).length == 1 )
      {
        ArchiveParallelReader preader = new ArchiveParallelReader( );
        for ( int i = 0; i < subDirs.length ; i++ )
          {
            preader.add( IndexReader.open( new NIOFSDirectory( subDirs[i] ), true ) );
          }
        
        IndexSearcher searcher = new IndexSearcher( preader );

        return searcher;
      }

    // This directory has sub-dirs, but they are not parallel
    Searchable subSearchers[] = new Searchable[subDirs.length];
    for ( int i = 0; i < subDirs.length ; i++ )
      {
        File subDir = subDirs[i];
        
        Searcher searcher = subsearcher( subDir );

        subSearchers[i] = searcher;
      }

    MultiSearcher multi = new MultiSearcher( subSearchers );

    return multi;
  }
  
}
