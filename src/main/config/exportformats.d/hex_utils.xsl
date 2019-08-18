<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:variable name="hex-characters">0123456789ABCDEF</xsl:variable>

     <xsl:template name="four-hex-digits">
        <xsl:param name="value"/>
        <xsl:call-template name="two-hex-digits">
            <xsl:with-param name="value" select="floor($value div 256)"/>
        </xsl:call-template>
        <xsl:call-template name="two-hex-digits">
            <xsl:with-param name="value" select="$value mod 256"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="two-hex-digits">
        <xsl:param name="value"/>
        <xsl:call-template name="hex-digit">
            <xsl:with-param name="value" select="floor($value div 16)"/>
        </xsl:call-template>
        <xsl:call-template name="hex-digit">
            <xsl:with-param name="value" select="$value mod 16"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="hex-digit">
        <xsl:param name="value"/>
        <xsl:value-of select="substring($hex-characters, $value + 1, 1)"/>
    </xsl:template>

</xsl:stylesheet>
