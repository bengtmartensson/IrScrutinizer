<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:girr="http://www.harctoolbox.org/Girr" version="1.0">

    <xsl:template match="girr:flash|girr:gap" mode="broadlink">
         <xsl:if test="position()!=last()">
         <xsl:call-template name="broadlink-number">
                 <xsl:with-param name="value" select="round(number(.) div 30.517578125)"/>
         </xsl:call-template>
         </xsl:if>
         <xsl:if test="position()=last()">
             <xsl:text>000D05</xsl:text>
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
</xsl:stylesheet>