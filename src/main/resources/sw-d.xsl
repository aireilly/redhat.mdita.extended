<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="*[contains(@class,' sw-d/filepath ')]" name="topic.sw-d.filepath">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' sw-d/msgph ')]" name="topic.sw-d.msgph">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' sw-d/userinput ')]" name="topic.sw-d.userinput">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' sw-d/systemoutput ')]" name="topic.sw-d.systemoutput">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' sw-d/cmdname ')]" name="topic.sw-d.cmdname">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' sw-d/msgnum ')]" name="topic.sw-d.msgnum">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

  <xsl:template match="*[contains(@class,' sw-d/varname ')]" name="topic.sw-d.varname">
    <code>
      <xsl:call-template name="commonattributes"/>
      <xsl:call-template name="setidaname"/>
      <xsl:apply-templates/>
    </code>
  </xsl:template>

</xsl:stylesheet>
