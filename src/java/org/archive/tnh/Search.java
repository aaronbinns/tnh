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

import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.util.Version;

public class Search
{
  private static final DefaultQueryTranslator TRANSLATOR = new DefaultQueryTranslator( );

  public Map<String,Searcher> searchers;
  public FieldCache           siteCache;

  public Search( Searcher searcher )
  {
    if ( searcher == null ) throw new IllegalArgumentException( "searcher cannot be null" );

    Map<String,Searcher> searchers = Collections.singletonMap( "", searcher );
    this.init( searchers );
  }

  public Search( Map<String,Searcher> searchers )
  {
    if ( searchers == null ) throw new IllegalArgumentException( "searchers cannot be null" );
    if ( searchers.get( "" ) == null )
      {
        throw new IllegalArgumentException( "Searchers map does not contain a searcher for key \"\"" );
      }
    this.init( searchers );
  }

  private void init( Map<String,Searcher> searchers )
  {
    this.searchers = searchers;
    this.siteCache = new FieldCacheLucene( "site" );
  }

  public FieldCache getSiteCache( )
  {
    return this.siteCache;
  }

  public void setSiteCache( FieldCache siteCache )
  {
    if ( siteCache == null ) throw new IllegalArgumentException( "siteCache cannot be null" );
    this.siteCache = siteCache;
  }

  public boolean hasIndex( String name )
  {
    return this.searchers.containsKey( name );
  }

  public Set<String> getIndexNames( )
  {
    return this.searchers.keySet( );
  }

  public Result search( String query, int maxHits, int hitsPerSite )
    throws Exception
  {
    return this.search( this.searchers.get(""), query, maxHits, hitsPerSite );
  }

  public Result search( String indexName, String query, int maxHits, int hitsPerSite )
    throws Exception
  {
    Searcher searcher = this.searchers.get( indexName );

    if ( searcher == null )  throw new IllegalArgumentException( "Index not found: " + indexName );

    return this.search( searcher, query, maxHits, hitsPerSite );
  }

  public Result search( String indexNames[], String query, int maxHits, int hitsPerSite )
    throws Exception
  {
    Searcher s = buildMultiSearcher( indexNames );

    return this.search( s, query, maxHits, hitsPerSite );
  }

  public Result search( String indexNames[], Query query, int maxHits, int hitsPerSite )
    throws Exception
  {
    Searcher s = buildMultiSearcher( indexNames );

    return this.search( s, query, maxHits, hitsPerSite );
  }

  public Result search( Searcher searcher, String query, int maxHits, int hitsPerSite )
    throws Exception
  {
    Query q = TRANSLATOR.translate( query );

    return this.search( searcher, q, maxHits, hitsPerSite );
  }

  public Result search( Searcher searcher, Query query, int maxHits, int hitsPerSite )
    throws Exception
  {
    if ( searcher == null ) throw new IllegalArgumentException( "searcher cannot be null" );
    if ( query    == null ) throw new IllegalArgumentException( "query cannot be null" );
    if ( maxHits  <= 0    ) throw new IllegalArgumentException( "maxHits must be > 0" );
    if ( hitsPerSite < 0  ) throw new IllegalArgumentException( "hitsPerSite must be >= 0" );

    CollapsingCollector collector = new CollapsingCollector( this.siteCache, maxHits, hitsPerSite );

    searcher.search( query, collector );

    Result result = new Result( );
    result.searcher  = searcher;
    result.numRawHits= collector.getNumUncollapsedHits( );
    result.hits      = collector.getHits( );
 
    return result;
  }

  public MultiSearcher buildMultiSearcher( String indexNames[] )
    throws IOException
  {
    if ( indexNames == null || indexNames.length == 0 ) throw new IllegalArgumentException( "At least one indexName must be specified" );
    
    Searchable[] searchables = new Searchable[indexNames.length];
    for ( int i = 0 ; i < indexNames.length ; i++ )
      {
        searchables[i] = this.searchers.get( indexNames[i] );
        if ( searchables[i] == null ) throw new IllegalArgumentException( "Index not found: " + indexNames[i] );
      }
    
    MultiSearcher searcher = new MultiSearcher( searchables );
    
    return searcher;
  }

  /**
   * Given a searcher and a docId, find the corresponding name of the
   * corresponding index in the searchers map.  Returns the name of
   * the index, or <code>null</code> if not found.
   */
  public String resolveIndexName( Searchable searcher, int docId )
  {
    if ( searcher instanceof MultiSearcher )
      {
        MultiSearcher ms = (MultiSearcher) searcher;
        
        Searchable[] subsearchers = ms.getSearchables();
        
        searcher = subsearchers[ms.subSearcher( docId )];

        return resolveIndexName( searcher, docId );
      }
    if ( searcher instanceof IndexSearcher )
      {
        for ( Map.Entry<String,Searcher> entry : this.searchers.entrySet() )
          {
            // System.err.println( "Considering " + entry.getKey() + " (" + entry.getValue() + " ) for result.searcher: " + searcher );

            if ( entry.getValue() == searcher )
              {
                // System.err.println( "Result " + docId + " from searcher " + entry.getKey() );
                
                return entry.getKey( );
              }
          }

        // Didn't find the IndexSearcher.
        return null;
      }
    else
      {
        throw new RuntimeException( "Unknown searcher type: " + searcher );
      }    
  }

  public static class Result
  {
    public Searcher searcher;
    public int      numRawHits;
    public Hit[]    hits;
  }

}

