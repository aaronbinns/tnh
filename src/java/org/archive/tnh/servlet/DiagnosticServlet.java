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

package org.archive.tnh.servlet;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jdom.*;

import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.analysis.*;
import org.apache.lucene.search.highlight.*;

import org.archive.tnh.*;

/**
 * Simple servlet which emits diagnostic info via an XML Document of
 * the form:
 * &lt;info>
 *  &lt;searcher name="foo" type="org.apache.lucene.search.IndexSearcher">
 *    &lt;index numDocs="82215">
 *      &lt;field name="date">
 *        &lt;term name="2010" count="532" />
 *        &lt;term name="2009" count="9982" />
 *      &lt;/field>
 *    &lt;/index>
 * &lt;/info>
 *
 * The list of indexes and fields to emit are controlled via URL
 * parameters <code>i</code> and <code>f</code> respectively.  E.g.
 * <code>http://localhost/info?i=foo&amp;f=date</code>
 *
 * If neither are given, then all indexes are listed, but no fields
 * are emitted.
 *
 * The <code>i</code> and <code>f</code> parameters are otherwise
 * independent and can be used in any combination.
 *
 * Only indexed fields can be emitted, and the only sensible ones are
 * those with a limited number of terms, otherwise the response is
 * enormous.  Good fields to emit are: date and type.
 */
public class DiagnosticServlet extends HttpServlet
{
  public static final Logger LOG = Logger.getLogger( DiagnosticServlet.class.getName() );

  public void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
  {
    Document doc  = new Document();
    Element  root = new Element( "info" );
    
    doc.addContent( root );

    Search search = (Search) this.getServletConfig()
      .getServletContext()
      .getAttribute( "tnh.search" );

    if ( search == null )
      {
        OpenSearchHelper.writeResponse( doc, response );
        
        return ;
      }

    Set<String> indexNames = new HashSet( Arrays.asList( ServletHelper.getParam( request, "i", search.searchers.keySet( ).toArray( QueryParameters.EMPTY_STRINGS ) ) ) );
    Set<String> fieldNames = new HashSet( Arrays.asList( ServletHelper.getParam( request, "f", QueryParameters.EMPTY_STRINGS ) ) );
    
    for ( String indexName : indexNames )
      {
        Searcher searcher = search.searchers.get( indexName );

        if ( searcher == null ) continue ;
        
        Element e = new Element( "searcher" );
        root.addContent( e );
        
        e.setAttribute( "name", indexName );
        e.setAttribute( "type", searcher.getClass().getCanonicalName() );
        
        try
          {
            IndexReader ir = ((IndexSearcher) searcher).getIndexReader();
            
            Element ise = new Element( "index" );
            e.addContent( ise );
            
            ise.setAttribute( "numDocs", Integer.toString( ir.numDocs() ) );
                        
            TermDocs termDocs = ir.termDocs( );
            for ( String fieldName : ir.getFieldNames( IndexReader.FieldOption.ALL ).toArray( QueryParameters.EMPTY_STRINGS ) )
              {
                // If this field is not requested, skip it.
                if ( ! fieldNames.contains( fieldName ) )
                  {
                    continue ;
                  }
                
                Element field = new Element( "field" );
                field.setAttribute( "name", fieldName );
                ise.addContent( field );

                // Iterate through the terms and for each term that
                // belongs to this field, count up the number of
                // documents containing that term and add it to the
                // XML Document.
                TermEnum termEnum = ir.terms( new Term( fieldName ) );
                do
                  {
                    Term term = termEnum.term();
                    
                    if ( term == null || ! fieldName.equals( term.field() ) ) continue ;
                    
                    termDocs.seek( termEnum );
                    
                    int c = 0;
                    for ( ; termDocs.next() ; c++ ) ;
                    
                    Element value = new Element( "term" );
                    value.setAttribute( "name",  term.text() );
                    value.setAttribute( "count", Integer.toString( c ) );
                    
                    field.addContent( value );
                  }
                while ( termEnum.next() );
              }
          }
        catch ( ClassCastException cce )
          {
          }
      }

    OpenSearchHelper.writeResponse( doc, response );
  }

}
