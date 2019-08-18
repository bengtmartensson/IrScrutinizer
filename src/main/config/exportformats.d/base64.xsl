<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright (C) 2019 Bengt Martensson

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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:variable name="hex-chars">0123456789ABCDEF</xsl:variable>
    <xsl:variable name="base64-chars">ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/</xsl:variable>

    <xsl:template name="parse-hex-digit">
        <xsl:param name="ch"/>
        <xsl:value-of select="string-length(substring-before($hex-chars, translate($ch, 'abcdef', 'ABCDEF')))"/>
    </xsl:template>

    <xsl:template name="base64-char">
        <xsl:param name="c"/>
        <xsl:value-of select="substring($base64-chars, $c + 1, 1)"/>
    </xsl:template>

    <xsl:template name="parse-hex-byte">
        <xsl:param name="byte"/>
        <xsl:if test="string-length($byte)=0">
            <xsl:value-of select="0"/>
        </xsl:if>
        <xsl:if test="string-length($byte)&gt;0">
            <xsl:variable name="hi">
                <xsl:call-template name="parse-hex-digit">
                    <xsl:with-param name="ch" select="substring($byte, 1, 1)"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="lo">
                <xsl:call-template name="parse-hex-digit">
                    <xsl:with-param name="ch" select="substring($byte, 2, 1)"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:value-of select="16*$hi + $lo"/>
        </xsl:if>
    </xsl:template>

    <xsl:template name="base64">
        <xsl:param name="data"/>
        <xsl:if test="string-length($data) &gt; 0">
            <xsl:call-template name="base64-24bits">
                <xsl:with-param name="chunk">
                    <xsl:value-of select="substring($data, 1, 6)"/>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="base64">
                <xsl:with-param name="data">
                    <xsl:value-of select="substring($data, 7)"/>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template name="base64-24bits">
        <xsl:param name="chunk"/>
        <xsl:variable name="first">
            <xsl:call-template name="parse-hex-byte">
                <xsl:with-param name="byte" select="substring($chunk, 1, 2)"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="second">
            <xsl:call-template name="parse-hex-byte">
                <xsl:with-param name="byte" select="substring($chunk, 3, 2)"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="third">
            <xsl:call-template name="parse-hex-byte">
                <xsl:with-param name="byte" select="substring($chunk, 5, 2)"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="n" select="65536 * $first + 256 * $second + $third"/>
        <xsl:variable name="c1" select="floor($n div 262144)"/>
        <xsl:variable name="c2" select="floor(($n mod 262144) div 4096)"/>
        <xsl:variable name="c3" select="floor(($n mod 4096) div 64)"/>
        <xsl:variable name="c4" select="$n mod 64"/>

        <xsl:call-template name="base64-char">
            <xsl:with-param name="c" select="$c1"/>
        </xsl:call-template>
        <xsl:call-template name="base64-char">
            <xsl:with-param name="c" select="$c2"/>
        </xsl:call-template>
        <xsl:if test="string-length($chunk)=2">
            <xsl:text>==</xsl:text>
        </xsl:if>
        <xsl:if test="string-length($chunk)&gt;2">
            <xsl:call-template name="base64-char">
                <xsl:with-param name="c" select="$c3"/>
            </xsl:call-template>
            <xsl:if test="string-length($chunk)=4">
                <xsl:text>=</xsl:text>
            </xsl:if>
            <xsl:if test="string-length($chunk)&gt;4">
                <xsl:call-template name="base64-char">
                    <xsl:with-param name="c" select="$c4"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
