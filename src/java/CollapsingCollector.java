/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.util.*;
import java.net.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;


public class CollapsingCollector extends Collector
{
  public static final Comparator<Hit> SCORE_COMPARATOR = new Comparator<Hit>( )
  {
    public int compare( Hit h1, Hit h2 )
    {
      if ( h1.score <  h2.score ) return -1;
      if ( h1.score >  h2.score ) return 1;

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
      return String.CASE_INSENSITIVE_ORDER.compare( h1.site, h2.site );
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

    if ( this.candidate.score <= this.sortedByScore[0].score )
      {
        this.numCandidatesFailScore++;

        return ;
      }

    this.candidate.site = this.siteCache.getValue( this.reader, this.docBase, docId );

    // Use "" rather than null to keep searching and sorting simple.
    if ( this.candidate.site == null ) this.candidate.site = "";

    int sitePos = findReplacementPosition( candidate );

    // No existing hit to be replaced, so we replace the overall
    // lowest-scoring one, which is always in position 0 in the
    // sortedByScore list.
    if ( sitePos < 0 )
      {
        this.sortedByScore[0].id    = candidate.id;
        this.sortedByScore[0].score = candidate.score;
        this.sortedByScore[0].site  = candidate.site;

        this.numCandidatesPassScore++;

        // Since we just added a new site, re-sort them.
        Arrays.sort( this.sortedByScore, SCORE_COMPARATOR );

        // No need to re-sort the sites if not collapsing.
        if ( this.hitsPerSite != 0 )
          {
            Arrays.sort( this.sortedBySite, SITE_COMPARATOR_TOTAL );
          }

        // Done!
        return ;
      }

    // We have an existing Hit from the same site which can be
    // replaced *if* the candidate's score is better.
    if ( candidate.score > this.sortedBySite[sitePos].score )
      {
        this.numCandidatesPassSite++;
        
        this.sortedBySite[sitePos].id    = this.candidate.id;
        this.sortedBySite[sitePos].score = this.candidate.score;
        
        // We have to re-sort by scores.
        Arrays.sort( this.sortedByScore, SCORE_COMPARATOR );

        // If our hitsPerSite > 1, then we have to re-sort by site to
        // ensure that the hit we just inserted is put into the proper
        // sorted position within the site group.  If hitsPerSite==1,
        // then the group size == 1 and therefore no need to re-sort.
        if ( this.hitsPerSite > 1 )
          {
            Arrays.sort( this.sortedBySite, SITE_COMPARATOR_TOTAL );
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
   * For hitsPerSite > 0, we return the position of the lowest-scoring
   * Hit for the candidate site, *if and only if* the number of hits
   * from that site is at the maxiumum number allowed.  Otherwise,
   * we can still allow more hits from the candidate site, so we
   * return -1.
   */
  private int findReplacementPosition( Hit candidate )
  {
    if ( this.hitsPerSite == 0 ) return -1;

    int pos = Arrays.binarySearch( this.sortedBySite, candidate, SITE_COMPARATOR_PARTIAL );
    
    if ( pos < 0 || this.hitsPerSite == 1 ) return pos;
    
    int i = pos, j = pos;

    final int mini = 0, maxj = this.sortedBySite.length - 1;

    for ( ; i > mini && SITE_COMPARATOR_PARTIAL.compare( this.sortedBySite[i], this.sortedBySite[i-1] ) == 0; i-- )
      ;

    for ( ; j < maxj && SITE_COMPARATOR_PARTIAL.compare( this.sortedBySite[i], this.sortedBySite[j+1] ) == 0; j++ )
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

  public int getNumHits( )
  {
    for ( int i = this.sortedByScore.length - this.maxNumResults ; i < this.sortedByScore.length ; i++ )
      {
        if ( this.sortedByScore[i].score != Float.NEGATIVE_INFINITY )
          {
            return this.sortedByScore.length - i;
          }
      }
    return 0;
  }

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
