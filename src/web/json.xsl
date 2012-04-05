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

<xsl:template match="rss/channel">{
  "title" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="title" /></xsl:call-template>&quot;,
  "description" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="description" /></xsl:call-template>&quot;,
  "link" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="link" /></xsl:call-template>&quot;,
  "totalResults" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="opensearch:totalResults" /></xsl:call-template>&quot;,
  "startIndex" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="opensearch:startIndex" /></xsl:call-template>&quot;,
  "itemsPerPage" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="opensearch:itemsPerPage" /></xsl:call-template>&quot;,
  "query" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="query" /></xsl:call-template>&quot;,
  "responseTime" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="responseTime" /></xsl:call-template>&quot;,
  "urlParams" : [ <xsl:for-each select="archive:urlParams/archive:param">&quot;<xsl:value-of select="@name" />&quot; : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="@value" /></xsl:call-template>&quot;, </xsl:for-each> ],
  "items" : [ <xsl:for-each select="item">
  {
    "title" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="title" /></xsl:call-template>&quot;,
    "link" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="link" /></xsl:call-template>&quot;,
    "docId" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:docId" /></xsl:call-template>&quot;,
    "score" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:score" /></xsl:call-template>&quot;,
    "site" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:site" /></xsl:call-template>&quot;,
    "length" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:length" /></xsl:call-template>&quot;,
    "type" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:type" /></xsl:call-template>&quot;,
    "collection" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:collection" /></xsl:call-template>&quot;,
    "index" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="archive:index" /></xsl:call-template>&quot;,
    "date" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="date" /></xsl:call-template>&quot;,
    "description" : &quot;<xsl:call-template name="json-value"><xsl:with-param name="value" select="description" /></xsl:call-template>&quot;,
  } , </xsl:for-each> ]
}
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

<!-- Template to emit a JSON string value.  Just need to escape
     characters that are not allowed in JSON.
  -->
<xsl:template name="json-value">
  <xsl:param name="value" />
  <xsl:call-template name="string-replace-all">
    <xsl:with-param name="text">
      <xsl:call-template name="string-replace-all">
        <xsl:with-param name="text">
          <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text">
              <xsl:call-template name="string-replace-all">
                <xsl:with-param name="text" select="$value" />
                <xsl:with-param name="replace" select="'\'"     />
                <xsl:with-param name="by"      select="'\\'"   />
              </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="replace" select="'&quot;'"       />
            <xsl:with-param name="by"      select="'\&quot;'"  />
          </xsl:call-template>
        </xsl:with-param>
        <xsl:with-param name="replace" select="'&#x0A;'"     />
        <xsl:with-param name="by"      select="'\n'"   />
      </xsl:call-template>
    </xsl:with-param>
    <xsl:with-param name="replace" select="'&#x0D;'"       />
    <xsl:with-param name="by"      select="'\r'"  />
  </xsl:call-template>
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

<!-- Template to emit a date in YYYY/MM/DD format 
  -->
<xsl:template match="archive:date" >
  <xsl:value-of select="substring(.,1,4)" /><xsl:text>-</xsl:text><xsl:value-of select="substring(.,5,2)" /><xsl:text>-</xsl:text><xsl:value-of select="substring(.,7,2)" /><xsl:text> </xsl:text>
</xsl:template>

</xsl:stylesheet>
