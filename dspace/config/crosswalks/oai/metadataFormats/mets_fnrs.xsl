<?xml version="1.0" encoding="UTF-8" ?>
<!-- http://www.loc.gov/standards/mets/mets.xsd -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:doc="http://www.lyncode.com/xoai"
                xmlns:my="http://custom.functions"
                exclude-result-prefixes="my"
                version="2.0">
  <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

  <!-- GLOBAL VARIABLES ========================================================================================== -->
  <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyzàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿžšœ'" />
  <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸŽŠŒ'" />
  <xsl:variable name="emptyValue" select="'#PLACEHOLDER_PARENT_METADATA_VALUE#'"/>

  <xsl:variable name="handle" select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"/>
  <xsl:variable name="record_id" select="translate($handle, '/', '_')"/>
  <xsl:variable name="docType" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='maintype']/doc:element/doc:field[@name='value']"/>
  <xsl:variable name="openAccessBitstreams" select="/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][doc:field[@name='name']='ORIGINAL']//doc:element[@name='bitstream'][not(doc:element[@name='resourcePolicies']/doc:element[@name='resourcePolicy']) or doc:element[@name='resourcePolicies']/doc:element[@name='resourcePolicy'][doc:field[@name='group']='Anonymous' and doc:field[@name='action']='READ']]"/>
  <xsl:variable name="restrictedBitstreams" select="/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][doc:field[@name='name']='ORIGINAL']//doc:element[@name='bitstream'][doc:element[@name='resourcePolicies']/doc:element[@name='resourcePolicy'][doc:field[@name='group']='UCLouvain network' and doc:field[@name='action']='READ']]"/>

  <!-- CUSTOM FUNCTIONS ========================================================================================== -->
  <xsl:function name="my:isNotEmpty" as="xs:boolean">
    <xsl:param name="value" as="xs:string?"/>
    <xsl:sequence select="exists($value) and string-length($value)>0 and $value!=$emptyValue"/>
  </xsl:function>

  <!-- MAIN TEMPLATE ============================================================================================= -->
  <xsl:template match="/">
    <mets:mets xmlns:mets="http://www.loc.gov/METS/"
               xmlns:mods="http://www.loc.gov/mods/v3"
               xmlns:xlink="http://www.w3.org/1999/xlink"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-1.xsd">
      <xsl:attribute name="OBJID">hdl:<xsl:value-of select="$handle"/></xsl:attribute>
      <xsl:attribute name="ID">ITEM_<xsl:value-of select="$record_id"/></xsl:attribute>
      <xsl:attribute name="TYPE">
        <xsl:value-of select="translate(doc:metadata/doc:element[@name='dspace']/doc:element[@name='entity']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text(), $uppercase, $lowercase)"/>
      </xsl:attribute>

      <!-- METS HEADERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
      <mets:metsHdr>
        <xsl:attribute name="CREATEDATE">
          <xsl:value-of select="concat(format-date(current-date(), '[Y0001]-[M02]-[D02]'), 'T' , format-time(current-time(), '[H01]:[m01]:[s01]'), 'Z')"/>
        </xsl:attribute>
        <mets:agent ROLE="CUSTODIAN" TYPE="ORGANIZATION">
          <mets:name><xsl:value-of select="doc:metadata/doc:element[@name='repository']/doc:field[@name='name']/text()" /></mets:name>
        </mets:agent>
        <mets:agent ROLE="DISSEMINATOR" TYPE="ORGANIZATION">
          <mets:name><xsl:value-of select="doc:metadata/doc:element[@name='repository']/doc:field[@name='name']/text()" /></mets:name>
        </mets:agent>
        <mets:altRecordID TYPE="HANDLE"><xsl:value-of select="$handle"/></mets:altRecordID>
      </mets:metsHdr>

      <!-- DMD SECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
      <!--   Used to specify all publication metadata for "FNRS Periscops project" harvesting process          -->
      <mets:dmdSec>
        <xsl:attribute name="ID">DMD_<xsl:value-of select="$record_id"/></xsl:attribute>
        <mets:mdWrap MDTYPE="MODS">
          <mets:xmlData>
            <mods:mods>
              <!-- recordInfo ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata" mode="recordInfo"/>
              <!-- genre ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:call-template name="genre">
                <xsl:with-param name="docType" select="$docType"/>
                <xsl:with-param name="docSubtype" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='maintype']/doc:element/doc:field[@name='value']"/>
              </xsl:call-template>
              <!-- title ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <mods:titleInfo>
                <mods:title><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']"/></mods:title>
              </mods:titleInfo>
              <!-- authors ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']"/>
              <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name='value']"/>
              <!-- identifiers ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element"/>
              <mods:identifier type="hdl">hdl:<xsl:value-of select="$handle"/></mods:identifier>
              <!-- origin infos ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata" mode="originInfos"/>
              <!-- language ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']"/>
              <!-- subject ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']"/>
              <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='mesh']/doc:element/doc:field[@name='value']"/>
              <!-- specific fields ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:choose>
                <xsl:when test="$docType='text::book-part'">
                  <xsl:apply-templates select="doc:metadata" mode="hostBook"/>
                </xsl:when>
                <xsl:when test="$docType='text::conference-speech'">
                  <xsl:apply-templates select="doc:metadata" mode="hostSerial"/>
                  <xsl:apply-templates select="doc:metadata" mode="hostBook"/>
                </xsl:when>
                <xsl:when test="$docType='text::journal-article'">
                  <xsl:apply-templates select="doc:metadata" mode="hostSerial"/>
                </xsl:when>
              </xsl:choose>
              <!-- citations ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata/doc:element[@name='citations']/doc:field[@name='apa']"/>
              <!-- funding's ∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞∞ -->
              <xsl:apply-templates select="doc:metadata/doc:element[@name='funding']/doc:element[@name='organization']/doc:element/doc:field[@name='value']"/>
            </mods:mods>
          </mets:xmlData>
        </mets:mdWrap>
      </mets:dmdSec>
      <!-- AMD SECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
      <!--   Used to specify administrative metadata like
               * technical metadata about files,
               * copyright
               * access conditions
      -->
      <xsl:if test="count($openAccessBitstreams | $restrictedBitstreams) &gt; 0">
        <mets:amdSec>
          <xsl:attribute name="ID">AMD_<xsl:value-of select="$record_id"/></xsl:attribute>
          <xsl:if test="count($openAccessBitstreams) &gt; 0">
            <mets:rightsMD ID="RIGHTS_OPENACCESS">
              <mets:mdWrap MDTYPE="MODS">
                <mets:xmlData>
                  <mods:accessCondition
                          authorityURI="https://purl.archive.org/purl/eu-repo/semantics/"
                          type="restriction on access"
                  >info:eu-repo/semantics/openAccess</mods:accessCondition>
                </mets:xmlData>
              </mets:mdWrap>
            </mets:rightsMD>
          </xsl:if>
          <xsl:if test="count($restrictedBitstreams) &gt; 0">
            <mets:rightsMD ID="RIGHTS_RESTRICTED">
              <mets:mdWrap MDTYPE="MODS">
                <mets:xmlData>
                  <mods:accessCondition
                          authorityURI="https://purl.archive.org/purl/eu-repo/semantics/"
                          type="restriction on access"
                  >info:eu-repo/semantics/restrictedAccess</mods:accessCondition>
                </mets:xmlData>
              </mets:mdWrap>
            </mets:rightsMD>
          </xsl:if>
        </mets:amdSec>
      </xsl:if>
      <!-- FILE SECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
      <!--   Used to describe any attached file to this publication.
             To be eligible for "FNRS Periscops project" harvesting project:
               * only bitstream belonging to `ORIGINAL` bundle will be exposed
               * bitstream must be freely available without any restriction (openAccess)
      -->
      <xsl:if test="count($openAccessBitstreams | $restrictedBitstreams) &gt; 0">
        <mets:fileSec>
          <mets:fileGrp USE="DIFFUSION">
            <xsl:for-each select="$openAccessBitstreams | $restrictedBitstreams">
              <mets:file>
                <xsl:attribute name="ID"><xsl:value-of select="concat($record_id, '_', ancestor::doc:element[@name='bundle']/doc:field[@name='name']/text(), '_', doc:field[@name='sid']/text())"/></xsl:attribute>
                <xsl:attribute name="MIMETYPE"><xsl:value-of select="doc:field[@name='format']/text()" /></xsl:attribute>
                <xsl:attribute name="SEQ"><xsl:value-of select="position()" /></xsl:attribute>
                <xsl:attribute name="AMDID">
                  <xsl:choose>
                    <xsl:when test="exists(. intersect $openAccessBitstreams)">
                      <xsl:text>RIGHTS_OPENACCESS</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:text>RIGHT_RESTRICTEDACCESS</xsl:text>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:attribute>
                <mets:FLocat LOCTYPE="URL" xlink:type="simple">
                  <xsl:attribute name="xlink:href"><xsl:value-of select="doc:field[@name='url']/text()" /></xsl:attribute>
                  <xsl:attribute name="xlink:title"><xsl:value-of select="doc:field[@name='name']" /></xsl:attribute>
                </mets:FLocat>
              </mets:file>
            </xsl:for-each>
          </mets:fileGrp>
        </mets:fileSec>
      </xsl:if>
      <!-- STRUCT-MAP SECTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
      <mets:structMap LABEL="Publication" TYPE="LOGICAL">
        <mets:div TYPE="publication contents">
          <xsl:attribute name="DMDID">DMD_<xsl:value-of select="$record_id"/></xsl:attribute>
          <xsl:if test="count($openAccessBitstreams | $restrictedBitstreams) &gt; 0">
            <mets:div TYPE="full-text">
              <xsl:for-each select="$openAccessBitstreams">
                <mets:fptr>
                  <xsl:attribute name="FILEID"><xsl:value-of select="concat($record_id, '_', ancestor::doc:element[@name='bundle']/doc:field[@name='name']/text(), '_', doc:field[@name='sid']/text())"/></xsl:attribute>
                </mets:fptr>
              </xsl:for-each>
              <xsl:for-each select="$restrictedBitstreams">
                <mets:fptr>
                  <xsl:attribute name="FILEID"><xsl:value-of select="concat($record_id, '_', ancestor::doc:element[@name='bundle']/doc:field[@name='name']/text(), '_', doc:field[@name='sid']/text())"/></xsl:attribute>
                </mets:fptr>
              </xsl:for-each>
            </mets:div>
          </xsl:if>
        </mets:div>
      </mets:structMap>
    </mets:mets>
  </xsl:template>


  <!-- ADDITIONAL TEMPLATES ====================================================================================== -->
  <!-- GENRE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template name="genre" xmlns:mods="http://www.loc.gov/mods/v3" >
    <xsl:param name="docType"/>
    <xsl:param name="docSubtype"/>
    <xsl:variable name="computedDocType">
      <xsl:choose>
        <xsl:when test="$docType='text::journal-article'">
          <xsl:choose>
            <xsl:when test="$docSubtype='research-article'">FT-A01</xsl:when>
            <xsl:when test="$docSubtype='editorial'">FT-A02</xsl:when>
            <xsl:when test="$docSubtype='letter-to-the-editor'">FT-A03</xsl:when>
            <xsl:when test="$docSubtype='law-case-note'">FT-A04</xsl:when>
            <xsl:when test="$docSubtype='feature-article'">FT-A06</xsl:when>
            <xsl:when test="$docSubtype='clinical-study'">FT-A07</xsl:when>
            <xsl:when test="$docSubtype='popularising-article'">FT-J01</xsl:when>
            <xsl:otherwise>FT-A00</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="$docType='text::book'">FT-B01</xsl:when>
        <xsl:when test="$docType='text::book-part'">
          <xsl:choose>
            <xsl:when test="$docSubtype='book-chapter'">FT-C01</xsl:when>
            <xsl:when test="$docSubtype='preface/postface/foreword'">FT-C02</xsl:when>
            <xsl:otherwise>FT-C00</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="$docType='text::report'">FT-D01</xsl:when>
        <xsl:when test="$docType='text::thesis'">FT-E01</xsl:when>
        <xsl:when test="$docType='text::working-paper'">FT-F01</xsl:when>
        <xsl:when test="$docType='text::patent'">FT-G01</xsl:when>
        <xsl:when test="$docType='text::conference-speech'">
          <xsl:choose>
            <xsl:when test="$docSubtype='with-selection-speech'">FT-I01</xsl:when>
            <xsl:when test="$docSubtype='without-selection-speech'">FT-I02</xsl:when>
            <xsl:when test="$docSubtype='conference-poster'">FT-I03</xsl:when>
            <xsl:otherwise>FT-I00</xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>FT-L01</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="$computedDocType and string-length($computedDocType)>0">
      <mods:genre authority="fnr"><xsl:value-of select="$computedDocType"/></mods:genre>
    </xsl:if>
  </xsl:template>
  <!-- AUTHORS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']" xmlns:mods="http://www.loc.gov/mods/v3" >
    <xsl:variable name="pos" select="position()"/>
    <xsl:call-template name="publication-author">
      <xsl:with-param name="name" select="text()"/>
      <xsl:with-param name="role" select="//doc:metadata/doc:element[@name='authors']/doc:element[@name='role']/doc:element/doc:field[@name='value'][$pos]" />
      <xsl:with-param name="institution" select="//doc:metadata/doc:element[@name='authors']/doc:element[@name='institution']/doc:element[@name='code']/doc:element/doc:field[@name='value'][$pos]" />
      <xsl:with-param name="orcidID" select="//doc:metadata/doc:element[@name='authors']/doc:element[@name='identifier']/doc:element[@name='orcid']/doc:element/doc:field[@name='value'][$pos]" />
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name='value']">
    <xsl:variable name="pos" select="position()"/>
    <xsl:call-template name="publication-author">
      <xsl:with-param name="name" select="text()"/>
      <xsl:with-param name="role" select="'scientific_director_editor'" />
      <xsl:with-param name="institution" select="//doc:metadata/doc:element[@name='advisors']/doc:element[@name='institution']/doc:element[@name='code']/doc:element/doc:field[@name='value'][$pos]" />
      <xsl:with-param name="orcidID" select="//doc:metadata/doc:element[@name='advisors']/doc:element[@name='identifier']/doc:element[@name='orcid']/doc:element/doc:field[@name='value'][$pos]" />
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="publication-author" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:param name="name"/>
    <xsl:param name="role"/>
    <xsl:param name="institution"/>
    <xsl:param name="orcidID"/>
    <mods:name type="personal">
      <xsl:choose>
        <xsl:when test="contains($name, ',')">
          <mods:namePart type="family"><xsl:value-of select="normalize-space(substring-before($name, ','))"/></mods:namePart>
          <mods:namePart type="given"><xsl:value-of select="normalize-space(substring-after($name, ','))"/></mods:namePart>
        </xsl:when>
        <xsl:otherwise>
          <mods:namePart><xsl:value-of select="normalize-space($name)"/></mods:namePart>
        </xsl:otherwise>
      </xsl:choose>
      <mods:displayForm><xsl:value-of select="normalize-space($name)"/></mods:displayForm>
      <mods:role>
        <xsl:choose>
          <xsl:when test="$role='author' or $role='co_first_author' or $role='co_last_author'">
            <mods:roleTerm type="text" authroity="marcrelator">author</mods:roleTerm>
            <mods:roleTerm type="code" authority="marcrelator">aut</mods:roleTerm>
            <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/aut"/>
          </xsl:when>
          <xsl:when test="$role='scientific_director_editor'">
            <mods:roleTerm type="text" authroity="marcrelator">editor</mods:roleTerm>
            <mods:roleTerm type="code" authority="marcrelator">edt</mods:roleTerm>
            <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/edt"/>
          </xsl:when>
          <xsl:when test="$role='translator'">
            <mods:roleTerm type="text" authroity="marcrelator">translator</mods:roleTerm>
            <mods:roleTerm type="code" authority="marcrelator">trl</mods:roleTerm>
            <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/trl"/>
          </xsl:when>
          <xsl:when test="$role='inventor'">
            <mods:roleTerm type="text" authroity="marcrelator">inventor</mods:roleTerm>
            <mods:roleTerm type="code" authority="marcrelator">inv</mods:roleTerm>
            <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/inv"/>
          </xsl:when>
          <xsl:otherwise>
            <mods:roleTerm type="text" authroity="marcrelator">other</mods:roleTerm>
            <mods:roleTerm type="code" authority="marcrelator">oth</mods:roleTerm>
            <mods:roleTerm valueURI="http://id.loc.gov/vocabulary/relators/oth"/>
          </xsl:otherwise>
        </xsl:choose>
      </mods:role>
      <xsl:if test="my:isNotEmpty($orcidID)">
        <mods:nameIdentifier type="orcid" typeURI="http://id.loc.gov/vocabulary/identifiers/orcid">
          <xsl:choose>
            <xsl:when test="contains($orcidID, 'orcid.org/')"><xsl:value-of select="substring-after($orcidID, 'orcid.org/')"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="$orcidID"/></xsl:otherwise>
          </xsl:choose>
        </mods:nameIdentifier>
      </xsl:if>
      <xsl:if test="my:isNotEmpty($institution)">
        <mods:affiliation>
          <xsl:choose>
            <xsl:when test="$institution='UCLouvain'">
              <xsl:attribute name="authority">ror</xsl:attribute>
              <xsl:attribute name="authorityURI">https://ror.org</xsl:attribute>
              <xsl:attribute name="valueURI">https://ror.org/02495e989</xsl:attribute>
            </xsl:when>
            <xsl:when test="$institution='UNamur'">
              <xsl:attribute name="authority">ror</xsl:attribute>
              <xsl:attribute name="authorityURI">https://ror.org</xsl:attribute>
              <xsl:attribute name="valueURI">https://ror.org/03d1maw17</xsl:attribute>
            </xsl:when>
            <xsl:when test="$institution='USLB' or $institution='USL-B'">
              <xsl:attribute name="authority">ror</xsl:attribute>
              <xsl:attribute name="authorityURI">https://ror.org</xsl:attribute>
              <xsl:attribute name="valueURI">https://ror.org/02ygek028</xsl:attribute>
            </xsl:when>
          </xsl:choose>
          <xsl:value-of select="$institution"/>
        </mods:affiliation>
      </xsl:if>
    </mods:name>
  </xsl:template>
  <!-- IDENTIFIERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:variable name="type" select="@name"/>
    <xsl:variable name="value" select="doc:element/doc:field[@name='value']"/>
    <xsl:if test="$type='uri' and contains($value, 'handle.net/')">
      <xsl:call-template name="identifier">
        <xsl:with-param name="type" select="'hdl'"/>
        <xsl:with-param name="value" select="substring-after($value, 'handle.net/')"/>
      </xsl:call-template>
    </xsl:if>
    <xsl:call-template name="identifier">
      <xsl:with-param name="type" select="$type"/>
      <xsl:with-param name="value" select="$value"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template name="identifier" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:param name="type"/>
    <xsl:param name="value"/>
    <xsl:if test="my:isNotEmpty($value)">
      <mods:identifier>
        <xsl:if test="my:isNotEmpty($type)">
          <xsl:attribute name="type"><xsl:value-of select="$type"/></xsl:attribute>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="$type='doi'">doi:<xsl:value-of select="$value"/></xsl:when>
          <xsl:when test="$type='hdl'">hdl:<xsl:value-of select="$value"/></xsl:when>
          <xsl:otherwise><xsl:value-of select="$value"/></xsl:otherwise>
        </xsl:choose>
      </mods:identifier>
    </xsl:if>
  </xsl:template>
  <!-- LANGUAGES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']" xmlns:mods="http://www.loc.gov/mods/v3">
    <mods:languageTerm>
      <xsl:choose>
        <xsl:when test="text()='eng'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>en</xsl:text>
        </xsl:when>
        <xsl:when test="text()='fre'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>fr</xsl:text>
        </xsl:when>
        <xsl:when test="text()='dut'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>nl</xsl:text>
        </xsl:when>
        <xsl:when test="text()='spa'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>es</xsl:text>
        </xsl:when>
        <xsl:when test="text()='ita'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>it</xsl:text>
        </xsl:when>
        <xsl:when test="text()='ger'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>de</xsl:text>
        </xsl:when>
        <xsl:when test="text()='por'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>pt</xsl:text>
        </xsl:when>
        <xsl:when test="text()='gre'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>el</xsl:text>
        </xsl:when>
        <xsl:when test="text()='rus'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>ru</xsl:text>
        </xsl:when>
        <xsl:when test="text()='lat'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>la</xsl:text>
        </xsl:when>
        <xsl:when test="text()='pol'">
          <xsl:attribute name="authority">iso639</xsl:attribute>
          <xsl:attribute name="type">code</xsl:attribute>
          <xsl:text>pl</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="text()"/>
        </xsl:otherwise>
      </xsl:choose>
    </mods:languageTerm>
  </xsl:template>
  <!-- ORIGIN INFOS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata" mode="originInfos" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:variable name="rawDate" select="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="rawStatus" select="doc:element[@name='publication']/doc:element[@name='publicationStatus']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="dateIssued">
      <xsl:choose>
        <xsl:when test="$rawStatus='accepted/in-press'">in press</xsl:when>
        <xsl:when test="$rawStatus='submitted'">undated</xsl:when> <!-- should not happen because we exclude 'submitted' using FNRS filter -->
        <xsl:when test="my:isNotEmpty($rawDate) and $rawDate!='n.d.' and $rawDate!='no date'"><xsl:value-of select="substring($rawDate, 1, 4)"/></xsl:when>
        <xsl:otherwise>undated</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <mods:originInfo>
      <mods:dateIssued encoding="w3cdtf"><xsl:value-of select="$dateIssued"/></mods:dateIssued>
    </mods:originInfo>
  </xsl:template>
  <!-- REPOSITORY INFORMATION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="/doc:metadata" mode="recordInfo" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:variable name="creationdDate" select="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']" />
    <xsl:variable name="modifiedDate" select="doc:element[@name='others']/doc:field[@name='lastModifyDate']" />
    <mods:recordInfo>
      <mods:recordIdentifier><xsl:value-of select="$handle"/></mods:recordIdentifier>
      <mods:recordChangeDate encoding="w3cdtf">
        <xsl:value-of select="concat(substring($modifiedDate, 1, 10), 'T', substring($modifiedDate, 12, 8), 'Z')" />
      </mods:recordChangeDate>
      <xsl:if test="my:isNotEmpty($creationdDate)">
        <mods:recordCreationDate encoding="w3cdtf"><xsl:value-of select="$creationdDate"/></mods:recordCreationDate>
      </xsl:if>
    </mods:recordInfo>
  </xsl:template>
  <!-- APA CITATION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata/doc:element[@name='citations']/doc:field[@name='apa']" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:if test="my:isNotEmpty(.)">
      <mods:note type="citation/reference"><xsl:value-of select="normalize-space(text())"/></mods:note>
    </xsl:if>
  </xsl:template>
  <!-- SUBJECT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value'] |
                         doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element[@name='mesh']/doc:element/doc:field[@name='value']"
                xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:if test="my:isNotEmpty(.)">
      <mods:subject>
        <mods:topic><xsl:value-of select="text()"/></mods:topic>
      </mods:subject>
    </xsl:if>
  </xsl:template>
  <!-- HOST DOCUMENT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata" mode="hostSerial" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:variable name="name" select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='journal']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="issn" select="doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='issn']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="eissn" select="doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='eissn']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="editorName" select="doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="editorLocation" select="doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']"/>
    <xsl:if test="my:isNotEmpty($name)">
      <mods:relatedItem type="host">
        <mods:genre>journal</mods:genre>
        <mods:titleInfo>
          <mods:title><xsl:value-of select="$name"/></mods:title>
        </mods:titleInfo>
        <xsl:call-template name="identifier">
          <xsl:with-param name="type" select="'issn'"/>
          <xsl:with-param name="value" select="$issn"/>
        </xsl:call-template>
        <xsl:call-template name="identifier">
          <xsl:with-param name="type" select="'eissn'"/>
          <xsl:with-param name="value" select="$eissn"/>
        </xsl:call-template>
        <xsl:if test="my:isNotEmpty($editorName) or my:isNotEmpty($editorLocation)">
          <mods:originInfo>
            <xsl:if test="my:isNotEmpty($editorName)">
              <mods:publisher><xsl:value-of select="$editorName"/></mods:publisher>
            </xsl:if>
            <xsl:if test="my:isNotEmpty($editorLocation)">
              <mods:place>
                <mods:placeTerm type="text"><xsl:value-of select="$editorLocation"/></mods:placeTerm>
              </mods:place>
            </xsl:if>
          </mods:originInfo>
        </xsl:if>
      </mods:relatedItem>
    </xsl:if>
  </xsl:template>
  <xsl:template match="doc:metadata" mode="hostBook" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:variable name="name" select="doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='title']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="isbn" select="doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='isbn']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="editorName" select="doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="editorLocation" select="doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']"/>
    <xsl:if test="my:isNotEmpty($name)">
      <mods:relatedItem type="host">
        <mods:genre>book</mods:genre>
        <mods:titleInfo>
          <mods:title><xsl:value-of select="$name"/></mods:title>
        </mods:titleInfo>
        <xsl:call-template name="identifier">
          <xsl:with-param name="type" select="'isbn'"/>
          <xsl:with-param name="value" select="$isbn"/>
        </xsl:call-template>
        <xsl:if test="my:isNotEmpty($editorName) or my:isNotEmpty($editorLocation)">
          <mods:originInfo>
            <xsl:if test="my:isNotEmpty($editorName)">
              <mods:publisher><xsl:value-of select="$editorName"/></mods:publisher>
            </xsl:if>
            <xsl:if test="my:isNotEmpty($editorLocation)">
              <mods:place>
                <mods:placeTerm type="text"><xsl:value-of select="$editorLocation"/></mods:placeTerm>
              </mods:place>
            </xsl:if>
          </mods:originInfo>
        </xsl:if>
      </mods:relatedItem>
    </xsl:if>
  </xsl:template>
  <!-- FUNDING'S ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
  <xsl:template match="doc:metadata/doc:element[@name='funding']/doc:element[@name='organization']/doc:element/doc:field[@name='value']" xmlns:mods="http://www.loc.gov/mods/v3">
    <xsl:if test="my:isNotEmpty(text())">
      <mods:name type="corporate">
        <mods:role>
          <mods:roleTerm authority="marcrelator" type="text">sponsor</mods:roleTerm>
          <mods:roleTerm authority="marcrelator" type="code">spn</mods:roleTerm>
        </mods:role>
        <mods:namePart><xsl:value-of select="text()"/></mods:namePart>
      </mods:name>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
