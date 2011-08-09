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

import java.io.IOException;
import java.io.File;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.NIOFSDirectory;


public class IndexOptimizer
{
  public static void main( String[] args )
    throws Exception
  {
    if ( args.length != 1 )
      {
        System.err.println( "IndexOptimizer <index>" );
        System.exit( 1 );
      }
    
    IndexWriter w = null;
    try
      {
        w = new IndexWriter( new NIOFSDirectory( new File( args[0] ) ), new SimpleAnalyzer( ), false, IndexWriter.MaxFieldLength.UNLIMITED );
        
        w.setUseCompoundFile(false);
        w.optimize( );
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