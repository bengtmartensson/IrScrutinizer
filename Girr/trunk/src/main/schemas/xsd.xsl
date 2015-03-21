<!DOCTYPE xsl:stylesheet [
<!--*
<!DOCTYPE xsl:stylesheet PUBLIC 'http://www.w3.org/1999/XSL/Transform'
      '../../People/cmsmcq/lib/xslt10.dtd' [ 
*-->
<!ATTLIST xsl:stylesheet 
          xmlns:xsl CDATA "http://www.w3.org/1999/XSL/Transform" 
          xmlns:xsd CDATA #IMPLIED
          xmlns:xhtml CDATA #IMPLIED
>
<!ATTLIST xsl:text
          xmlns:xsl CDATA "http://www.w3.org/1999/XSL/Transform" 
>

<!ENTITY copy   "&#169;" ><!--=copyright sign-->
<!ENTITY reg    "&#174;" ><!--/circledR =registered sign-->
<!ENTITY rarr   "&#x2192;" ><!--/rightarrow /to A: =rightward arrow-->

<!ENTITY nl "&#xA;">
<!ENTITY lsquo  "&#x2018;" ><!--=single quotation mark, left-->
<!ENTITY rsquo  "&#x2019;" ><!--=single quotation mark, right-->
<!ENTITY ldquo  "&#x201C;" ><!--=double quotation mark, left-->
<!ENTITY rdquo  "&#x201D;" ><!--=double quotation mark, right-->

]>
<xsl:stylesheet version="1.0"
     xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
     xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
     xmlns:xhtml="http://www.w3.org/1999/xhtml"
>

 <!--* xsd.xsl:  format an XSD schema document for simple display in a Web browser.
     * http://www.w3.org/XML/2004/xml-schema-test-suite/xsd.xsl
     * 
     * Copyright © 2008-2009 World Wide Web Consortium, (Massachusetts
     * Institute of Technology, European Research Consortium for
     * Informatics and Mathematics, Keio University). All Rights
     * Reserved. This work is distributed under the W3C® Software
     * License [1] in the hope that it will be useful, but WITHOUT ANY
     * WARRANTY; without even the implied warranty of MERCHANTABILITY or
     * FITNESS FOR A PARTICULAR PURPOSE. 
     * 
     * [1] http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231
     *
     *-->

 <!--* Revisions:
     * 2012-05-03 : CMSMcQ : reissue under Software License, not document
     *                       license
     * 2009-01-21 : CMSMcQ : wrap start-tags only when necessary
     * 2009-01-20 : CMSMcQ : wrap start-tags
     * 2008-12-19 : CMSMcQ : add toc for schema documents with more than
     *                       five children of xsd:schema
     * 2008-12-18 : CMSMcQ : fix problems with text breaking
     *                       add rule for top-level attribute groups
     * 2008-09-27 : CMSMcQ : made first version of this stylesheet
     *-->

 <xsl:output method="html" indent="yes"/>

 <xsl:param name="line-wrap-length" select="60"/>
 <xsl:param name="ind-depth" select="6"/>
 <xsl:param name="additional-indent" select="substring(
  '                                                                                ',
  1,$ind-depth)"/>

 <xsl:variable name="tns">
  <xsl:value-of select="/xsd:schema/@targetNamespace"/>
 </xsl:variable>

 <!--* 0 Document root *-->
 <xsl:template match="/">
  
  <xsl:variable name="doctitle">
   <xsl:text>Schema document for </xsl:text>
   <xsl:choose>
    <xsl:when test="xsd:schema/@targetNamespace">
     <xsl:text>namespace </xsl:text>
     <xsl:value-of select="$tns"/>
    </xsl:when>
    <xsl:otherwise>
     <xsl:text>unspecified namespace</xsl:text>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>

  <xsl:variable name="docHasHeading" select="
   xsd:schema/xsd:annotation[1]/xsd:documentation/*[1]
      [self::xhtml:h1 or self::h1]
   or 
   xsd:schema/xsd:annotation[1]/xsd:documentation/xhtml:div/*[1]
      [self::xhtml:h1 or self::h1]
   "/>
  <xsl:variable name="docIsProlific" select="
   count(xsd:schema/child::*) &gt; 5
   "/>

  <xsl:element name="html">
   <xsl:element name="head">
    <xsl:element name="title">
     <xsl:value-of select="$doctitle"/>
    </xsl:element>
    <xsl:element name="style">
     <xsl:attribute name="type">text/css</xsl:attribute>
     .bodytext .bodytext {
       margin-left: 0;
       margin-right: 0;
     }
     .bodytext {
       margin-left: 15%;
       margin-right: 2%;
     }
     .annotation {
       <!--* anything special to do here? *-->
       <!--* color: navy; *-->
     }
     .same-ns {
       color: Green;
     }
     .diff-ns {
       color: maroon;
     }
     .warning {
       color: red;
     }
     p.note {  
       font-style: italic;
     }
     p.dt {  
       font-weight: bold;
     }
     span.rfc {
      font-variant: small-caps;
     }
    </xsl:element>
   </xsl:element>
   <xsl:element name="body">
    
    <xsl:choose>
     <xsl:when test="$docHasHeading and not($docIsProlific)">
      <!--* If the first thing in the first documentation element is a heading,
          * and there are few children, then don't interfere. 
          *-->
      <xsl:comment>* <xsl:value-of select="$doctitle"/> *</xsl:comment>
     </xsl:when>
     <xsl:otherwise>
      <!--* either document has no heading (and needs one), or
          * we're going to do a toc and need a heading anyway
          *-->
      <xsl:element name="h1">
       <xsl:value-of select="$doctitle"/>
      </xsl:element>
      <xsl:if test="$docIsProlific">
       <xsl:element name="div">
	<xsl:element name="hr"/>
	<xsl:element name="ul">
	 <xsl:attribute name="class">bodytext</xsl:attribute>
	 <xsl:apply-templates mode="toc" select="./xsd:schema/*"/>
	</xsl:element>
	<xsl:element name="hr"/>
       </xsl:element>
      </xsl:if>
     </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates/>
   </xsl:element>
  </xsl:element>
 </xsl:template>

 <!--* 2 Schema element *-->
 <xsl:template match="xsd:schema">
  <!--* optional future change:  write out information here about
      * the attributes of xsd:schema:  version, finalDefault, blockDefault,
      * elementFormDefault, atgtributeFormDefault, namespace bindings ... 
      *-->
  <xsl:apply-templates/>
 </xsl:template>

 <!--* 3 Anotation *-->
 <xsl:template match="xsd:annotation">
  <xsl:element name="div">
   <xsl:attribute name="class">annotation</xsl:attribute>
   <xsl:attribute name="id">
    <xsl:call-template name="leid">
    </xsl:call-template>
   </xsl:attribute>
   <xsl:if test="not(./xsd:documentation//*[@class='bodytext'])">
    <xsl:element name="h3">Annotation</xsl:element>
   </xsl:if>
   <xsl:element name="div">
    <xsl:choose>
     <xsl:when test="./xsd:documentation//*[@class='bodytext']">
      <!--* if the schema document is already using class=bodytext,
          * let the schema author control the margins, don't 
          * assign the class here.
          *-->
     </xsl:when>
     <xsl:otherwise>
      <xsl:attribute name="class">bodytext</xsl:attribute>
     </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates/>
   </xsl:element>
  </xsl:element>
 </xsl:template>

 <xsl:template match="xsd:documentation">
  <xsl:choose>
   <xsl:when test=".//xhtml:* or .//div or .//p or .//li">
    <xsl:copy-of select="*"/>
   </xsl:when>
   <xsl:when test="./*">
    <xsl:message>! Unrecognized children in xs:documentation element</xsl:message>
    <xsl:copy-of select="*"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:call-template name="break-pcdata">
     <xsl:with-param name="s" select="string(.)"/>
    </xsl:call-template>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="break-pcdata">
  <xsl:param name="s"></xsl:param>

  <xsl:choose>
   <xsl:when test="starts-with($s,'&#xA;')">
    <xsl:text>&#xA;</xsl:text>
    <xsl:element name="br"/>
    <xsl:call-template name="break-pcdata">
     <xsl:with-param name="s" select="substring($s,2)"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="starts-with($s,' ')">
    <xsl:text>&#xA0;</xsl:text>
    <xsl:call-template name="break-pcdata">
     <xsl:with-param name="s" select="substring($s,2)"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:when test="contains($s,'&#xA;')">
    <xsl:value-of select="substring-before($s,'&#xA;')"/>
    <xsl:text>&#xA;</xsl:text>
    <xsl:element name="br"/>
    <xsl:call-template name="break-pcdata">
     <xsl:with-param name="s" select="substring-after($s,'&#xA;')"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="$s"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <!--* 4 Top-level components *-->
 <xsl:template match="
    xsd:schema/xsd:attribute
  | xsd:schema/xsd:element
  | xsd:schema/xsd:simpleType
  | xsd:schema/xsd:complexType
  | xsd:schema/xsd:attributeGroup
  | xsd:schema/xsd:import
  | xsd:schema/xsd:group
  | xsd:schema/xsd:notation
  ">
  <xsl:call-template name="show-top-level-component"/>
 </xsl:template>

 <xsl:template name="show-top-level-component">
  <xsl:variable name="sort">
   <xsl:call-template name="sort"/>
  </xsl:variable>
  <xsl:variable name="leid">
   <xsl:call-template name="leid"/>
  </xsl:variable>
  <xsl:variable name="has-heading-already">
   <xsl:choose>
    <xsl:when test="./xsd:annotation[1]/xsd:documentation/*//*
       [self::xhtml:*[starts-with(local-name(),'h')]  
        or 
        self::*[contains(' h1 h2 h3 h4 h5 ',local-name())]
       ]">
     <xsl:value-of select="'true'"/>
    </xsl:when>
    <xsl:otherwise>
     <xsl:value-of select="'false'"/>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>

  <xsl:element name="div">
   <xsl:attribute name="id">
    <xsl:value-of select="$leid"/>
   </xsl:attribute>
   <xsl:element name="h3">
    <xsl:element name="a">
     <xsl:attribute name="name">
      <xsl:value-of select="$leid"/>
     </xsl:attribute>
     <xsl:value-of select="concat($sort,' ')"/>
     <xsl:choose>
      <xsl:when test="count(@name) = 1">
       <xsl:element name="em">
	<xsl:value-of select="@name"/>
       </xsl:element>
      </xsl:when>
      <xsl:when test="self::xsd:import and (count(@namespace) = 1)">
       <xsl:element name="code">
	<xsl:value-of select="@namespace"/>
       </xsl:element>
      </xsl:when>
     </xsl:choose>
    </xsl:element>
   </xsl:element>

   <xsl:element name="div">
    <xsl:attribute name="class">bodytext</xsl:attribute>

    <xsl:if test="./xsd:annotation/xsd:documentation">
     <xsl:element name="div">
      <xsl:if test="$has-heading-already = 'false'">
       <xsl:element name="h4">Notes</xsl:element>
      </xsl:if><!-- /if .$has-heading-already *-->
      <xsl:apply-templates select="xsd:annotation/xsd:documentation"/>
      <xsl:if test="count(./xsd:annotation/xsd:documentation/@source) = 1">
       <xsl:element name="p">
	<xsl:text>External documentation at </xsl:text>
	<xsl:element name="code">
	 <xsl:element name="a">
	  <xsl:attribute name="href">
	   <xsl:value-of select="./xsd:annotation/xsd:documentation/@source"/>
	  </xsl:attribute>
	  <xsl:value-of select="./xsd:annotation/xsd:documentation/@source"/>
	 </xsl:element>
	</xsl:element>
       </xsl:element>
      </xsl:if>
     </xsl:element>
    </xsl:if><!-- /if ./xsd:annotation/xsd:documentation *-->

    <xsl:element name="div">
     <xsl:element name="h4">Formal declaration in XSD source form</xsl:element>
     <xsl:element name="pre">
      <xsl:variable name="preceding-node"
       select="./preceding-sibling::node()[1]"/>
      <xsl:if test="not($preceding-node/self::*)
       and (normalize-space($preceding-node) = '')">
       <xsl:value-of select="$preceding-node"/>
      </xsl:if>
      <xsl:apply-templates select="." mode="echo-xml"/>
     </xsl:element>
    </xsl:element><!--* div for XSD source form *-->
   </xsl:element><!--* div for documentation and formal description *-->

  </xsl:element><!--* div for top-level component *-->
 </xsl:template>


 <!--* 5 xml mode *-->
 <xsl:template match="*" mode="echo-xml">
  <xsl:variable name="s0">
   <xsl:call-template name="lastline-suffix">
    <xsl:with-param name="s0" select="preceding-sibling::text()
     [string-length(.) > 0][1]" />
   </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="width">
   <xsl:call-template name="stag-width">
    <xsl:with-param name="indent-length" select="string-length($s0)"/>
   </xsl:call-template>
  </xsl:variable>
  <!--* <xsl:message>Start-tag width for <xsl:value-of select="name()"/> 
        = <xsl:value-of select="$width"/></xsl:message> *-->

  <xsl:text>&lt;</xsl:text>
  <xsl:value-of select="name()"/>
  <xsl:apply-templates select="@*" mode="echo-xml">
   <xsl:with-param name="break-or-nobreak">
    <xsl:choose>
     <xsl:when test="$width > $line-wrap-length">break</xsl:when>
     <xsl:otherwise>nobreak</xsl:otherwise>
    </xsl:choose>
   </xsl:with-param>
   <xsl:with-param name="s0">
    <xsl:call-template name="lastline-suffix">
     <xsl:with-param name="s0" select="preceding-sibling::text()
      [string-length(.) > 0][1]" />
    </xsl:call-template>
   </xsl:with-param>
  </xsl:apply-templates>
  <xsl:choose>
   <xsl:when test="child::node()">
    <xsl:text>&gt;</xsl:text>
    <xsl:apply-templates select="node()" mode="echo-xml"/>
    <xsl:text>&lt;/</xsl:text>
    <xsl:value-of select="name()"/>
    <xsl:text>&gt;</xsl:text>
   </xsl:when>
   <xsl:otherwise>/&gt;</xsl:otherwise>
  </xsl:choose>
  <!--*  </xsl:element> *-->
 </xsl:template>

 <xsl:template match="xsd:annotation" mode="echo-xml"/>
 <xsl:template match="@xml:space" mode="echo-xml"/>

 <xsl:template match="@*" mode="echo-xml">
  <xsl:param name="break-or-nobreak">nobreak</xsl:param>
  <xsl:param name="s0"></xsl:param>
  <xsl:variable name="indent">
   <xsl:choose>
    <xsl:when test="normalize-space($s0) = ''">
     <xsl:value-of select="concat($additional-indent,$s0)"/>
    </xsl:when>
    <xsl:otherwise>
     <xsl:value-of select="'    '"/>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>

  <xsl:choose>
   <xsl:when test="parent::xsd:* and $break-or-nobreak = 'break'">
    <xsl:text>&#xA;</xsl:text>
    <xsl:value-of select="$indent"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:text> </xsl:text>
   </xsl:otherwise>
  </xsl:choose>
  <xsl:value-of select="name()"/>
  <xsl:text>="</xsl:text>
  <xsl:choose>
   <xsl:when test="parent::xsd:element 
    and namespace-uri() = ''
    and local-name() = 'ref'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:attribute 
    and namespace-uri() = ''
    and local-name() = 'ref'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:restriction
    and namespace-uri() = ''
    and local-name() = 'base'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:extension
    and namespace-uri() = ''
    and local-name() = 'base'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:group
    and namespace-uri() = ''
    and local-name() = 'ref'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:list
    and namespace-uri() = ''
    and local-name() = 'itemType'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:union
    and namespace-uri() = ''
    and local-name() = 'memberTypes'
    ">
    <xsl:call-template name="makelink-several-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:element
    and namespace-uri() = ''
    and local-name() = 'type'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:attribute
    and namespace-uri() = ''
    and local-name() = 'type'
    ">
    <xsl:call-template name="makelink-maybe"/>
   </xsl:when>
   <xsl:when test="parent::xsd:element
    and namespace-uri() = ''
    and local-name() = 'substitutionGroup'
    ">
    <xsl:call-template name="makelink-several-maybe"/>
   </xsl:when>

   <xsl:otherwise>
    <xsl:value-of select="."/>
   </xsl:otherwise>
  </xsl:choose>
  <xsl:text>"</xsl:text>
  
 </xsl:template>
 <xsl:template match="text()" mode="echo-xml">
  <xsl:value-of select="."/>
 </xsl:template>


 <!--* 6 toc *-->
 <xsl:template mode="toc" match="
    xsd:schema/xsd:annotation
  | xsd:schema/xsd:attribute
  | xsd:schema/xsd:element
  | xsd:schema/xsd:simpleType
  | xsd:schema/xsd:complexType
  | xsd:schema/xsd:attributeGroup
  | xsd:schema/xsd:import
  | xsd:schema/xsd:group
  | xsd:schema/xsd:notation
  ">
  <xsl:call-template name="toc-entry"/>
 </xsl:template>

 <xsl:template name="toc-entry">
  <xsl:variable name="sort">
   <xsl:call-template name="sort"/>
  </xsl:variable>
  <xsl:variable name="leid">
   <xsl:call-template name="leid"/>
  </xsl:variable>
  <xsl:element name="li">
   <xsl:element name="a">
    <xsl:attribute name="href">
     <xsl:value-of select="concat('#',$leid)"/>
    </xsl:attribute>
    <xsl:choose>
     <xsl:when test="self::xsd:annotation 
      and
      ( descendant::xhtml:h1 or descendant::xhtml:h2 or descendant::xhtml:h3
        or descendant::h1 or descendant::h2 or descendant::h3)
      ">
      <xsl:choose>
       <xsl:when test="descendant::xhtml:h1">
	<xsl:value-of select="descendant::xhtml:h1[1]"/>
       </xsl:when>
       <xsl:when test="descendant::h1">
	<xsl:value-of select="descendant::h1[1]"/>
       </xsl:when>
       <xsl:when test="descendant::xhtml:h2">
	<xsl:value-of select="descendant::xhtml:h2[1]"/>
       </xsl:when>
       <xsl:when test="descendant::h2">
	<xsl:value-of select="descendant::h2[1]"/>
       </xsl:when>
       <xsl:when test="descendant::xhtml:h3">
	<xsl:value-of select="descendant::xhtml:h3[1]"/>
       </xsl:when>
       <xsl:when test="descendant::h3">
	<xsl:value-of select="descendant::h3[1]"/>
       </xsl:when>
      </xsl:choose>
      
     </xsl:when>
     <xsl:otherwise>
      <xsl:value-of select="concat($sort,' ')"/>
      <xsl:choose>
       <xsl:when test="count(@name) = 1">
	<xsl:element name="em">
	 <xsl:value-of select="@name"/>
	</xsl:element>
       </xsl:when>
       <xsl:when test="self::xsd:annotation">
	<xsl:value-of select="1 + count(preceding-sibling::xsd:annotation)"/>
       </xsl:when>
       <xsl:when test="self::xsd:import">
	<xsl:element name="code">
	 <xsl:value-of select="@namespace"/>
	</xsl:element>
       </xsl:when>
       <xsl:otherwise>
	<!--* fake it *-->      
	<xsl:variable name="gi" select="local-name()"/>
	<xsl:value-of select="1 + count(preceding-sibling::*[local-name() = $gi])"/>
       </xsl:otherwise>
      </xsl:choose>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:element>
  </xsl:element>

 </xsl:template>

 <!--* 7 common code for calculating sort and little-endian IDs *-->
 <xsl:template name="sort">
  <xsl:choose>
   <xsl:when test="self::xsd:annotation">
    <xsl:value-of select="'Annotation'"/>
   </xsl:when>
   <xsl:when test="self::xsd:attribute">
    <xsl:value-of select="'Attribute'"/>
   </xsl:when>
   <xsl:when test="self::xsd:element">
    <xsl:value-of select="'Element'"/>
   </xsl:when>
   <xsl:when test="self::xsd:simpleType">
    <xsl:value-of select="'Simple type'"/>
   </xsl:when>
   <xsl:when test="self::xsd:complexType">
    <xsl:value-of select="'Complex type'"/>
   </xsl:when>
   <xsl:when test="self::xsd:attributeGroup">
    <xsl:value-of select="'Attribute group'"/>
   </xsl:when>
   <xsl:when test="self::xsd:group">
    <xsl:value-of select="'Model group'"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:variable name="gi" select="local-name()"/>
    <xsl:value-of select="concat(
      translate(substring($gi,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
      substring($gi,2)
     )"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>
 <xsl:template name="leid">
  <xsl:choose>
   <xsl:when test="self::xsd:annotation">
    <xsl:value-of select="concat('ann_',
     string(1+count(preceding-sibling::xsd:annotation)))"/>   
   </xsl:when>
   <xsl:when test="self::xsd:attribute">
    <xsl:value-of select="concat('att_',@name)"/>
   </xsl:when>
   <xsl:when test="self::xsd:element">
    <xsl:value-of select="concat('elem_',@name)"/>
   </xsl:when>
   <xsl:when test="self::xsd:simpleType">
    <xsl:value-of select="concat('type_',@name)"/>
   </xsl:when>
   <xsl:when test="self::xsd:complexType">
    <xsl:value-of select="concat('type_',@name)"/>
   </xsl:when>
   <xsl:when test="self::xsd:attributeGroup">
    <xsl:value-of select="concat('attgrp_',@name)"/>
   </xsl:when>
   <xsl:when test="self::xsd:group">
    <xsl:value-of select="concat('grp_',@name)"/>
   </xsl:when>
   <xsl:when test="self::xsd:import">
    <xsl:value-of select="concat('imp_',
     string(1+count(preceding-sibling::xsd:import)))"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:choose>
     <xsl:when test="@name">
      <xsl:variable name="sort" select="local-name()"/>
      <xsl:value-of select="concat($sort,'_',@name)"/>
     </xsl:when>
     <xsl:otherwise>
      <xsl:variable name="sort" select="local-name()"/>
      <xsl:variable name="pos"  select="1 + count(preceding-sibling::*[local-name() = $sort])"/>
      <xsl:value-of select="concat($sort,'_',$pos)"/>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <!--* 8 unmatched elements (mostly diagnostic for development) *-->
 <xsl:template match="*|@*">
  <xsl:variable name="fqgi">
   <xsl:call-template name="fqgi"/>
  </xsl:variable>
  <xsl:message>Warning: <xsl:value-of select="$fqgi"/> not matched.</xsl:message>
  <xsl:element name="div">
   <xsl:attribute name="class">warning</xsl:attribute>
   <xsl:value-of select="concat('&lt;',name(),'>')"/>
   <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
   </xsl:copy>
   <xsl:value-of select="concat('&lt;/',name(),'>')"/>
  </xsl:element>
 </xsl:template>

 <xsl:template name="fqgi" match="*" mode="fqgi">
  <xsl:param name="sBuf"/>
  <xsl:variable name="sCur">
   <xsl:choose>
    <xsl:when test="self::*">
     <!--* elements *-->
     <xsl:value-of select="name()"/>
    </xsl:when>
    <xsl:otherwise>
     <!--* attributes and other riffraff *-->
     <xsl:value-of select="concat('@',name())"/>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>
  <!--*
  <xsl:message>FQGI(<xsl:value-of select="concat($sBuf,',',$sCur)"/>)</xsl:message>
  *-->
  <xsl:choose>
   <xsl:when test="parent::*">
    <xsl:apply-templates mode="fqgi" select="parent::*">
     <xsl:with-param name="sBuf">
      <xsl:value-of select="concat('/',$sCur,$sBuf)"/>
     </xsl:with-param>
    </xsl:apply-templates>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="concat('/',$sCur,$sBuf)"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>


 <!--* 9 intra-document link calculation, qname manipulation *-->
 <xsl:template name="makelink-several-maybe">
  <xsl:param name="lQNames" select="normalize-space(.)"/>
  <xsl:choose>
   <xsl:when test="contains($lQNames,' ')">
    <!--* recur *-->
    <xsl:call-template name="makelink-maybe">
     <xsl:with-param name="qn" select="substring-before($lQNames,' ')"/>
    </xsl:call-template>
    <xsl:text> </xsl:text>
    <xsl:call-template name="makelink-several-maybe">
     <xsl:with-param name="lQNames" select="substring-after($lQNames,' ')"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:otherwise>
    <!--* base step, no blank so at most one QName *-->
    <xsl:call-template name="makelink-maybe">
     <xsl:with-param name="qn" select="$lQNames"/>
    </xsl:call-template>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="makelink-maybe">
  <xsl:param name="qn" select="."/>
  <xsl:param name="refns">
   <xsl:call-template name="qname-to-uri"> 
    <xsl:with-param name="qname" select="$qn"/>
   </xsl:call-template>
  </xsl:param>
  <xsl:param name="lname">
   <xsl:call-template name="qname-to-ncname">
    <xsl:with-param name="qname" select="$qn"/>
   </xsl:call-template>
  </xsl:param>

  <xsl:variable name="linktarget">
   <xsl:choose>
    <xsl:when test="$tns = $refns">
     <xsl:choose>
      <xsl:when test="parent::xsd:element
       and local-name() = 'ref'
       and count(/xsd:schema/xsd:element[@name = $lname]) = 1">
       <xsl:value-of select="concat('elem_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:element
       and local-name() = 'substitutionGroup'
       and count(/xsd:schema/xsd:element[@name = $lname]) = 1">
       <xsl:value-of select="concat('elem_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:attribute
       and local-name() = 'ref'
       and count(/xsd:schema/xsd:attribute[@name = $lname]) = 1">
       <xsl:value-of select="concat('att_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:restriction
       and count(/xsd:schema/xsd:*[@name = $lname 
                 and (self::xsd:simpleType or self::xsd:complexType)]) 
           = 1">
       <xsl:value-of select="concat('type_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:extension
       and count(/xsd:schema/xsd:*[@name = $lname 
                 and (self::xsd:simpleType or self::xsd:complexType)]) 
           = 1">
       <xsl:value-of select="concat('type_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:element
       and local-name() = 'type'
       and count(/xsd:schema/xsd:*[@name = $lname 
                 and (self::xsd:simpleType or self::xsd:complexType)]) 
           = 1">
       <xsl:value-of select="concat('type_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:attribute
       and local-name() = 'type'
       and count(/xsd:schema/xsd:*[@name = $lname 
                 and (self::xsd:simpleType or self::xsd:complexType)]) 
           = 1">
       <xsl:value-of select="concat('type_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:list
       and count(/xsd:schema/xsd:*[@name = $lname 
                 and (self::xsd:simpleType or self::xsd:complexType)]) 
           = 1">
       <xsl:value-of select="concat('type_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:union
       and count(/xsd:schema/xsd:*[@name = $lname 
                 and (self::xsd:simpleType or self::xsd:complexType)]) 
           = 1">
       <xsl:value-of select="concat('type_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:group
       and count(/xsd:schema/xsd:group[@name = $lname]) = 1">
       <xsl:value-of select="concat('grp_',$lname)"/>
      </xsl:when>
      <xsl:when test="parent::xsd:attributeGroup
       and count(/xsd:schema/xsd:atributeGroup[@name = $lname]) = 1">
       <xsl:value-of select="concat('attgrp_',$lname)"/>
      </xsl:when>
      <!--* static links to built-ins could be handled here *-->
     </xsl:choose>
    </xsl:when>
    <xsl:when test="count(ancestor::*/namespace::*) = 0">
     <!--* we are either in a no-namespace document in Opera,
         * or we are in Firefox, without ns support.
         *-->
     <xsl:value-of select="'no-ns-support'"/>
    </xsl:when>
    <xsl:otherwise>
     <!--* namespaces did not match, no target *-->
     <xsl:value-of select="'no-target'"/>
    </xsl:otherwise>
   </xsl:choose>
  </xsl:variable>

  <xsl:choose>
   <xsl:when test="($linktarget='no-ns-support')">
    <xsl:value-of select="$qn"/>
   </xsl:when>
   <xsl:when test="($linktarget='no-target' or $linktarget='') 
    and ($tns = $refns)">
    <xsl:element name="span">
     <xsl:attribute name="class">external-link same-ns</xsl:attribute>
     <xsl:value-of select="$qn"/>
    </xsl:element>
   </xsl:when>
   <xsl:when test="($linktarget='no-target') 
    and not($tns = $refns)">
    <xsl:element name="span">
     <xsl:attribute name="class">external-link diff-ns</xsl:attribute>
     <xsl:value-of select="$qn"/>
    </xsl:element>
   </xsl:when>
   <xsl:otherwise>
    <xsl:element name="a">
     <xsl:attribute name="href">
      <xsl:value-of select="concat('#',$linktarget)"/>
     </xsl:attribute>
     <xsl:value-of select="$qn"/>
    </xsl:element>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="qname-to-uri" match="*" mode="qname-to-uri">
  <xsl:param name="qname" select="."/>
  <xsl:variable name="prefix" select="substring-before($qname,':')"/>
  <xsl:choose>
   <xsl:when test="(1=1) and ($prefix='xml')">
    <!--* we need to special-case 'xml', since
        * Opera does not provide a ns node for it.
        *-->
    <xsl:value-of select="'http://www.w3.org/XML/1998/namespace'"/>
   </xsl:when>
   <xsl:when test="self::*">
    <!--* we're an element *-->
    <xsl:value-of select="string(namespace::*[name()=$prefix])"/>
   </xsl:when>
   <xsl:otherwise>
    <!--* we're not an element *-->
    <xsl:value-of select="string(parent::*/namespace::*[name()=$prefix])"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>
 <xsl:template name="qname-to-ncname">
  <xsl:param name="qname" select="."/>
  <xsl:choose>
   <xsl:when test="contains($qname,':')">
    <xsl:value-of select="substring-after($qname,':')"/>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="$qname"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="lastline-suffix">
  <xsl:param name="s0"></xsl:param>
  <xsl:choose>
   <xsl:when test="contains($s0,'&#xA;')">
    <xsl:call-template name="lastline-suffix">
     <xsl:with-param name="s0" select="substring-after($s0,'&#xA;')"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="$s0"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>


 <xsl:template name="stag-width">
  <xsl:param name="indent-length" select="0"/>
  
  <xsl:variable name="attcount" select="count(@*)"/>
  <xsl:variable name="list-attname-lengths">
   <xsl:call-template name="make-length-list">
    <xsl:with-param name="kw">attnames</xsl:with-param>
   </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="list-attval-lengths">
   <xsl:call-template name="make-length-list">
    <xsl:with-param name="kw">attvals</xsl:with-param>
   </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="sum-att-lengths">
   <xsl:call-template name="sum-list">
    <xsl:with-param name="s0" select="concat($list-attname-lengths,' ',$list-attval-lengths)"/>
   </xsl:call-template>
  </xsl:variable>

  <!--*
  <xsl:message>indent-length = <xsl:value-of select="$indent-length"/></xsl:message>
  <xsl:message>attcount = <xsl:value-of select="$attcount"/></xsl:message>
  <xsl:message>sum-att-lengths = <xsl:value-of select="$sum-att-lengths"/></xsl:message>
  <xsl:message>namelen = <xsl:value-of select="string-length(name())"/></xsl:message>
  *-->

  <xsl:value-of select="$indent-length + (4 * $attcount) + $sum-att-lengths + string-length(name()) + 3"/>

 </xsl:template>


 <xsl:template name="make-length-list">
  <xsl:param name="kw">unknown</xsl:param>
  <xsl:choose>
   <xsl:when test="$kw = 'attnames'">
    <xsl:apply-templates select="@*" mode="attnamelength"/>
   </xsl:when>
   <xsl:when test="$kw = 'attvals'">
    <xsl:apply-templates select="@*" mode="attvallength"/>
   </xsl:when>
   <xsl:otherwise>0</xsl:otherwise>
  </xsl:choose>
 </xsl:template>

 <xsl:template name="sum-list">
  <xsl:param name="n0" select="0"/>
  <xsl:param name="s0"/>

  <xsl:variable name="s1" select="normalize-space($s0)"/>

  <!--*
  <xsl:message><xsl:value-of select="concat('n0 =', $n0, ', s1 = /',$s1,'/')"/></xsl:message>
  *-->

  <xsl:choose>
   <xsl:when test="contains($s1,' ')">
    <xsl:variable name="term" select="substring-before($s1,' ')"/>
    <xsl:variable name="s2" select="substring-after($s1,' ')"/>
    <xsl:call-template name="sum-list">
     <xsl:with-param name="n0" select="$n0 + $term"/>
     <xsl:with-param name="s0" select="$s2"/>
    </xsl:call-template>
   </xsl:when>
   <xsl:otherwise>
    <xsl:value-of select="$n0 + $s1"/>
   </xsl:otherwise>
  </xsl:choose>
 </xsl:template>
 
 <xsl:template match="@*" mode="attnamelength">
  <xsl:value-of select="concat(string-length(name()), ' ')"/>
 </xsl:template>
 <xsl:template match="@*" mode="attvallength">
  <xsl:value-of select="concat(string-length(.), ' ')"/>
 </xsl:template>

</xsl:stylesheet>
<!-- Keep this comment at the end of the file
Local variables:
mode: xml
sgml-default-dtd-file:"/Library/SGML/Public/Emacs/xslt.ced"
sgml-omittag:t
sgml-shorttag:t
sgml-indent-data:t
sgml-indent-step:1
End:
-->
