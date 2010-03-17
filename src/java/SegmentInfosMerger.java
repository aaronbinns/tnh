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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

/**
 * 
 */
public class SegmentInfosMerger
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 2 )
      {
        System.err.println( "SegmentInfosMerger <dest> <source>..." );
        System.exit( 1 );
      }
    
    // Remove extra '/' at end of path.
    args[0] = args[0].replaceAll( "[/]+$", "" );

    NIOFSDirectory destDir = new NIOFSDirectory( new File( args[0] ) );

    SegmentInfos dest = new SegmentInfos( );
    
    dest.read( destDir );

    SortedSet<String> existingNames = new TreeSet<String>( );
    for ( int i = 0; i < dest.size( ) ; i++ )
      {
        SegmentInfo s = (SegmentInfo) dest.get( i );

        existingNames.add( s.name );
      }

    TreeMap userData = new TreeMap( dest.getUserData( ) );

    for ( int i = 1 ; i < args.length ; i++ )
      {
        SegmentInfos source = new SegmentInfos( );
        
        // Remove extra '/' at end of path.
        args[i] = args[i].replaceAll( "[/]+$", "" );

        source.read( new NIOFSDirectory( new File( args[i] ) ) );
        
        // For each SegmentInfo in the source, merge into destination.
        for ( int si = 0 ; si < source.size() ; si++ )
          {
            SegmentInfo s = (SegmentInfo) source.get( si );

            String mergedName = s.name;
            if ( existingNames.contains( mergedName ) )
              {
                mergedName = "_" + dest.size( ) + 1;

                existingNames.add( mergedName );
              }

            String sourcePath = args[i] + "/" + s.name;
            String mergedPath = args[0] + "/" + mergedName;

            userData.put( sourcePath, mergedPath );

            System.out.println( sourcePath + " " + mergedPath );

            s.name = mergedName;
            dest.add( s );
          }
      }

    Method m = SegmentInfos.class.getDeclaredMethod( "setUserData", Map.class );
    m.setAccessible( true );
    m.invoke( dest, userData );
    
    m = SegmentInfos.class.getDeclaredMethod( "prepareCommit", Directory.class );
    m.setAccessible( true );
    m.invoke( dest, destDir );

    m = SegmentInfos.class.getDeclaredMethod( "finishCommit", Directory.class );
    m.setAccessible( true );
    m.invoke( dest, destDir ); 
  }
}
