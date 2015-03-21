<?xml version="1.0" encoding="UTF-8"?>

<!-- This style sheet translates the XML format from IrpMaster and IrMaster to Girr format.

    Author: Bengt Martensson
    License: GPL3
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/protocol">
        <remotes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" girrVersion="1.0"
                 xsi:noNamespaceSchemaLocation="girr.xsd"
                 title="Conversion from Ir(p)master XML file">
            <remote name="unnamedRemote">
                <commandSet name="commandSet">
                    <parameters>
                        <xsl:attribute name="protocol">
                            <xsl:value-of select="@name"/>
                        </xsl:attribute>
                    </parameters>
                    <xsl:apply-templates select="signal"/>
                </commandSet>
            </remote>
        </remotes>
    </xsl:template>

    <xsl:template match="signal">
        <command master="parameters">
            <xsl:attribute name="name">
                <xsl:value-of select="../@name"/>
                <xsl:text>_</xsl:text>
                <xsl:value-of select="@D"/>
                <xsl:value-of select="@S"/>
                <xsl:value-of select="@F"/>
                <xsl:value-of select="@T"/>
            </xsl:attribute>
            <xsl:apply-templates select="raw"/>
            <xsl:apply-templates select="pronto"/>
            <xsl:apply-templates select="uei-learned"/>
        </command>
    </xsl:template>

    <xsl:template match="raw">
        <raw>
            <xsl:attribute name="frequency">
                <xsl:value-of select="../../@frequency"/>
            </xsl:attribute>
            <xsl:copy-of select="intro"/>
            <xsl:copy-of select="repeat"/>
            <xsl:copy-of select="ending"/>
        </raw>
    </xsl:template>

    <xsl:template match="pronto">
        <ccf>
            <xsl:value-of select="."/>
        </ccf>
    </xsl:template>

    <xsl:template match="uei-learned">
        <format name="uei-learned">
            <xsl:value-of select="."/>
        </format>
    </xsl:template>

</xsl:stylesheet>
