<?xml version="1.0" encoding="UTF-8"?>
<!-- Copying and distribution of this file, with or without modification,
     are permitted in any medium without royalty provided the copyright
     notice and this notice are preserved.  This file is offered as-is,
     without any warranty.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:html="http://www.w3.org/1999/xhtml">
    <xsl:output method="html"/>

    <xsl:template match="/">
        <html>
            <head>
                <title><xsl:value-of select="remotes/@title"/></title>
            </head>
            <body>
                <h1><xsl:value-of select="remotes/@title"/></h1>
                <xsl:apply-templates select="remotes/remote"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="remote">
        <h2>Remote
            <xsl:value-of select="@name"/>
        </h2>
        <xsl:apply-templates select="notes"/>
        <xsl:apply-templates select="html:img"/>
        <xsl:apply-templates select="commandSet"/>
    </xsl:template>

    <xsl:template match="notes">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="html:img">
        <img>
            <xsl:copy-of select="@*"/>
        </img>
    </xsl:template>

    <xsl:template match="command">
        <h3>
            <xsl:value-of select="@name"/>
            <xsl:apply-templates select="parameters"/>
            <xsl:apply-templates select="@F"/>
        </h3>
        <p>
            <xsl:value-of select="ccf"/>
        </p>

    </xsl:template>

    <xsl:template match="commandSet">
        <xsl:apply-templates select="parameters"/>
        <xsl:apply-templates select="command"/>
    </xsl:template>

        <xsl:template match="commandSet/parameters">
        <xsl:text>Common parameters: </xsl:text>
        <xsl:apply-templates select="@protocol"/>
        <xsl:apply-templates select="parameter"/>
    </xsl:template>

    <xsl:template match="command/parameters">
        (<xsl:apply-templates select="@protocol"/>
        <xsl:apply-templates select="parameter"/>)
    </xsl:template>

    <xsl:template match="@protocol">
        <xsl:text>Protocol=</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="@F">
        <xsl:text> F=</xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="parameter">
        <xsl:text> </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="@value"/>
    </xsl:template>

</xsl:stylesheet>
