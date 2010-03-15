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

import java.util.Arrays;
import java.util.Collections;
import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Scorer;


public class TestCollapse2
{
  static Hit[] docs = 
    {
      new Hit(  0, 0.0f, "d" ),
      new Hit(  1, 0.1f, "a" ),
      new Hit(  2, 0.2f, "b" ),
      new Hit(  3, 0.3f, "c" ),
      new Hit(  4, 0.4f, "a" ),
      new Hit(  5, 0.5f, "c" ),
      new Hit(  6, 0.6f, "b" ),
      new Hit(  7, 0.7f, "d" ),
      new Hit(  8, 0.8f, "e" ),
      new Hit(  9, 0.9f, "a"  ),
      new Hit( 10, 0.15f, "g" ),
      new Hit( 11, 0.15f, "g" ),
      new Hit( 12, 0.15f, "g" ),
      new Hit( 13, 0.25f, "g" ),
      new Hit( 14, 0.25f, "g" ),
      new Hit( 15, 0.35f, "g" ),
      new Hit( 16, 0.35f, "g" ),
      new Hit( 17, 0.45f, "g" ),
      new Hit( 18, 0.55f, "g" ),
    };

  public static void main( String[] args )
    throws Exception
  {
    int hitsPerSite = 1;
    if ( args.length > 0 )
      {
        hitsPerSite = Integer.parseInt( args[0] );
      }

    String siteCache[] = new String[docs.length];
    for ( int i = 0; i < docs.length ; i++ )
      {
        siteCache[i] = docs[i].site;
      }

    FieldCache fc = new TestFieldCache( siteCache );

    for ( int max = 1 ; max < (docs.length + 1); max++ )
      {
        System.out.println( "Max: " + max );
        System.out.println( "HPS: " + hitsPerSite );

        Collections.shuffle( Arrays.asList( docs ) );

        TestScorer scorer = new TestScorer( );

        CollapsingCollector2 collector = new CollapsingCollector2( fc, max, hitsPerSite );

        // Dummy call to force collector to load site cache.
        collector.setNextReader( null, 0 );
        collector.setScorer( scorer );

        long responseTime = System.nanoTime( );
        for ( int i = 0 ; i < docs.length ; i++ )
          {
            scorer.setCurrent( docs[i] );
            
            collector.collect( docs[i].id );
          }
        responseTime = System.nanoTime( ) - responseTime;
        System.err.println( "Response Time   : " + (responseTime/1000.0f/1000.0f) + "ms"  );

        for ( int i = 0; i < collector.sortedByScore.length ; i++ )
          {
            Hit sd = collector.sortedByScore[i];

            System.out.println( "doc["+i+"]: id="+sd.id+" score="+sd.score+" site="+sd.site );
          }

        System.out.println( );
      }
  }
  
}

class TestFieldCache implements FieldCache
{
  String[] cache;

  public TestFieldCache( String[] cache )
  {
    this.cache = cache;
  }

  public String getValue( IndexReader reader, int docBase, int docId )
  {
    return this.cache[docId];
  }

}

class TestScorer extends Scorer
{
  Hit currentDoc;

  public TestScorer( )
  {
    super( null );
  }

  public void setCurrent( Hit doc )
  {
    this.currentDoc = doc;
  }

  public float score( )
  {
    return this.currentDoc.score;
  }
}
