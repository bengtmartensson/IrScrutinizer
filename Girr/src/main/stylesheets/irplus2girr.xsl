<?xml version="1.0" encoding="UTF-8"?>

<!-- This style sheet translates the XML format from IrPlus (https://irplus-remote.github.io/) to Girr format.

    Author: Bengt Martensson
    License: GPL3
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:variable name="frequency">
        <xsl:if test="/irplus/device/@frequency">
            <xsl:value-of select="/irplus/device/@frequency"/>
        </xsl:if>
        <xsl:if test="not(/irplus/device/@frequency)">38000</xsl:if> <!-- LIRC default frequency -->
    </xsl:variable>

    <xsl:template match="/irplus">
        <remotes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" girrVersion="1.0"
                 xsi:noNamespaceSchemaLocation="http://www.harctoolbox.org/schemas/girr.xsd"
                 title="Conversion from IrPlus XML file">
            <xsl:apply-templates select="device"/>
        </remotes>
    </xsl:template>

    <!-- Default format -->
    <xsl:template match="device">
        <remote>
            <xsl:copy-of select="@manufacturer"/>
            <xsl:copy-of select="@model"/>
            <xsl:attribute name="name">
                <xsl:value-of select="@manufacturer"/>
                <xsl:text>_</xsl:text>
                <xsl:value-of select="@model"/>
            </xsl:attribute>
            <commandSet name="commandSet">
                <xsl:apply-templates select="button"/>
            </commandSet>
        </remote>
    </xsl:template>

    <xsl:template match="button">
        <xsl:comment>Format not implemented</xsl:comment>
    </xsl:template>

    <xsl:template match="button[../@format='PRONTO_HEX']">
        <command master="ccf">
            <xsl:attribute name="name">
                <xsl:value-of select="@label"/>
            </xsl:attribute>
            <ccf>
                <xsl:value-of select="substring-before(.,'|')"/>
                <xsl:if test="not(contains(.,'|'))">
                    <xsl:value-of select="."/>
                </xsl:if>
            </ccf>
        </command>
    </xsl:template>

    <xsl:template match="button[../@format='WINLIRC_RAW']">
        <command master="raw">
            <xsl:attribute name="name">
                <xsl:value-of select="@label"/>
            </xsl:attribute>
            <raw>
                <xsl:attribute name="frequency">
                    <xsl:value-of select="$frequency"/>
                </xsl:attribute>
                <repeat> <!-- only reasonable guess -->
                    <xsl:value-of select="."/>
                    <xsl:text> 30000</xsl:text> <!-- Lirc does not supply ending gaps!! -->
                </repeat>
            </raw>
        </command>
    </xsl:template>

</xsl:stylesheet>
