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
import java.util.logging.Logger;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;


/**
 * This is heavily cribbed from org.apache.lucene.misc.LengthNormModifier
 */
public class LengthNormUpdater 
{
  public static final Logger LOG = Logger.getLogger( LengthNormUpdater.class.getName() );

  private static final String USAGE = 
    "Usage: LengthNormUpdater [OPTIONS] <pageranks> <index> [field1]...\n"
    + "\n"
    + "Update the norms of <index> with boosts based on values from <pageranks>\n"
    + "\n"
    + "Options:\n"
    + "\t-s <classname>    similarity implementation to use\n"
    + "\t-v                increase verbosity\n"
    + "\n" 
    + "Reads the pagerank values from the <pageranks> file and calculates new\n"
    + "norms for the documents based on the formula:\n"
    + "\n"
    + "\tnorm = similarity.lengthNorm * log10(pagerank)\n"
    + "\n"
    + "If fields are specified on the command-line, only they will be updated.\n"
    + "If a specified field does not have norms, an error message is given and\n"
    + "the program terminates without performing any updates.\n"
    + "\n"
    + "If no fields are given, all the fields in the index that have norms will\n"
    + "be updated.\n"
    + "\n"
    + "The default similarity implementation is Lucene's DefaultSimilarity\n"
    + "\n"
    + "Examples:\n"
    + "\n"
    + "\tLengthNormUpdater pagerank.txt index\n"
    + "\tLengthNormUpdater -v -v pagerank.txt index title content\n"
    + "\n"
    ;

  private static int VERBOSE = 0;

  /**
   *
   */
  public static void main( String[] args ) throws IOException 
  {
    if ( args.length < 1 ) 
      {
        System.err.print( USAGE );
        System.exit(1);
      }
    
    Similarity s = new DefaultSimilarity( );

    int pos = 0;
    for ( ; (pos < args.length) && args[pos].startsWith( "-" ) ; pos++ )
      {
        if ( "-h".equals( args[pos] ) )
          {
            System.out.println( USAGE );
            System.exit( 0 );
          }
        else if ( "-v".equals( args[pos] ) )
          {
            VERBOSE++;
          }
        else if ( "-s".equals( args[pos] ) )
          {
            pos++;

            if ( pos == args.length )
              {
                System.err.println( "Error: missing argument to option -s" );
                System.exit( 1 );
              }

            try 
              {
                Class simClass = Class.forName(args[pos]);
                s = (Similarity)simClass.newInstance();
              }
            catch (Exception e) 
              {
                System.err.println( "Couldn't instantiate similarity with empty constructor: " + args[pos] );
                e.printStackTrace(System.err);
                System.exit( 1 );
              }
          }
      }
    
    if ( (pos + 2) > args.length ) 
      {
        System.out.println( USAGE );
        System.exit( 1 );
      }

    String pagerankFile = args[pos++];
    
    IndexReader reader = IndexReader.open( new MMapDirectory( new File( args[pos++] ) ), false );

    try
      {
        Set<String> fieldNames = new HashSet<String>( );
        if ( pos == args.length )
          {
            // No fields specified on command-line, get a list of all
            // fields in the index that have norms.
            for ( String fieldName : (Collection<String>) reader.getFieldNames( IndexReader.FieldOption.ALL ) )
              {
                if ( reader.hasNorms( fieldName ) )
                  {
                    fieldNames.add( fieldName );
                  }
              }
          }
        else
          {
            // Verify all explicitly specified fields have norms.
            for ( int i = pos ; i < args.length ; i++ )
              {
                if ( ! reader.hasNorms( args[i] ) )
                  {
                    System.err.println( "Error: No norms for field: " + args[i] );
                    System.exit( 1 );
                  }
                
                fieldNames.add( args[i] );
              }
          }
        
        if ( fieldNames.isEmpty( ) )
          {
            System.out.println( "Warning: No fields with norms to update" );
            System.exit( 0 );
          }
        
        Map<String,Integer> ranks = getPageRanks( pagerankFile );
        
        for ( String fieldName : fieldNames )
          {
            updateNorms( reader, fieldName, ranks, s );
          }

      }
    finally
      {
        if ( reader != null )
          {
            reader.close( );
          }
        
      }
  }

 
  /**
   *
   */
  public static void updateNorms( IndexReader reader, 
                                  String fieldName, 
                                  Map<String,Integer> ranks, 
                                  Similarity sim )
    throws IOException 
  {
    if ( VERBOSE > 0 ) System.out.println( "Updating field: " + fieldName );

    int[] termCounts = new int[0];
    
    TermEnum termEnum = null;
    TermDocs termDocs = null;
    
    termCounts = new int[reader.maxDoc()];
    try 
      {
        termEnum = reader.terms(new Term(fieldName,""));
        try 
          {
            termDocs = reader.termDocs();
            do 
              {
                Term term = termEnum.term();
                if (term != null && term.field().equals(fieldName)) 
                  {
                    termDocs.seek(termEnum.term());
                    while (termDocs.next()) 
                      {
                        termCounts[termDocs.doc()] += termDocs.freq();
                      }
                  }
              } 
            while (termEnum.next());
          }
        finally 
          {
            if (null != termDocs) termDocs.close();
          }
      }
    finally 
      {
        if (null != termEnum) termEnum.close();
      }
    
    for (int d = 0; d < termCounts.length; d++) 
      {
        if ( ! reader.isDeleted(d) ) 
          {
            Document doc = reader.document( d );

            String url = doc.get( "url" );

            if ( url != null )
              {
                Integer rank = ranks.get( url );
                if ( rank == null ) continue;
                
                float originalNorm = sim.lengthNorm(fieldName, termCounts[d]);
                byte  encodedOrig  = sim.encodeNorm(originalNorm);
                float rankedNorm   = originalNorm * (float) ( Math.log10( rank ) + 1 );
                byte  encodedRank  = sim.encodeNorm(rankedNorm);
                                
                if ( VERBOSE > 1 ) System.out.println( fieldName + "\t" + d + "\t" + originalNorm + "\t" + encodedOrig + "\t" + rankedNorm + "\t" + encodedRank );
                
                reader.setNorm(d, fieldName, encodedRank);
              }
          }
      }
  }

  /**
   * Utility function to read a list of page-rank records from a file
   * specified in the configuration.
   */
  public static Map<String,Integer> getPageRanks( String filename )
  {
    if ( VERBOSE > 0 ) System.out.println( "Reading pageranks from: " + filename );

    Map<String,Integer> pageranks = new HashMap<String,Integer>( );

    BufferedReader reader = null;
    try
      {
        reader = new BufferedReader( new InputStreamReader( new FileInputStream( filename), "UTF-8" ) );
        
        String line;
        while ( (line = reader.readLine()) != null )
          {
            String fields[] = line.split( "\\s+" );
            
            if ( fields.length < 2 )
              {
                System.err.println( "Malformed pagerank, not enough fields ("+fields.length+"): " + line );
                continue ;
              }
            
            try
              {
                int    rank = Integer.parseInt( fields[0] );
                String url  = fields[1];
                
                if ( rank < 0 )
                  {
                    System.err.println( "Malformed pagerank, rank less than 0: " + line );
                  }
                
                pageranks.put( url, rank );
              }
            catch ( NumberFormatException nfe )
              {
                System.err.println( "Malformed pagerank, rank not an integer: " + line );
                continue ;
              }
          }
      }
    catch ( IOException e )
      {
        // Umm, what to do?
        throw new RuntimeException( e );
      }
    finally
      {
        try
          {
            if ( reader != null )
              {
                reader.close( );
              }
          }
        catch  ( IOException e )
          {
            // Ignore it.
          }
      }
    
    return pageranks;
  }

  
}
