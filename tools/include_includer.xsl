<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:girr="http://www.harctoolbox.org/Girr"
                     xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:java="http://xml.apache.org/xalan/java"
                     xmlns:cidentifierfactory="http://xml.apache.org/xalan/java/org.harctoolbox.irscrutinizer.exporter.CIdentifierFactory"
                     version="1.0">

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="xsl:import|xsl:include">
        <xsl:comment>
            <xsl:text> ******************** Content of </xsl:text>
            <xsl:value-of select="@href"/>
            <xsl:text> ******************** </xsl:text>
        </xsl:comment>
        <xsl:apply-templates select="document(@href)/xsl:stylesheet/node()"/>
        <xsl:text>
    </xsl:text>
        <xsl:comment>
            <xsl:text> ******************** End of </xsl:text>
            <xsl:value-of select="@href"/>
            <xsl:text> ******************** </xsl:text>
        </xsl:comment>
        <xsl:text>
        </xsl:text>
    </xsl:template>

</xsl:stylesheet>
