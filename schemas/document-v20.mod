<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!-- ===================================================================

     Apache Common Documentation elements (Version 2.0)

PURPOSE:
  This DTD was developed to create a simple yet powerful document
  type for software documentation for use with the Apache projects.

TYPICAL INVOCATION:

  <!ENTITY % document PUBLIC
      "-//APACHE//ENTITIES Documentation Vxy//EN"
      "document-vxy.mod">
  %document;

  where

    x := major version
    y := minor version

NOTES:

==================================================================== -->
<!-- =============================================================== -->
<!-- Useful entities for increased DTD readability -->
<!-- =============================================================== -->
<!ENTITY % text "#PCDATA">
<!-- Entities referred to later on are defined up front -->
<!ENTITY % markup "strong|em|code|sub|sup">
<!ENTITY % special-inline "br|img|icon|acronym|map">
<!ENTITY % links "a">
<!ENTITY % paragraphs "p|source|note|warning|fixme">
<!ENTITY % tables "table">
<!ENTITY % lists "ol|ul|dl">
<!ENTITY % special-blocks "figure|anchor">
<!-- these are used for image maps -->
<!ENTITY % Shape "(rect|circle|poly|default)">
<!ENTITY % Coords "CDATA">

<!-- =============================================================== -->
<!-- Entities for general XML compliance -->
<!-- =============================================================== -->
<!-- Common attributes
        Every element has an ID attribute (sometimes required,
        but usually optional) for links. %common.att;
        is for common attributes where the ID is optional, and
        %common-idreq.att; is for common attributes where the
        ID is required.
-->
<!ENTITY % common.att 'id                     ID              #IMPLIED
         class                  CDATA           #IMPLIED
         xml:lang               NMTOKEN         #IMPLIED'>
<!ENTITY % common-idreq.att 'id                     ID              #REQUIRED
         class                  CDATA           #IMPLIED
         xml:lang               NMTOKEN         #IMPLIED'>
<!-- xml:space attribute ===============================================
        Indicates that the element contains white space
        that the formatter or other application should retain,
        as appropriate to its function.
==================================================================== -->
<!ENTITY % xmlspace.att 'xml:space (default|preserve) #FIXED "preserve"'>
<!-- def attribute =====================================================
        Points to the element where the relevant definition can be
        found, using the IDREF mechanism.  %def.att; is for optional
        def attributes, and %def-req.att; is for required def
        attributes.
==================================================================== -->
<!ENTITY % def.att 'def                    IDREF           #IMPLIED'>
<!ENTITY % def-req.att 'def                    IDREF           #REQUIRED'>
<!-- ref attribute =====================================================
        Points to the element where more information can be found,
        using the IDREF mechanism.  %ref.att; is for optional
        ref attributes, and %ref-req.att; is for required ref
        attributes.
================================================================== -->
<!ENTITY % ref.att 'ref                    IDREF           #IMPLIED'>
<!ENTITY % ref-req.att 'ref                    IDREF           #REQUIRED'>
<!-- =============================================================== -->
<!-- Entities for general usage -->
<!-- =============================================================== -->
<!-- Key attribute =====================================================
        Optionally provides a sorting or indexing key, for cases when
        the element content is inappropriate for this purpose.
==================================================================== -->
<!ENTITY % key.att 'key                    CDATA           #IMPLIED'>
<!-- Title attributes ==================================================
        Indicates that the element requires to have a title attribute.
==================================================================== -->
<!ENTITY % title.att 'title                  CDATA           #REQUIRED'>
<!-- Name attributes ==================================================
        Indicates that the element requires to have a name attribute.
==================================================================== -->
<!ENTITY % name.att 'name                   CDATA           #REQUIRED'>
<!-- Email attributes ==================================================
        Indicates that the element requires to have an email attribute.
==================================================================== -->
<!ENTITY % email.att 'email                  CDATA           #REQUIRED'>
<!-- Link attributes ===================================================
        Indicates that the element requires to have hyperlink attributes.
==================================================================== -->
<!ENTITY % link.att 'href      CDATA             #REQUIRED
                     title     CDATA             #IMPLIED
                     rel       CDATA             #IMPLIED'>

<!ENTITY % xinclude-ns.att 'xmlns:xi CDATA #FIXED "http://www.w3.org/2001/XInclude"'>

<!-- =============================================================== -->
<!-- General definitions -->
<!-- =============================================================== -->
<!-- A person is a general unparsed human entity -->
<!ELEMENT person EMPTY>
<!ATTLIST person
  %common.att; 
  %name.att; 
  %email.att; 
>
<!-- =============================================================== -->
<!-- Content definitions -->
<!-- =============================================================== -->
<!ENTITY % local.inline "">
<!ENTITY % link-content.mix "%text;|%markup;|%special-inline;|xi:include %local.inline;">
<!ENTITY % content.mix "%link-content.mix;|%links;">
<!-- ==================================================== -->
<!-- Phrase Markup -->
<!-- ==================================================== -->
<!-- Strong (typically bold) -->
<!ELEMENT strong (%content.mix;)*>
<!ATTLIST strong
  %common.att; 
>
<!-- Emphasis (typically italic) -->
<!ELEMENT em (%content.mix;)*>
<!ATTLIST em
  %common.att; 
>
<!-- Code (typically monospaced) -->
<!ELEMENT code (%text;)>
<!ATTLIST code
  %common.att; 
>
<!-- Superscript (typically smaller and higher) -->
<!ELEMENT sup (%text;)>
<!ATTLIST sup
  %common.att; 
>
<!-- Subscript (typically smaller and lower) -->
<!ELEMENT sub (%text;)>
<!ATTLIST sub
  %common.att; 
>
<!-- ==================================================== -->
<!-- Hypertextual Links -->
<!-- ==================================================== -->
<!-- hyperlink (equivalent of <a ...>) -->
<!-- http://www.w3.org/TR/xhtml2/mod-hypertext.html#s_hypertextmodule -->
<!ELEMENT a (%link-content.mix;)*>
<!ATTLIST a
  %common.att; 
  %link.att; 
>

<!-- ==================================================== -->
<!-- Specials -->
<!-- ==================================================== -->
<!-- Breakline Object (typically forces line break) -->
<!ELEMENT br EMPTY>
<!ATTLIST br
  %common.att; 
>
<!-- Image Object (typically an inlined image) -->
<!ELEMENT img EMPTY>
<!ATTLIST img
  src CDATA #REQUIRED
  alt CDATA #REQUIRED
  title CDATA #IMPLIED
  height CDATA #IMPLIED
  width CDATA #IMPLIED
  usemap CDATA #IMPLIED
  ismap (ismap) #IMPLIED
  %common.att; 
>
<!-- support the img usemap attribute -->
<!ELEMENT map ( area+)>
<!ATTLIST map
  name CDATA #IMPLIED
  %common.att;
>
<!ELEMENT area EMPTY>
<!ATTLIST area
  shape %Shape; "rect"
  coords %Coords; #IMPLIED
  %link.att;
  nohref (nohref) #IMPLIED
  alt CDATA #REQUIRED
>
<!-- Image Icon (typically an inlined image placed as graphical item) -->
<!ELEMENT icon EMPTY>
<!ATTLIST icon
  src CDATA #REQUIRED
  alt CDATA #REQUIRED
  height CDATA #IMPLIED
  width CDATA #IMPLIED
  %common.att; 
>
<!-- Acronym (in modern browsers, will have rollover text) -->
<!ELEMENT acronym (%text;)*>
<!ATTLIST acronym
  title CDATA #REQUIRED
  %common.att; 
>

<!ELEMENT xi:include EMPTY>
<!ATTLIST xi:include
  parse     (text|xml)  "xml"
  href      CDATA       #REQUIRED
  xpointer  CDATA       #IMPLIED
  encoding  CDATA       #IMPLIED
>

<!-- =============================================================== -->
<!-- Blocks definitions -->
<!-- =============================================================== -->
<!ENTITY % local.blocks "">
<!ENTITY % blocks "%paragraphs;|%tables;|%lists;|%special-blocks; %local.blocks;">

<!-- Flow mixes block and inline -->
<!ENTITY % flow "%content.mix;|%blocks;">

<!-- ==================================================== -->
<!-- Paragraphs -->
<!-- ==================================================== -->
<!-- Text Paragraph (normally vertically space delimited. Space can be preserved.) -->
<!ELEMENT p (%content.mix;)*>
<!ATTLIST p
  %common.att; 
  xml:space (default|preserve) #IMPLIED
>
<!-- Source Paragraph (normally space is preserved) -->
<!ELEMENT source (%content.mix;)*>
<!ATTLIST source
  %common.att; 
  %xmlspace.att; 
>
<!-- Note Paragraph (normally shown encapsulated) -->
<!ELEMENT note (%content.mix;)*>
<!ATTLIST note
  label CDATA #IMPLIED
  %common.att; 
>
<!-- Warning Paragraph (normally shown with eye-catching colors) -->
<!ELEMENT warning (%content.mix;)*>
<!ATTLIST warning
  label CDATA #IMPLIED
  %common.att; 
>
<!-- Fixme Paragraph (normally not shown) -->
<!ELEMENT fixme (%content.mix;)*>
<!ATTLIST fixme
  author CDATA #REQUIRED
  %common.att; 
>
<!-- ==================================================== -->
<!-- Tables -->
<!-- ==================================================== -->
<!-- Attributes that indicate the spanning of the table cell -->
<!ENTITY % cell.span 'colspan CDATA "1"
         rowspan CDATA "1"'>
<!-- Table element -->
<!ELEMENT table (caption?, (tr | xi:include)+)>
<!ATTLIST table
  %common.att; 
>
<!-- The table title -->
<!ELEMENT caption (%content.mix;)*>
<!ATTLIST caption
  %common.att; 
>
<!-- The table row element -->
<!ELEMENT tr (th | td)+>
<!ATTLIST tr
  %common.att; 
>
<!-- The table row header element -->
<!ELEMENT th (%flow;)*>
<!ATTLIST th
  %common.att; 
  %cell.span; 
>
<!-- The table row description element -->
<!ELEMENT td (%flow;)*>
<!ATTLIST td
  %common.att; 
  %cell.span; 
>
<!-- ==================================================== -->
<!-- Lists -->
<!-- ==================================================== -->
<!-- List item -->
<!ELEMENT li (%flow;)*>
<!ATTLIST li
  %common.att; 
>
<!-- Unordered list (typically bulleted) -->
<!ELEMENT ul (li)+>
<!--    spacing attribute:
            Use "normal" to get normal vertical spacing for items;
            use "compact" to get less spacing.  The default is dependent
            on the stylesheet. -->
<!ATTLIST ul
  %common.att; 
  spacing (normal | compact) #IMPLIED
>
<!-- Ordered list (typically numbered) -->
<!ELEMENT ol (li)+>
<!--    spacing attribute:
            Use "normal" to get normal vertical spacing for items;
            use "compact" to get less spacing.  The default is dependent
            on the stylesheet. -->
<!ATTLIST ol
  %common.att; 
  spacing (normal | compact) #IMPLIED
>
<!-- Definition list (typically two-column) -->
<!ELEMENT dl (dt, dd)+>
<!ATTLIST dl
  %common.att; 
>
<!-- Definition term -->
<!ELEMENT dt (%content.mix;)*>
<!ATTLIST dt
  %common.att; 
>
<!-- Definition description -->
<!ELEMENT dd (%flow; )*>
<!ATTLIST dd
  %common.att; 
>
<!-- ==================================================== -->
<!-- Special Blocks -->
<!-- ==================================================== -->
<!-- Image Block (typically a separated and centered image) -->
<!ELEMENT figure EMPTY>
<!ATTLIST figure
  src CDATA #REQUIRED
  alt CDATA #REQUIRED
  height CDATA #IMPLIED
  width CDATA #IMPLIED
  usemap CDATA #IMPLIED
  ismap (ismap) #IMPLIED
  align CDATA #IMPLIED
  %common.att; 
>
<!-- anchor point (equivalent of <a name="...">, typically not rendered) -->
<!ELEMENT anchor EMPTY>
<!ATTLIST anchor
  %common-idreq.att; 
>

<!-- =============================================================== -->
<!-- Document -->
<!-- =============================================================== -->
<!ELEMENT document (header, body, footer?)>
<!ATTLIST document
  %common.att; 
  %xinclude-ns.att;
>
<!-- ==================================================== -->
<!-- Header -->
<!-- ==================================================== -->
<!ENTITY % local.headers "">
<!ELEMENT header (title, subtitle?, version?, type?, authors?,
                      notice*, abstract?, meta* %local.headers;)>
<!ATTLIST header
  %common.att; 
>
<!ELEMENT title (%text; | %markup; | %links; | %special-inline;)*>
<!ATTLIST title
  %common.att; 
>
<!ELEMENT subtitle (%text; | %markup;)*>
<!ATTLIST subtitle
  %common.att; 
>
<!ELEMENT version (%text;)>
<!ATTLIST version
  %common.att;
  major CDATA #IMPLIED
  minor CDATA #IMPLIED
  fix CDATA #IMPLIED
  tag CDATA #IMPLIED
>
<!ELEMENT type (%text;)>
<!ATTLIST type
  %common.att; 
>
<!ELEMENT authors (person+)>
<!ATTLIST authors
  %common.att; 
>
<!ELEMENT notice (%content.mix;)*>
<!ATTLIST notice
  %common.att; 
>
<!ELEMENT abstract (%content.mix;)*>
<!ATTLIST abstract
  %common.att; 
>
<!-- See http://www.w3.org/TR/xhtml2/mod-meta.html#s_metamodule -->
<!ELEMENT meta (#PCDATA)>
<!ATTLIST meta
  name NMTOKEN #REQUIRED
  %common.att; 
>

<!-- ==================================================== -->
<!-- Body -->
<!-- ==================================================== -->
<!ENTITY % local.sections "">
<!ENTITY % sections "section %local.sections;">
<!ELEMENT body (%sections; | %blocks; | xi:include)+>
<!ATTLIST body
  %common.att; 
>
<!ELEMENT section (title, (%sections; | %blocks; | xi:include)*)>
<!ATTLIST section
  %common.att; 
>
<!-- ==================================================== -->
<!-- Footer -->
<!-- ==================================================== -->
<!ENTITY % local.footers "">
<!ELEMENT footer (legal %local.footers;)>
<!ELEMENT legal (%content.mix;)*>
<!ATTLIST legal
  %common.att; 
>
<!-- =============================================================== -->
<!-- End of DTD -->
<!-- =============================================================== -->
