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
import java.net.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Custom Lucene (document) Collector which only keeps the N
 * top-scoring documents per site.
 *
 * Instantiate with a FieldCache containing the sites for all the
 * documents in the index; then pass to the Lucene Searcher.search()
 * method to execute the search.  The CollapsingCollector will collect
 * the results, keeping only the top N per site.
 * 
 * Use the getNumUncollapsedHits() and getHits() methods to retrieve
 * the total number of uncollapsed hits and the collapsed Hits.
 */
public class CollapsingCollector extends Collector
{
  public static final Comparator<Hit> SCORE_COMPARATOR = new Comparator<Hit>( )
  {
    /**
     * This is an *ascending* sort for *score*, so that *lower* scores
     * are sorted *first*.
     *
     * But it is *descending* sort for document id, so that *higher* ids 
     * are sorted *first*.  I.e. a 
     */
    public int compare( Hit h1, Hit h2 )
    {
      if ( h1.score <  h2.score ) return -1;
      if ( h1.score >  h2.score ) return  1;

      if ( h1.id < h2.id ) return  1;
      if ( h1.id > h2.id ) return -1;

      // must be equal
      return 0;
    }
  };

  public static final Comparator<Hit> SITE_COMPARATOR_PARTIAL = new Comparator<Hit>( )
  {
    public int compare( Hit h1, Hit h2 )
    {
      return String.CASE_INSENSITIVE_ORDER.compare( h1.site, h2.site );
    }
  };

  public static final Comparator<Hit> SITE_COMPARATOR_TOTAL = new Comparator<Hit>( )
  {
    public int compare( Hit h1, Hit h2 )
    {
      int c = String.CASE_INSENSITIVE_ORDER.compare( h1.site, h2.site );
      
      if ( c != 0 ) return c;

      // If the sites are the same, then compare the scores.
      return SCORE_COMPARATOR.compare( h1, h2 );
    }
  };

  final FieldCache  siteCache;
  final int         maxNumResults;
  final int         hitsPerSite;

  IndexReader reader    = null;
  int         docBase   = 0;
  Scorer      scorer    = null;

  final Hit[] sortedByScore;
  final Hit[] sortedBySite;
  final Hit   candidate;
  
  int lowestHitPosition;
 
  int numUncollapsedHits     = 0;
  int numCandidatesPassScore = 0;
  int numCandidatesFailScore = 0;
  int numCandidatesPassSite  = 0;
  int numCandidatesFailSite  = 0;

  public CollapsingCollector( final FieldCache siteCache, final int maxNumResults )
  {
    this( siteCache, maxNumResults, 1 );
  }

  public CollapsingCollector( final FieldCache siteCache, final int maxNumResults, final int hitsPerSite )
  {
    this.siteCache      = siteCache;
    this.maxNumResults  = maxNumResults;
    this.hitsPerSite    = hitsPerSite;

    this.sortedByScore = new Hit[maxNumResults];
    this.sortedBySite  = new Hit[maxNumResults];

    for ( int i = 0; i < maxNumResults; i++ )
      {
        Hit sd = new Hit( -1, Float.NEGATIVE_INFINITY, "" );
        this.sortedByScore[i] = sd;
        this.sortedBySite [i] = sd;
      }

    this.candidate = new Hit( -1, Float.NEGATIVE_INFINITY, "" );

    this.lowestHitPosition = maxNumResults - 1;
  }

  public boolean acceptsDocsOutOfOrder( )
  {
    return true;
  }

  public void setNextReader( IndexReader reader, int docBase )
    throws IOException
  {
    this.reader  = reader;
    this.docBase = docBase;
  }

  public void setScorer( Scorer scorer )
  {
    this.scorer = scorer;
  }

  public void collect( int docId )
    throws IOException
  {
    this.numUncollapsedHits++;
   
    this.candidate.id    = this.docBase + docId;
    this.candidate.score = this.scorer.score( );

    if ( lowestHitPosition == 0 && ( this.candidate.score <= this.sortedByScore[0].score ) )
      {
        this.numCandidatesFailScore++;

        return ;
      }

    this.candidate.site = this.siteCache.getValue( this.reader, this.docBase, docId );

    // Use "" rather than null to keep searching and sorting simple.
    if ( this.candidate.site == null ) this.candidate.site = "";

    int sitePos = findReplacementPosition( candidate );

    // No existing hit to be replaced, so we replace the overall
    // lowest-scoring one in the current lowestHitPosition.
    if ( sitePos < 0 )
      {
        this.sortedByScore[this.lowestHitPosition].id    = candidate.id;
        this.sortedByScore[this.lowestHitPosition].score = candidate.score;
        this.sortedByScore[this.lowestHitPosition].site  = candidate.site;

        if ( lowestHitPosition > 0 ) lowestHitPosition--;

        this.numCandidatesPassScore++;

        // Since we just added a new document, re-sort them by score.
        Arrays.sort( this.sortedByScore, this.lowestHitPosition, this.sortedByScore.length, SCORE_COMPARATOR );

        // If we are collapsing, re-sort by site too.
        if ( this.hitsPerSite != 0 )
          {
            Arrays.sort( this.sortedBySite, this.lowestHitPosition, this.sortedBySite.length, SITE_COMPARATOR_TOTAL );
          }

        // Done!
        return ;
      }

    // If we have a candidate Hit with the *same* score as the current
    // Hit for the site, then we replace the docId if the candidate's
    // docId is lower.  Since we're not changing the scores, no need
    // to re-sort afterwards.
    if ( candidate.score == this.sortedBySite[sitePos].score &&
         candidate.id    <  this.sortedBySite[sitePos].id )
      {
        this.numCandidatesPassSite++;

        this.sortedBySite[sitePos].id = candidate.id;
      }
    // If the candidate Hit has a *higher* score than the current Hit
    // for the site, then replace the current with the candidate.
    else if ( candidate.score > this.sortedBySite[sitePos].score )
      {
        this.numCandidatesPassSite++;
        
        this.sortedBySite[sitePos].id    = this.candidate.id;
        this.sortedBySite[sitePos].score = this.candidate.score;
        
        // We have to re-sort by scores.
        Arrays.sort( this.sortedByScore, this.lowestHitPosition, this.sortedByScore.length, SCORE_COMPARATOR );

        // If our hitsPerSite > 1, then we have to re-sort by site to
        // ensure that the hit we just inserted is put into the proper
        // sorted position within the site group.  If hitsPerSite==1,
        // then the group size == 1 and therefore no need to re-sort.
        if ( this.hitsPerSite > 1 )
          {
            Arrays.sort( this.sortedBySite, this.lowestHitPosition, this.sortedByScore.length, SITE_COMPARATOR_TOTAL );
          }
      }
    else
      {
        this.numCandidatesFailSite++;
      }
  }

  /**
   * Finds the position in the sortedBySite array to potentially
   * replace with the candidate, if the candidate's score is good
   * enough.
   * For hitsPerSite == 0, just return -1.
   * For hitsPerSite &gt; 0, we return the position of the lowest-scoring
   * Hit for the candidate site, *if and only if* the number of hits
   * from that site is at the maximum number allowed.  Otherwise,
   * we can still allow more hits from the candidate site, so we
   * return -1.
   */
  private int findReplacementPosition( Hit candidate )
  {
    if ( this.hitsPerSite == 0 ) return -1;

    // By using a partial comparator, we will either find one (of
    // potentially many) Hits having the candidate site, or we will
    // get pos < 1 indicating none were found.
    int pos = Arrays.binarySearch( this.sortedBySite, candidate, SITE_COMPARATOR_PARTIAL );
    
    // If none found, return -1;
    if ( pos < 0 ) return -1;

    // If one was found, and hitsPerSite == 1, then we found the only
    // match, return its position.
    if ( this.hitsPerSite == 1 ) return pos;
    
    // Ok, if we get here, we found a hit with the same site as the
    // candidate.  We search both left and right from that position to
    // determine where the left and right ending positions are for all
    // the Hits with the same site.
    int i = pos, j = pos;

    final int min = 0, max = this.sortedBySite.length - 1;

    for ( ; i > min && SITE_COMPARATOR_PARTIAL.compare( this.sortedBySite[i], this.sortedBySite[i-1] ) == 0; i-- )
      ;

    for ( ; j < max && SITE_COMPARATOR_PARTIAL.compare( this.sortedBySite[i], this.sortedBySite[j+1] ) == 0; j++ )
      ;

    // The number of hits from this site is (j-i+1), so if we are less
    // than the max number of hits per site, then we return -1 to
    // indicate there is still room for more Hits from the candidate
    // site.
    if ( (j - i + 1) < this.hitsPerSite ) return -1;

    // Otherwise, the Hit to be potentially replaced is the lowest
    // scoring hit, which is the one at position i.
    return i;
  }

  /**
   * Return array of Hits.  If there were no hits, the array is of
   * size 0.
   */
  public Hit[] getHits()
  {
    Hit[] hits = new Hit[this.getNumHits( )];

    final int sortedByScoreEndPos = this.sortedByScore.length - 1;
    for ( int i = 0; i < hits.length ; i++ )
      {
        hits[i] = this.sortedByScore[ sortedByScoreEndPos - i ];
      }

    return hits;
  }

  /**
   * Returns the number of collapsed hits.  Usually this will be equal
   * to the number of hits requested, unless we found fewer after
   * collapsing.
   *
   * For example, if we requested 10, found 349 "raw" hits, then
   * collapsed and collected the top 10, then this would return 10.
   * If you want the total uncollapsed, or "raw", hits, then call
   * getNumUncollapsedHits().
   *
   * This number returned by this method will match the size of the
   * array returned by getHits().  So, usually you would just get the
   * Hits[] and not bother calling this method.
   */
  public int getNumHits( )
  {
    for ( int i = 0; i < this.sortedByScore.length ; i++ )
      {
        if ( this.sortedByScore[i].score != Float.NEGATIVE_INFINITY )
          {
            return this.sortedByScore.length - i;
          }
      }
    return 0;
  }

  /**
   * Returns the total number of uncollapsed, or "raw", hits.
   */
  public int getNumUncollapsedHits( )
  {
    return this.numUncollapsedHits;
  }

  public int getNunCandidatesPassScore( )
  {
    return this.numCandidatesPassScore;
  }

  public int getNumCandidatesFailScore( )
  {
    return this.numCandidatesFailScore;
  }

  public int getNumCandidatesPassSite( )
  {
    return this.numCandidatesPassSite;
  }

  public int getNumCandidatesFailSite( )
  {
    return this.numCandidatesFailSite;
  }

}
