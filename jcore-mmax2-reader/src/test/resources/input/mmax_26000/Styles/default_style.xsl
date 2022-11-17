<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
xmlns:mmax="org.eml.MMAX2.discourse.MMAX2DiscourseLoader"
xmlns:proteins="www.eml.org/NameSpaces/proteins"
xmlns:sentence="www.eml.org/NameSpaces/sentence">
<xsl:output method="text" indent="no" omit-xml-declaration="yes"/>
<xsl:strip-space elements="*"/>


<xsl:template match="words">

  <xsl:apply-templates/>

</xsl:template>

<xsl:template match="word">

  <xsl:value-of select="mmax:registerDiscourseElement(@id)"/>

  <xsl:apply-templates select="mmax:getStartedMarkables(@id)" mode="opening"/>

<xsl:value-of select="mmax:setDiscourseElementStart()"/>
<xsl:value-of select="mmax:startBold()"/>
   <xsl:apply-templates/>
<xsl:value-of select="mmax:endBold()"/>
  <xsl:value-of select="mmax:setDiscourseElementEnd()"/>

  <xsl:apply-templates select="mmax:getEndedMarkables(@id)" mode="closing"/>

<xsl:text> </xsl:text>

</xsl:template>

<xsl:template match="proteins:markable" mode="opening">
<xsl:value-of select="mmax:startBold()"/>
<xsl:value-of select="mmax:addLeftMarkableHandle(@mmax_level, @id, '[')"/>
<xsl:value-of select="mmax:endBold()"/>
</xsl:template>

<xsl:template match="proteins:markable" mode="closing">
<xsl:value-of select="mmax:startBold()"/>
<xsl:value-of select="mmax:addRightMarkableHandle(@mmax_level, @id, ']')"/>
<xsl:value-of select="mmax:endBold()"/>
</xsl:template>


<xsl:template match="sentence:markable" mode="closing">

<xsl:value-of select="mmax:startSubscript()"/>
 <xsl:text>
</xsl:text>
<xsl:value-of select="mmax:endSubscript()"/>

</xsl:template>


</xsl:stylesheet>


