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

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.search.highlight.*;

public class DiagnosticServlet extends HttpServlet
{
  public static final Logger LOG = Logger.getLogger( OpenSearchServlet.class.getName() );

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
  {
    Document doc  = new Document();
    Element  root = new Element( "search" );
    
    doc.addContent( root );

    Search search = (Search) this.getServletConfig().getServletContext().getAttribute( "tnh.search" );

    if ( search != null )
      {
        for ( Map.Entry<String,Searcher> s : search.searchers.entrySet() )
          {
            String   name     = s.getKey();
            Searcher searcher = s.getValue();

            Element e = new Element( "searcher" );
            root.addContent( e );

            e.setAttribute( "name", name );
            e.setAttribute( "type", searcher.getClass().getCanonicalName() );

            try
              {
                IndexSearcher is = (IndexSearcher) searcher;
                IndexReader   ir = is.getIndexReader();

                Element ise = new Element( "index" );
                e.addContent( ise );

                ise.setAttribute( "numDocs", Integer.toString( ir.numDocs() ) );
              }
            catch ( ClassCastException cce )
              {
              }
            
          }
      }

    OpenSearchHelper.writeResponse( doc, response );
  }

}
