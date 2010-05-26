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

import java.io.IOException;
import java.io.File;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;


public class IndexMerger
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length < 2 )
      {
        System.err.println( "IndexMerger [-o|-m] <dest> <source>..." );
        System.exit( 1 );
      }

    boolean optimize = false;
    boolean merge    = false;

    int i = 0;
    for ( ; i < args.length ; i++ )
      {
        if ( "-o".equals( args[i] ) )
          {
            optimize = true;
          }
        else if ( "-m".equals( args[i] ) )
          {
            merge = true;
          }
        else
          {
            break ;
          }
      }

    File dest = new File( args[i++] );

    if ( ! merge && dest.exists( ) )
      {
        System.err.println( "Destination exits, use -m to merge.  Dest: " + args[i] );

        System.exit( 2 );
      }
    
    Directory d[] = new Directory[args.length-i];
    for ( int j = i ; j < args.length ; j++ )
      {
        d[j-i] = new NIOFSDirectory( new File( args[j] ) );
      }

    IndexWriter w = null;
    try
      {
        w = new IndexWriter( new NIOFSDirectory( dest ), null, true, IndexWriter.MaxFieldLength.UNLIMITED );
        w.setUseCompoundFile(false);
        w.addIndexesNoOptimize( d );
        if ( optimize )
          {
            w.optimize( );
          }
        w.commit( );
        w.close( );
      }
    catch ( IOException ioe )
      {
        System.err.println( "Error: " + args[0] + " " + ioe );

        if ( w != null ) w.close();
      }    
  }
}
