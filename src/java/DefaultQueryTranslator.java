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

import java.util.*;
import java.io.*;

import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.queryParser.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.util.Version;


public class DefaultQueryTranslator
{

  public BooleanQuery translate( String query )
  {
    String[] splitOnQuotes = query.split( "[\"]" );
    List<String> terms = new ArrayList<String>( 8 );
    List<String> minus = new ArrayList<String>( 8 );
    for ( int i = 0 ; i < splitOnQuotes.length ; i++ )
      {
        String qterm = splitOnQuotes[i];
        if ( i % 2 == 1 )
          {
            // If a '-' appears inside a quoted phrase, remove it.
            qterm = qterm.replace( '-', ' ' );
          }

        // HACK: To handle "don't" -> "dont" and such.
        qterm = qterm.replace( "'", "" );

        // Use the Java regex syntax:
        //   \p{L}  -- All Unicode letters
        //   \p{N}  -- All Unicode numbers
        // Anything that is not a letter|number or '-' is stripped.
        qterm = qterm.replaceAll( "[^\\p{L}\\p{N}-']", " " );

        for ( String t : qterm.split( "\\s+" ) )
          {
            if ( t.length() == 0 ) continue;

            if ( t.charAt( 0 ) == '-' )
              {
                String m = t.replaceFirst( "[-]+", "" );
                if ( m.trim().length( ) > 0 )
                  {
                    minus.add( m.toLowerCase( ) );
                  }
              }
            else 
              {
                terms.add( t.toLowerCase( ) );
              }
          }
      }
    
    BooleanQuery bq = new BooleanQuery( );
    fieldGroup( bq, terms, "url"  ,   (float) 4.0 );
    fieldGroup( bq, terms, "title",   (float) 3.0 );
    fieldGroup( bq, terms, "content", (float) 1.5 );
    if ( terms.size( ) > 1 ) mixAndMatch( bq, terms );
    minuses( bq, minus );
    
    BooleanQuery q = new BooleanQuery( );
    q.add( bq, BooleanClause.Occur.MUST );

   return q;
  }

  public void fieldGroup( BooleanQuery bq, List<String> terms, String field, float boost )
  {
    BooleanQuery group = new BooleanQuery( );

    for ( String t : terms )
      {
        TermQuery tq = new TermQuery( new Term( field, t ) );
        tq.setBoost( boost );

        group.add( tq, BooleanClause.Occur.MUST );
      }

    bq.add( group, BooleanClause.Occur.SHOULD );
  }

  public void mixAndMatch( BooleanQuery bq, List<String> terms )
  {
    for ( int i = 0 ; i < terms.size() ; i++ )
      {
        BooleanQuery group = new BooleanQuery( );
        
        TermQuery tq = new TermQuery( new Term( "title", terms.get(i) ) );
        group.add( tq, BooleanClause.Occur.MUST );

        for ( int j = 0 ; j < terms.size() ; j++ )
          {
            if ( j == i ) continue ;

            tq = new TermQuery( new Term( "content", terms.get(j) ) );
            group.add( tq, BooleanClause.Occur.MUST );
          }

        bq.add( group, BooleanClause.Occur.SHOULD );
      }
  }

  public void minuses( BooleanQuery bq, List<String> minus )
  {
    for ( String m : minus )
      {
        bq.add( new TermQuery( new Term( "url"    , m ) ), BooleanClause.Occur.MUST_NOT );
        bq.add( new TermQuery( new Term( "title"  , m ) ), BooleanClause.Occur.MUST_NOT );
        bq.add( new TermQuery( new Term( "content", m ) ), BooleanClause.Occur.MUST_NOT );
      }
  }

  public void addGroup( BooleanQuery bq, String field, String[] values )
  {
    if ( values == null || values.length == 0 )
      {
        return ;
      }

    BooleanQuery group = new BooleanQuery( );

    for ( String value : values )
      {
        TermQuery tq = new TermQuery( new Term( field, value ) );
        group.add( tq, BooleanClause.Occur.SHOULD );            
      }

    bq.add( group, BooleanClause.Occur.MUST );
  }


  public static void main( String[] args )
    throws Exception
  {
    if ( args.length != 1 )
      {
        System.out.println( "QueryTranslator <query>" );
        System.exit( 1 );
      }
    
    String query = args[0]; 

    DefaultQueryTranslator translator = new DefaultQueryTranslator( );
    
    BooleanQuery q = translator.translate( query );

    System.out.println( "q: " + q.toString( ) );
  }

}
