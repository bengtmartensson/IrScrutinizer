<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2023 Bengt Martensson

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->

<xsl:stylesheet xmlns:girr="http://www.harctoolbox.org/Girr"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:java="http://xml.apache.org/xalan/java"
                xmlns:cidentifierfactory="http://xml.apache.org/xalan/java/org.harctoolbox.irscrutinizer.exporter.CIdentifierFactory"
                version="1.0">
    <xsl:variable name="cIdentifierFactory" select="cidentifierfactory:new()"/>

    <xsl:template match="girr:remote" mode="using">
        <xsl:apply-templates select="//girr:command" mode="using"/>
    </xsl:template>

    <xsl:template match="girr:command" mode="definition">
        <xsl:text>// Command #</xsl:text>
        <xsl:value-of select="position()"/>
        <xsl:text>: </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>
</xsl:text>
        <xsl:apply-templates select="girr:parameters"/>
        <xsl:apply-templates select="girr:raw[2]" mode="togglewarning"/>
        <xsl:apply-templates select="girr:raw[1]" mode="definition"/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:parameters">
        <xsl:text>// Protocol: </xsl:text>
        <xsl:value-of select="@protocol"/>
        <xsl:text>, Parameters:</xsl:text>
        <xsl:apply-templates select="girr:parameter"/>
        <xsl:text>
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:parameter">
        <xsl:text> </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>=</xsl:text>
        <xsl:value-of select="@value"/>
    </xsl:template>

    <xsl:template match="girr:raw" mode="togglewarning">
        <xsl:text>// Warning: this signal has toggles, i.e. several different raw versions.
// Only the first one is given here.
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:raw" mode="definition">
        <xsl:apply-templates select="*" mode="definition"/>
    </xsl:template>

    <xsl:template match="girr:intro|girr:repeat" mode="definition">
        <xsl:text>const microseconds_t </xsl:text>
        <xsl:value-of select="name(.)"/>
        <xsl:text>_</xsl:text>
        <xsl:value-of select="cidentifierfactory:mkCIdentifier($cIdentifierFactory,
                                                               string(../../@name),
                                                               count(../../preceding-sibling::girr:command))"/>
        <xsl:text>[] LOCATION = { </xsl:text>
        <xsl:apply-templates select="*"/>
        <xsl:text> };
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:flash">
        <xsl:value-of select="."/>
        <xsl:text>U, </xsl:text>
    </xsl:template>
    <xsl:template match="girr:gap">
        <xsl:value-of select="."/>
        <xsl:text>U, </xsl:text>
    </xsl:template>
    <xsl:template match="girr:gap[position()=last()]">
        <xsl:if test=". &gt; 65535">
            <xsl:text>65535U</xsl:text>
        </xsl:if>
        <xsl:if test=". &lt;= 65535">
            <xsl:value-of select="."/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="girr:command" mode="using">
        <xsl:text>    case </xsl:text>
        <xsl:value-of select="position()"/>
        <xsl:text>:
</xsl:text>
    <xsl:text>        sendRaw(</xsl:text>
        <xsl:apply-templates select="girr:raw[1]" mode="arg"/>
        <xsl:value-of select="girr:raw[1]/@frequency"/>
        <xsl:text>UL, times);
        break;
</xsl:text>
    </xsl:template>

    <xsl:template match="girr:raw" mode="arg">
        <xsl:if test="not(girr:intro)">
            <xsl:text>NULL, 0U, </xsl:text>
        </xsl:if>
        <xsl:apply-templates select="*" mode="arg"/>
        <xsl:if test="not(girr:repeat)">
            <xsl:text>NULL, 0U, </xsl:text>
        </xsl:if>
    </xsl:template>

    <xsl:template match="girr:intro|girr:repeat" mode="arg">
        <xsl:value-of select="name(.)"/>
        <xsl:text>_</xsl:text>
        <xsl:value-of select="cidentifierfactory:mkCIdentifier($cIdentifierFactory,
                                                                string(../../@name),
                                                                count(../../preceding-sibling::girr:command))"/>
        <xsl:text>, </xsl:text>
        <xsl:value-of select="count(*)"/>
        <xsl:text>U, </xsl:text>
    </xsl:template>

    <!-- just to be on the safe side -->
    <xsl:template match="girr:ending">
        <xsl:comment>Warning: ending sequence in command <xsl:value-of select="../../@name"/> was ignored.</xsl:comment>
    </xsl:template>
</xsl:stylesheet>
