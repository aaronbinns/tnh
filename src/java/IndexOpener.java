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


public class IndexOpener
{
  
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
        File[] subDirs = indexDir.listFiles( new FileFilter( )
          {
            public boolean accept( File pathname )
            {
              return pathname.isDirectory( );
            }
          } );
        
        if ( subDirs == null || subDirs.length == 0 ) throw new IllegalArgumentException( "No sub-dirs for: " + indexPath );
        
        Searchable subSearchers[] = new Searchable[subDirs.length];
        for ( int i = 0 ; i < subDirs.length ; i++ )
          {
            File subDir = subDirs[i];

            IndexSearcher searcher = new IndexSearcher( IndexReader.open( new NIOFSDirectory( subDir ), true ) );
            
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
}
