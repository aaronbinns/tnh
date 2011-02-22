<?xml version="1.0" encoding="utf-8" ?> 
<!--
 Copyright 2010 Internet Archive
 
 Licensed under the Apache License, Version 2.0 (the "License"); you
 may not use this file except in compliance with the License. You
 may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied. See the License for the specific language governing
 permissions and limitations under the License.
-->
<xsl:stylesheet
     version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
     xmlns:opensearch="http://a9.com/-/spec/opensearchrss/1.0/" 
     xmlns:archive="http://web.archive.org/-/spec/opensearchrss/1.0/"
>
<xsl:output method="text" />
<xsl:variable name="wayback" select="'http://waybackmachine.org'" />

<xsl:template match="rss/channel">
  &lt;html xmlns="http://www.w3.org/1999/xhtml">
  &lt;head>
  &lt;title><xsl:value-of select="title" />&lt;/title>
  &lt;link rel="shortcut icon" href="favicon.ico" />
  &lt;style media="all" lang="en" type="text/css">
  body
  {
    padding     : 20px;
    margin      : 0;
    font-family : Verdana; sans-serif;
    font-size   : 9pt;
    color : #000000;
    background-color: #ffffff;
  }
  .pageTitle
  {
    font-size   : 125% ;
    font-weight : bold ;
    text-align  : center ;
    padding-bottom : 2em ;
  }
  .searchForm
  {
    margin : 20px 0 5px 0;
    padding-bottom : 0px;
    border-bottom : 1px solid black;
  }
  .searchResult
  {
    margin  : 0;
    padding : 0;
  }
  .searchResult h1 
  {
    margin  : 0 0 5px 0 ;
    padding : 0 ;
    font-size : 120%;
  }
  .searchResult .details
  {
    font-size: 80%;
    color: green;
  }
  .searchResult .dates
  {
    font-size: 80%;
  }
  .searchResult .dates a
  {
    color: #3366cc;
  }
  form#searchForm
  {
    margin : 0; padding: 0 0 10px 0;
  }
  .searchFields
  {
    padding : 3px 0;
  }
  .searchFields input
  {
    margin  : 0 0 0 15px;
    padding : 0;
  }
  input#query
  {
    margin : 0;
  }
  ol
  {
    margin  : 5px 0 0 0;
    padding : 0 0 0 2em;
  }
  ol li
  {
    margin : 0 0 15px 0;
  }
  &lt;/style>
  &lt;/head>
  &lt;body>
    <!-- Page header: title and search form -->
    &lt;div class="pageTitle" >
      Archival Search Sample XSLT
    &lt;/div>
    &lt;div>
      This simple XSLT demonstrates the transformation of OpenSearch XML results into a fully-functional, human-friendly HTML search page.  No JSP needed.
    &lt;/div>
    &lt;div class="searchForm">
      &lt;form id="searchForm" name="searchForm" method="get">
        &lt;span class="searchFields">
        Search for 
        &lt;input id="q" name="q" type="text" size="40" value=&quot;<xsl:call-template name="string-replace-all">
           <xsl:with-param name="text"    select="archive:query" />
           <xsl:with-param name="replace" select="'&quot;'"       />
           <xsl:with-param name="by"      select="'&amp;quot;'"  />
         </xsl:call-template>&quot; />
        <!-- Create hidden form fields for the rest of the URL parameters -->
        <xsl:for-each select="archive:urlParams/archive:param[@name!='p' and @name!='q' and @name!='t']">
          &lt;input type="hidden" name="<xsl:value-of select="@name" />" value="<xsl:value-of select="@value" />" />
        </xsl:for-each>
        &lt;input type="submit" value="Search"/>
        &lt;span style="font-size: 80%; margin: 0;">
        &lt;input type="checkbox" name="t" value="text/html" <xsl:if test="archive:urlParams/archive:param[@name='t' and @value='text/html']"><xsl:text>checked="true"</xsl:text></xsl:if> > HTML&lt;/input> 
        &lt;input type="checkbox" name="t" value="application/pdf" <xsl:if test="archive:urlParams/archive:param[@name='t' and @value='application/pdf']"><xsl:text>checked="true"</xsl:text></xsl:if>> PDF&lt;/input> 
        &lt;input type="checkbox" name="t" value="application/msword" <xsl:if test="archive:urlParams/archive:param[@name='t' and @value='application/msword']"><xsl:text>checked="true"</xsl:text></xsl:if>> MS Word&lt;/input>
        &lt;/span>
        &lt;/span>
      &lt;/form>
    &lt;/div>
    &lt;div style="font-size: 8pt; margin:0; padding:0 0 0.5em 0;">Results <xsl:value-of select="opensearch:startIndex + 1" />-<xsl:choose>
          <xsl:when test="(opensearch:startIndex + opensearch:itemsPerPage) &lt; opensearch:totalResults"><xsl:value-of select="format-number( opensearch:startIndex + opensearch:itemsPerPage, '###,###' )" /></xsl:when>
          <xsl:otherwise><xsl:value-of select="format-number( opensearch:totalResults, '###,###' )" /></xsl:otherwise> 
        </xsl:choose>
          of about <xsl:value-of select="format-number( opensearch:totalResults, '###,###' )" /> &lt;span style="margin-left: 0;">(<xsl:value-of select="archive:responseTime" /> seconds)&lt;/span>
    <xsl:if test="archive:urlParams/archive:param[@name='s']">
      <xsl:text>from &lt;span style=&quot;color: green;&quot;></xsl:text>
      <xsl:value-of select="archive:urlParams/archive:param[@name='s']/@value" />
      <xsl:text>&lt;/span> (return to &lt;a href=&quot;?</xsl:text>
        <xsl:for-each select="archive:urlParams/archive:param[@name!='s' and @name!='h']">
          <xsl:value-of select="@name" /><xsl:text>=</xsl:text><xsl:value-of select="@value" />
          <xsl:text>&amp;</xsl:text>
        </xsl:for-each>
      <xsl:text>&quot;>all results&lt;/a>)</xsl:text>
    </xsl:if>
    &lt;/div>
    <!-- Search results -->
    &lt;ol start="<xsl:value-of select="opensearch:startIndex + 1"/>">
      <xsl:apply-templates select="item" />
    &lt;/ol>
    <!-- Generate list of page links -->
    &lt;center>
      <xsl:call-template name="pageLinks">
        <xsl:with-param name="labelPrevious" select="'&#171;'" />
        <xsl:with-param name="labelNext"     select="'&#187;'" />
      </xsl:call-template>
    &lt;/center>
  &lt;/body>
&lt;/html>
</xsl:template>


<!-- ======================================================================
     XSLT template/fuction library.
     
     The idea is that the above xhtml code is what most users will
     modify to tailor to their own look and feel.  The stuff below
     implements the core logic for generating results lists, page
     links, etc.

     Hopefully web developers will be able to easily edit the above
     xhtml and css and won't have to change the below.
     ====================================================================== -->

<!-- Template to emit a search result as an HTML list item (<li/>).
  -->
<xsl:template match="item">
  <!-- If there is a title, use it, otherwise use the URL. -->
  <xsl:variable name="title">
    <xsl:choose>
      <xsl:when test="normalize-space(title)"><xsl:value-of select="title" /></xsl:when>
      <xsl:otherwise><xsl:value-of select="link" /></xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <!-- Define substring to use in constructing the Wayback URL depending on the presence of a collection. -->
  <xsl:variable name="collection">
    <!-- TODO: Uncomment this if your deployment uses collection-based Wayback deployment -->
    <!--
    <xsl:choose>
      <xsl:when test="normalize-space(archive:collection)"><xsl:value-of select="concat('/', archive:collection)" /></xsl:when>
      <xsl:when test="normalize-space(archive:collection) = '' and normalize-space(archive:index)"><xsl:value-of select="concat('/', archive:index)" /></xsl:when>
      <xsl:otherwise><xsl:value-of select="''" /></xsl:otherwise>
    </xsl:choose>
    -->
  </xsl:variable>
  &lt;li>
  &lt;div class="searchResult">
    &lt;h1>
    &lt;a href=&quot;<xsl:value-of select="concat( $wayback, $collection, '/', date, '/', link )" /> &quot;><xsl:value-of select="$title" />&lt;/a>
    &lt;/h1>
    &lt;div>
      <xsl:value-of select="description" />
    &lt;/div>
    &lt;div class="details">
      <xsl:value-of select="link" /> - <xsl:value-of select="round( archive:length div 1024 )"/>k - <xsl:value-of select="archive:type" />
    &lt;/div>
    &lt;div class="dates">
      <xsl:text>&lt;a</xsl:text><xsl:text> href=&quot;</xsl:text><xsl:value-of select="concat( $wayback, $collection, '/*/', link )" /><xsl:text>&quot;</xsl:text><xsl:text>></xsl:text>
        <xsl:text>All versions (</xsl:text><xsl:value-of select="count( date )" /><xsl:text>)</xsl:text>
      <xsl:text>&lt;/a></xsl:text>
      <xsl:if test="not(../archive:urlParams/archive:param[@name='s'])">
      <xsl:text> - </xsl:text>
      <xsl:text>&lt;a</xsl:text><xsl:text> href=&quot;</xsl:text>
        <xsl:value-of select="concat( '?q=',../archive:query,'&amp;s=',archive:site,'&amp;h=0' )"/>
        <xsl:for-each select="../archive:urlParams/archive:param[@name !='q' and @name!='s' and @name!='h']">
          <xsl:text>&amp;</xsl:text>
          <xsl:value-of select="@name" /><xsl:text>=</xsl:text><xsl:value-of select="@value" />
        </xsl:for-each><xsl:text>&quot;</xsl:text><xsl:text>></xsl:text>
        <xsl:text>More from </xsl:text><xsl:value-of select="archive:site" />
      <xsl:text>&lt;/a></xsl:text>
      </xsl:if>
    &lt;/div>
  &lt;/div>
  &lt;/li>
</xsl:template>

<!-- Template to emit a date in YYYY/MM/DD format 
  -->
<xsl:template match="archive:date" >
  <xsl:value-of select="substring(.,1,4)" /><xsl:text>-</xsl:text><xsl:value-of select="substring(.,5,2)" /><xsl:text>-</xsl:text><xsl:value-of select="substring(.,7,2)" /><xsl:text> </xsl:text>
</xsl:template>

<!-- Template to emit a list of numbered page links, *including*
     "previous" and "next" links on either end, using the given labels.
     Parameters:
       labelPrevious   Link text for "previous page" link
       labelNext       Link text for "next page" link
  -->
<xsl:template name="pageLinks">
  <xsl:param name="labelPrevious" />
  <xsl:param name="labelNext"     />
  <xsl:variable name="startPage" select="floor(opensearch:startIndex   div opensearch:itemsPerPage) + 1" />
  <xsl:variable name="lastPage"  select="floor(opensearch:totalResults div opensearch:itemsPerPage) + 1" />
  <!-- If we are on any page past the first, emit a "previous" link -->
  <xsl:if test="$startPage != 1">
    <xsl:call-template name="pageLink">
      <xsl:with-param name="pageNum"  select="$startPage - 1" />
      <xsl:with-param name="linkText" select="$labelPrevious" />
    </xsl:call-template>
    <xsl:text> </xsl:text>
  </xsl:if>
  <!-- Now, emit numbered page links -->
  <xsl:choose>
    <!-- We are on pages 1-10.  Emit links  -->
    <xsl:when test="$startPage &lt; 11">
      <xsl:choose>
        <xsl:when test="$lastPage &lt; 21">
          <xsl:call-template name="numberedPageLinks" >
            <xsl:with-param name="begin"   select="1"  />
            <xsl:with-param name="end"     select="$lastPage + 1" />
            <xsl:with-param name="current" select="$startPage" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="numberedPageLinks" >
            <xsl:with-param name="begin"   select="1"  />
            <xsl:with-param name="end"     select="21" />
            <xsl:with-param name="current" select="$startPage" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <!-- We are past page 10, but not to the last page yet.  Emit links for 10 pages before and 10 pages after -->
    <xsl:when test="$startPage &lt; $lastPage">
      <xsl:choose>
        <xsl:when test="$lastPage &lt; ($startPage + 11)">
          <xsl:call-template name="numberedPageLinks" >
            <xsl:with-param name="begin"   select="$startPage - 10" />
            <xsl:with-param name="end"     select="$lastPage  +  1" />
            <xsl:with-param name="current" select="$startPage"      />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="numberedPageLinks" >
            <xsl:with-param name="begin"   select="$startPage - 10" />
            <xsl:with-param name="end"     select="$startPage + 11" />
            <xsl:with-param name="current" select="$startPage"      />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <!-- This covers the case where we are on (or past) the last page -->
    <xsl:otherwise>
      <xsl:call-template name="numberedPageLinks" >
        <xsl:with-param name="begin"   select="$startPage - 10" />
        <xsl:with-param name="end"     select="$lastPage  + 1"  />
        <xsl:with-param name="current" select="$startPage"      />
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
  <!-- Lastly, emit a "next" link. -->
  <xsl:text> </xsl:text>
  <xsl:if test="$startPage &lt; $lastPage">
    <xsl:call-template name="pageLink">
      <xsl:with-param name="pageNum"  select="$startPage + 1" />
      <xsl:with-param name="linkText" select="$labelNext" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- Template to emit a list of numbered links to results pages. 
     Parameters:
       begin    starting # inclusive
       end      ending # exclusive
       current  the current page, don't emit a link
  -->
<xsl:template name="numberedPageLinks">
  <xsl:param name="begin"   />
  <xsl:param name="end"     />
  <xsl:param name="current" />
  <xsl:if test="$begin &lt; $end">
    <xsl:choose>
      <xsl:when test="$begin = $current" >
        <xsl:value-of select="$current" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="pageLink" >
          <xsl:with-param name="pageNum"  select="$begin"  />
          <xsl:with-param name="linkText" select="$begin"  />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text> </xsl:text>
    <xsl:call-template name="numberedPageLinks">
      <xsl:with-param name="begin"   select="$begin + 1" />
      <xsl:with-param name="end"     select="$end"       />
      <xsl:with-param name="current" select="$current"   />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- Template to emit a single page link.  All of the URL parameters
     listed in the OpenSearch results are included in the link.
     Parmeters:
       pageNum    page number of the link
       linkText   text of the link
  -->
<xsl:template name="pageLink">
  <xsl:param name="pageNum"  />
  <xsl:param name="linkText" />
  <xsl:text>&lt;a</xsl:text>
    <xsl:text> href=&quot;?</xsl:text>
      <xsl:for-each select="archive:urlParams/archive:param[@name!='p']">
        <xsl:value-of select="@name" /><xsl:text>=</xsl:text><xsl:value-of select="@value" />
        <xsl:text>&amp;</xsl:text>
      </xsl:for-each>
      <xsl:text>p=</xsl:text><xsl:value-of select="($pageNum -1) * opensearch:itemsPerPage" />
    <xsl:text>&quot; ></xsl:text>
    <xsl:value-of select="$linkText" />
  <xsl:text>&lt;/a></xsl:text>
</xsl:template>

<xsl:template name="string-replace-all">
    <xsl:param name="text" />
    <xsl:param name="replace" />
    <xsl:param name="by" />
    <xsl:choose>
      <xsl:when test="contains($text, $replace)">
        <xsl:value-of select="substring-before($text,$replace)" />
        <xsl:value-of select="$by" />
        <xsl:call-template name="string-replace-all">
          <xsl:with-param name="text"
          select="substring-after($text,$replace)" />
          <xsl:with-param name="replace" select="$replace" />
          <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>

