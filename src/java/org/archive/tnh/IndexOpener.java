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

package org.archive.tnh;

import java.io.*;
import java.util.*;

import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;

import org.apache.lucene.index.ArchiveParallelReader;

/**
 * Utility class to create a possibly complex structure of Lucene
 * indexes representing a combination of named index shards and
 * parallel indexes.
 *
 * Call <code>open</code> with the root path of a directory hierarchy
 * containing a mix of per-index collections, shards and parallel
 * indexes and a Map&lt;String,Searcher&gt; is returned.
 *
 * The directory structures handled by this class are of the form:
 *
 * index/
 *
 *   Plain old single lucene index.
 *
 * index/
 *   collectionA/
 *   collectionB/
 *   collectionC/
 *
 *   Where collections A, B, and C are Lucene indexes.  The
 *   resulting Map will contain Searcher objects for each, 
 *   mapped to their corresponding directory names.
 *
 * index/
 *   collectionA/
 *   collectionB/
 *     shard1/
 *     shard2/
 *     shard3/
 *   collectionC/
 *
 *   Similar to the above, but in addition, collectionB will
 *   be a MultiSearcher spanning the three shards, each of
 *   which is a IndexSearcher.
 *
 * index/
 *   collectionA/
 *     _parallel
 *     dates/
 *     main/
 *   collectionB/
 *     shard1/
 *       _parallel
 *       dates/
 *       main/
 *     shard2/
 *   collectionC/
 *
 *   In this case, collectionA has two sub-dirs, but since the magic
 *   file "_parallel" is present, they are treated as parallel
 *   sub-dirs rather than shards.  Similarly for collection B, 
 *   the first shard has parallel sub-dirs, but the second does not.
 *
 * In all of the above examples, only the collection-level indexes are
 * in the Map&lt;String,Searcher&gt;, and thus able to be looked-up by name.
 */
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

  /**
   * Open an index tree rooted at the given <code>indexPath</code>.
   */
  public static Map<String,Searcher> open( String indexPath )
    throws IOException
  {
    return open( indexPath, 1 );
  }

  /**
   * Open an index tree rooted at the given <code>indexPath</code>. Also use
   * the given <code>indexDivisor</code> when opening the indexes.
   */
  public static Map<String,Searcher> open( String indexPath, int indexDivisor )
    throws IOException
  {
    if ( indexPath == null ) throw new IllegalArgumentException( "indexPath cannot be null" );
    if ( indexDivisor < 1  ) throw new IllegalArgumentException( "indexDivisor must be >= 1" );

    File indexDir = new File( indexPath );

    if ( ! indexDir.isDirectory() ) throw new IllegalArgumentException( "indexPath is not a directory: " + indexPath );

    // The map of name->searcher to be returned.
    Map<String,Searcher> searchers = new HashMap<String,Searcher>( );

    // If there are no sub-dirs, then try to open this directory as an index.
    File[] subDirs = indexDir.listFiles( DIR_FILTER );
    
    if ( subDirs == null || subDirs.length == 0 )
      {
        IndexSearcher searcher = new IndexSearcher( IndexReader.open( new MMapDirectory( indexDir ), new KeepOnlyLastCommitDeletionPolicy(), true, indexDivisor ) );
        
        searchers.put( "", searcher );
        
        return searchers;
      }

    // There are sub-dirs.  Try opening each as a subSearcher.
    Searchable subSearchers[] = new Searchable[subDirs.length];
    for ( int i = 0 ; i < subDirs.length ; i++ )
      {
        File subDir = subDirs[i];
        
        IndexSearcher subSearcher = new IndexSearcher( openIndexReader( subDir, indexDivisor ) );
        
        searchers.put( subDir.getName( ), subSearcher );
        
        subSearchers[i] = subSearcher;
      }
    
    // Finally, we create a single MultiSearcher that spans all
    // the others.  Put it in the map with a key of "".
    MultiSearcher multi = new MultiSearcher( subSearchers );
    
    searchers.put( "", multi );

    return searchers;
  }

  /**
   * Opens an IndexReader for the given directory.  The directory may
   * be a plain-old Lucene index, a parallel index, or a root
   * directory of index shards.  In the case of shards, this method is
   * called recursively to open the sub-indexes and combine them into
   * a MultiReader.
   */
  public static IndexReader openIndexReader( File directory, int indexDivisor )
    throws IOException
  {
    if ( directory == null          ) throw new IllegalArgumentException( "directory cannot be null" );
    if ( ! directory.isDirectory( ) ) throw new IllegalArgumentException( "not a directory: " + directory );

    File[] subDirs = directory.listFiles( DIR_FILTER );

    // If there are no sub-dirs, just open this as an IndexReader
    if ( subDirs.length == 0 )
      {
        return IndexReader.open( new MMapDirectory( directory ), new KeepOnlyLastCommitDeletionPolicy(), true, indexDivisor );
      }
    
    // This directory has sub-dirs, and they are parallel.
    if ( directory.listFiles( PARALLEL_FILTER ).length == 1 )
      {
        ArchiveParallelReader preader = new ArchiveParallelReader( );
        for ( int i = 0; i < subDirs.length ; i++ )
          {
            preader.add( IndexReader.open( new MMapDirectory( subDirs[i] ), new KeepOnlyLastCommitDeletionPolicy(), true, indexDivisor ) );
          }
        
        return preader;
      }
    
    // This directory has sub-dirs, but they are not parallel, so they
    // are shards.
    IndexReader[] subReaders = new IndexReader[subDirs.length];
    for ( int i = 0 ; i < subDirs.length ; i++ )
      {
        subReaders[i] = openIndexReader( subDirs[i], indexDivisor );
      }

    IndexReader multi = new MultiReader( subReaders, true );

    return multi;
  }
  
}
