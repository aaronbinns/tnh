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
import java.util.regex.*;

import org.apache.lucene.search.*;
import org.apache.lucene.index.*;

/**
 * Parses user query and translates it into a Lucene BooleanQuery,
 * applying custom rules for handling quoted phrases, +/- operators
 * and the application of terms to the url, title and content fields.
 *
 * NOTE: Although there is no member data, the methods are not static.
 * Allows addition of interface with multiple implementations later.
 */
public class DefaultQueryTranslator
{
  public static final Pattern TOKENIZER = Pattern.compile( "([-+]*[\"][^\"]+[\"]?|[-+]*[^\\s\"]+)" );

  public BooleanQuery translate( String query )
  {
    return this.translate( query, true );
  }

  public BooleanQuery translate( String query, boolean foldAccents )
  {
    List<String> terms = new ArrayList<String>( 8 );
    List<String> minus = new ArrayList<String>( 8 );

    // Replace "smart quotes" with " so phrases can be detected using
    // either.
    query = query.replace( '“', '"' ).replace( '”', '"' );

    Matcher m = TOKENIZER.matcher( query );

    while ( m.find( ) )
      {
        String term = m.group( 1 );

        boolean isMinus = term.charAt( 0 ) == '-';

        // Remove any leading + - characters.
        term = term.replaceFirst( "^[-+]+", "" );

        // Now that the quotes are handled, remove them.
        term = term.replace( "\"", "" );

        // HACK: To handle "don't" -> "dont" and such.
        term = term.replace( "'", "" );

        // Use the Java regex syntax:
        //   \p{L}  -- All Unicode letters
        //   \p{N}  -- All Unicode numbers
        // Anything that is not a letter|number is stripped.
        term = term.replaceAll( "[^\\p{L}\\p{N}]", " " );

        if ( term.length() == 0 ) continue ;

        term = term.toLowerCase();

        if ( foldAccents ) term = ASCIIFolder.fold( term );

        if ( isMinus )
          {
            minus.add( term );
          }
        else 
          {
            terms.add( term );
          }
      }
    
    BooleanQuery bq = new BooleanQuery( );
    fieldGroup( bq, terms, "url"  ,   (float) 4.0 );
    fieldGroup( bq, terms, "title",   (float) 3.0 );
    fieldGroup( bq, terms, "boiled",  (float) 2.5 );
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
        Query q = buildQuery( field, t );
        q.setBoost( boost );

        group.add( q, BooleanClause.Occur.MUST );
      }

    bq.add( group, BooleanClause.Occur.SHOULD );
  }

  public void mixAndMatch( BooleanQuery bq, List<String> terms )
  {
    for ( int i = 0 ; i < terms.size() ; i++ )
      {
        BooleanQuery group = new BooleanQuery( );
        
        Query q = buildQuery( "title", terms.get(i) );
        group.add( q, BooleanClause.Occur.MUST );

        for ( int j = 0 ; j < terms.size() ; j++ )
          {
            if ( j == i ) continue ;

            q = buildQuery( "content", terms.get(j) );
            group.add( q, BooleanClause.Occur.MUST );
          }

        bq.add( group, BooleanClause.Occur.SHOULD );
      }
  }

  public void minuses( BooleanQuery bq, List<String> minus )
  {
    for ( String m : minus )
      {
        bq.add( buildQuery( "url",     m ), BooleanClause.Occur.MUST_NOT );
        bq.add( buildQuery( "title",   m ), BooleanClause.Occur.MUST_NOT );
        bq.add( buildQuery( "content", m ), BooleanClause.Occur.MUST_NOT );
      }
  }

  public void addFilterGroup( BooleanQuery bq, String field, String[] values )
  {
    if ( values == null || values.length == 0 )
      {
        return ;
      }

    BooleanQuery group = new BooleanQuery( );

    for ( String value : values )
      {
        Query q = buildQuery( field, value );
        group.add( q, BooleanClause.Occur.SHOULD );            
      }

    // Use a ConstantScoreQuery so that the extra terms do not change
    // the scoring.  In fact, these should probably be implemented as
    // filters, not queries.
    bq.add( new ConstantScoreQuery( group ), BooleanClause.Occur.MUST );
  }

  public Query buildQuery( String field, String term )
  {
    if ( term.indexOf( ' ' ) == -1 )
      {
        TermQuery tq = new TermQuery( new Term( field, term ) );

        return tq;
      }
    else
      {
        PhraseQuery pq = new PhraseQuery( );
        for ( String t : term.trim().split( "\\s+" ) )
          {
            pq.add( new Term( field, t ) );
          }
        return pq;
      }
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
