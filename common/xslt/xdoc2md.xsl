<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>
    <xsl:strip-space elements="*"/>
    <xsl:preserve-space elements="source"/>

    <xsl:template match="/document">
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="header">
        <xsl:text># </xsl:text>
        <xsl:value-of select="title"/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="body">
        <xsl:apply-templates select="section"/>
    </xsl:template>

    <xsl:template match="section">
        <xsl:text>## </xsl:text>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="section[count(ancestor::section)=1]">
        <xsl:text>### </xsl:text>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="section[count(ancestor::section)=2]">
        <xsl:text>#### </xsl:text>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="section[count(ancestor::section)=3]">
        <xsl:text>#### </xsl:text>
        <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="title">
        <xsl:value-of select="."/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="p">
        <xsl:apply-templates select="node()"/>
        <xsl:text>

</xsl:text>
    </xsl:template>

    <xsl:template match="strong">
        <xsl:text>*</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>*</xsl:text>
    </xsl:template>

    <xsl:template match="source">
        <xsl:text>```
</xsl:text>
        <xsl:value-of select="text()"/>
        <xsl:text>
```
</xsl:text>
    </xsl:template>

    <xsl:template match="a">
        <xsl:text>[</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>](</xsl:text>
        <xsl:apply-templates select="@href"/>
        <xsl:text>)</xsl:text>
    </xsl:template>

    <xsl:template match="@href">
        <xsl:value-of select="."/>
    </xsl:template>

    <!-- Fix up local references; they won't be local any more -->
    <xsl:template match="@href[not(starts-with(., 'http'))]">
        <xsl:text>http://harctoolbox.org/</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="@href[substring(.,1,1)='#']">
        <xsl:text>http://harctoolbox.org/Girr.html</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="code">
        <xsl:text>`</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>`</xsl:text>
    </xsl:template>

    <xsl:template match="em">
        <xsl:text>_</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>_</xsl:text>
    </xsl:template>

    <xsl:template match="ul">
        <xsl:text>
</xsl:text>
        <xsl:apply-templates select="*"/>
        <xsl:text>

</xsl:text>
    </xsl:template>

    <xsl:template match="li">
        <xsl:text>
* </xsl:text>
        <xsl:apply-templates/>
        <xsl:text>
</xsl:text>
    </xsl:template>

</xsl:stylesheet>
