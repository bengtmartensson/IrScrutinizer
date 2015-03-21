<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <xsl:strip-space elements="parameter parameters remote command ccf" />

    <xsl:template match="/">
        <xsl:apply-templates select="remotes/remote"/>
    </xsl:template>

    <xsl:template match="remote">
        <xsl:text>Remote </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates select="commandSet/parameters"/>
        <xsl:text>
</xsl:text>
        <xsl:apply-templates select="commandSet/command"/>
        <xsl:text>

</xsl:text>
    </xsl:template>

    <xsl:template match="parameters">
        <xsl:text>(</xsl:text>
        <xsl:apply-templates select="@protocol"/>
        <xsl:apply-templates select="parameter"/>
        <xsl:text>)</xsl:text>
    </xsl:template>

    <xsl:template match="@protocol">
        <xsl:text>Protocol=</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="parameter">
        <xsl:text>, </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="@value"/>
    </xsl:template>

    <xsl:template match="command">
        <xsl:text>
</xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates select="parameters"/>
        <xsl:apply-templates select="@F"/>
        <xsl:text>
</xsl:text>
        <xsl:value-of select="ccf"/>
        <xsl:text>
</xsl:text>
    </xsl:template>
    
    <xsl:template match="@F">
        <xsl:text> (F=</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>)</xsl:text>
    </xsl:template>


</xsl:stylesheet>
