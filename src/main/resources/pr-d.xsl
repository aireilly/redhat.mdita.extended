<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:import href="syntax-braces.xsl"/>
  
  <xsl:template match="*[contains(@class,' pr-d/codeph ')]" name="topic.pr-d.codeph">
   <code>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
    </code>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/kwd ')]" name="topic.pr-d.kwd">
   <strong>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </strong>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/var ')]" name="topic.pr-d.var">
   <emph>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </emph>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/synph ')]" name="topic.pr-d.synph">
   <code>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </code>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/oper ')]" name="topic.pr-d.oper">
   <span>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </span>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/delim ')]" name="topic.pr-d.delim">
   <span>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </span>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/sep ')]" name="topic.pr-d.sep">
   <span>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </span>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/repsep ')]" name="topic.pr-d.repsep">
   <span>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </span>
  </xsl:template>
  
  <xsl:template match="*[contains(@class,' pr-d/option ')]" name="topic.pr-d.option">
   <code>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' pr-d/parmname ')]" name="topic.pr-d.parmname">
   <code>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' pr-d/apiname ')]" name="topic.pr-d.apiname">
   <code>
    <xsl:call-template name="commonattributes"/>
    <xsl:call-template name="setidaname"/>
    <xsl:apply-templates/>
   </code>
  </xsl:template>

</xsl:stylesheet>
