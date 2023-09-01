<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:girr="http://www.harctoolbox.org/Girr" version="1.0">

    <xsl:include href="base64.xsl"/>
    <xsl:include href="hex_utils.xsl"/>

    <xsl:param name="TICK">32.84</xsl:param>
    <xsl:param name="IR_TOKEN">26</xsl:param>
    <xsl:param name="RF_433_TOKEN">B2</xsl:param>
    <xsl:param name="IR_ENDING_TOKEN">000d05</xsl:param>
    <xsl:param name="RF_433_ENDING_TOKEN">000181</xsl:param>

    <xsl:template match="girr:flash|girr:gap" mode="broadlink">
        <xsl:if test="position()!=last()">
            <xsl:call-template name="broadlink-number">
                <xsl:with-param name="value" select="round(number(.) div $TICK)"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="position()=last()">
            <xsl:if test="number(../../@frequency)=0">
                <xsl:value-of select="$RF_433_ENDING_TOKEN"/>
            </xsl:if>
            <xsl:if test="not(number(../../@frequency)=0)">
                <xsl:value-of select="$IR_ENDING_TOKEN"/>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template name="broadlink-number">
        <xsl:param name="value"/>
        <xsl:if test="$value &gt; 255">
            <xsl:text>00</xsl:text>
            <xsl:call-template name="broadlink-number">
                <xsl:with-param name="value" select="floor($value div 256)"/>
            </xsl:call-template>
        </xsl:if>
        <xsl:call-template name="two-hex-digits">
            <xsl:with-param name="value" select="$value mod 256"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="girr:raw" mode="broadlink">
        <xsl:variable name="durationdata">
            <xsl:apply-templates select="*/*" mode="broadlink"/> <!-- concatenate everything into one single sequence -->
        </xsl:variable>
        <xsl:call-template name="base64">
            <xsl:with-param name="data">
                <xsl:if test="number(@frequency)=0">
                    <xsl:value-of select="$RF_433_TOKEN"/>
                </xsl:if>
                <xsl:if test="not(number(@frequency)=0)">
                    <xsl:value-of select="$IR_TOKEN"/>
                </xsl:if>
                <xsl:text>05</xsl:text>
                <xsl:call-template name="two-hex-digits">
                    <xsl:with-param name="value" select="((string-length($durationdata) div 2) + 4) mod 256"/>
                </xsl:call-template>
                <xsl:call-template name="two-hex-digits">
                    <xsl:with-param name="value" select="((string-length($durationdata) div 2) + 4) div 256"/>
                </xsl:call-template>
                <xsl:value-of select="$durationdata"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

</xsl:stylesheet>