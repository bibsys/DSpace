<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:oaire="http://namespace.openaire.eu/schema/oaire/" xmlns:datacite="http://datacite.org/schema/kernel-4"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:doc="http://www.lyncode.com/xoai"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:my="http://custom.functions"
                exclude-result-prefixes="my"
                version="2.0">
  <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

  <!-- GLOBAL VARIABLES ============================================================================================ -->
  <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyzàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿžšœ'" />
  <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸŽŠŒ'" />
  <xsl:variable name="emptyValue" select="'#PLACEHOLDER_PARENT_METADATA_VALUE#'"/>
  <xsl:variable name="openAccessBitstreams" select="/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][doc:field[@name='name']='ORIGINAL']//doc:element[@name='bitstream'][not(doc:element[@name='resourcePolicies']/doc:element[@name='resourcePolicy']) or doc:element[@name='resourcePolicies']/doc:element[@name='resourcePolicy'][doc:field[@name='group']='Anonymous' and doc:field[@name='action']='READ']]"/>
  <xsl:variable name="restrictedBitstreams" select="/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle'][doc:field[@name='name']='ORIGINAL']//doc:element[@name='bitstream'][doc:element[@name='resourcePolicies']/doc:element[@name='resourcePolicy'][doc:field[@name='group']='UCLouvain network' and doc:field[@name='action']='READ']]"/>

  <!-- CUSTOM FUNCTIONS ============================================================================================ -->
  <xsl:function name="my:isNotEmpty" as="xs:boolean">
    <xsl:param name="value" as="xs:string?"/>
    <xsl:sequence select="
        exists($value)
        and string-length(normalize-space($value)) > 0
        and $value != $emptyValue
        and not(matches(normalize-space($value), '^(n/?a|not specified|no[tn] applicable)$', 'i'))"/>
  </xsl:function>
  <xsl:function name="my:firstValue" as="xs:string">
    <xsl:param name="values" as="item()*"/>
    <xsl:sequence select="((for $value in $values return normalize-space($value))[. != ''], '')[1]"/>
  </xsl:function>

  <!-- ROOT ======================================================================================================== -->
  <xsl:template match="/">
    <oaire:resource xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://namespace.openaire.eu/schema/oaire/ https://www.openaire.eu/schema/repo-lit/4.0/openaire.xsd">
      <!-- oaire:resourceType -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']" mode="oaire"/>
      <!-- datacite:identifier & datacite:alternateIdentifier -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']" mode="datacite"/>
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']" mode="datacite"/>
      <!-- accessRights -->
      <xsl:call-template name="AccessRights">
        <xsl:with-param name="accessType" select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='accessRights']/doc:element/doc:field[@name='value']"/>
      </xsl:call-template>
      <!-- datacite:title -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']" mode="datacite"/>
      <!-- datacite:creator & contributors -->
      <xsl:variable name="creatorElements" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author' or @name='advisor']"/>
      <xsl:if test="$creatorElements">
        <datacite:creators>
          <xsl:apply-templates select="$creatorElements" mode="datacite"/>
        </datacite:creators>
      </xsl:if>
      <xsl:if test="doc:metadata/doc:element[@name='repository']">
        <datacite:contributors>
          <xsl:apply-templates select="doc:metadata/doc:element[@name='repository']" mode="contributor"/>
        </datacite:contributors>
      </xsl:if>
      <!-- oaire:fundingRefence -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='funding']/doc:element[@name='organization']" mode="oaire"/>
      <!-- datacite:dates & embargo -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']" mode="datacite"/>
      <!-- dc:language -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']" mode="dc"/>
      <!-- dc:description -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']" mode="dc"/>
      <!-- datacite:subject -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']" mode="datacite"/>
      <!-- dc:publisher -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']" mode="dc"/>
      <!-- dc:format -->
      <xsl:apply-templates select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']" mode="dc"/>
      <!-- oaire:file -->
      <xsl:call-template name="downloadFileUrls"/>
      <xsl:apply-templates select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='dataset']"/>
      <!-- specific tags depending on publication type -->
      <xsl:variable name="docType" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='maintype']/doc:element/doc:field[@name='value']"/>
      <xsl:choose>
        <xsl:when test="$docType='text::conference-speech'">
          <xsl:variable name="speechStatus" select="doc:metadata/doc:element[@name='publication']/doc:element[@name='speech']/doc:element[@name='status']/doc:element/doc:field[@name='value']"/>
          <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='name']" mode="oaire"/>
          <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='location']" mode="oaire"/>
          <xsl:apply-templates select="doc:metadata/doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='startDate']" mode="oaire"/>
          <xsl:choose>
            <xsl:when test="$speechStatus='published_in_serial'">
              <xsl:call-template name="serial_published"/>
            </xsl:when>
            <xsl:when test="$speechStatus='published_in_book'">
              <xsl:call-template name="book_published"/>
            </xsl:when>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="$docType='text::journal-article'">
          <xsl:call-template name="serial_published"/>
        </xsl:when>
        <xsl:when test="$docType='text::book-part'">
          <xsl:call-template name="book_published"/>
        </xsl:when>
      </xsl:choose>
    </oaire:resource>
  </xsl:template>


  <!-- oaire:resourceType =================================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='type']" mode="oaire">
    <xsl:variable name="maintype" select="doc:element[@name='maintype']/doc:element/doc:field[@name='value']"/>
    <oaire:resourceType>
      <xsl:choose>
        <xsl:when test="$maintype='text::book'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_2f33</xsl:attribute>
          <xsl:text>book</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::book-part'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_3248</xsl:attribute>
          <xsl:text>book part</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::conference-speech'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_5794</xsl:attribute>
          <xsl:text>conference paper</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::journal-article'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_6501</xsl:attribute>
          <xsl:text>journal article</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::report'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_93fc</xsl:attribute>
          <xsl:text>report</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::working-paper'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_8042</xsl:attribute>
          <xsl:text>working paper</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::patent'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_15cd</xsl:attribute>
          <xsl:text>patent</xsl:text>
        </xsl:when>
        <xsl:when test="$maintype='text::thesis'">
          <xsl:attribute name="resourceTypeGeneral">literature</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_db06</xsl:attribute>
          <xsl:text>doctoral thesis</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="resourceTypeGeneral">other research product</xsl:attribute>
          <xsl:attribute name="uri">http://purl.org/coar/resource_type/c_1843</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
    </oaire:resourceType>
  </xsl:template>

  <!-- datacite.titles ====================================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='title']" mode="datacite">
    <datacite:titles>
      <xsl:apply-templates select="." mode="title"/>
    </datacite:titles>
  </xsl:template>
  <xsl:template match="doc:element[@name='title']" mode="title">
    <!-- datacite.title -->
    <xsl:for-each select="./doc:element/doc:field[@name='value']">
      <datacite:title>
        <xsl:call-template name="xmlLanguage">
          <xsl:with-param name="name" select="../@name"/>
        </xsl:call-template>
        <xsl:value-of select="."/>
      </datacite:title>
    </xsl:for-each>
    <!-- datacite.title.* -->
    <xsl:for-each select="./doc:element/doc:element/doc:field[@name='value']">
      <datacite:title>
        <xsl:call-template name="xmlLanguage">
          <xsl:with-param name="name" select="../@name"/>
        </xsl:call-template>
        <xsl:attribute name="titleType">
          <xsl:call-template name="getTitleType">
            <xsl:with-param name="elementName" select="../../@name"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:value-of select="."/>
      </datacite:title>
    </xsl:for-each>
  </xsl:template>

  <!-- datacite.creators ==================================================================================== [DONE] -->
  <!-- Affiliations and ORCID iDs live in the "authors"/"advisors" metadata groups, parallel arrays
       correlated by position with the dc.contributor.author/advisor values. Indexing captured
       node-sets ($institutions[$pos]) uses the global position, matching the for-each order even
       when values are spread over several language wrappers. -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author' or @name='advisor']" mode="datacite">
    <xsl:variable name="group" select="concat(@name, 's')"/>
    <xsl:variable name="institutions" select="//doc:metadata/doc:element[@name=$group]/doc:element[@name='institution']/doc:element[@name='code']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="orcidIDs" select="//doc:metadata/doc:element[@name=$group]/doc:element[@name='identifier']/doc:element[@name='orcid']/doc:element/doc:field[@name='value']"/>
    <!-- datacite.creator -->
    <xsl:for-each select="./doc:element/doc:field[@name='value']">
      <xsl:variable name="pos" select="position()"/>
      <xsl:variable name="creatorName" select="normalize-space(.)"/>
      <xsl:variable name="institution" select="normalize-space($institutions[$pos])"/>
      <xsl:variable name="orcidID" select="normalize-space($orcidIDs[$pos])"/>
      <xsl:if test="my:isNotEmpty($creatorName)">
        <datacite:creator>
          <datacite:creatorName><xsl:value-of select="$creatorName"/></datacite:creatorName>
          <xsl:if test="my:isNotEmpty($institution)">
            <datacite:affiliation>
              <xsl:if test="$institution='UCLouvain'">
                <xsl:attribute name="affiliationIdentifier">https://ror.org/02495e989</xsl:attribute>
              </xsl:if>
              <xsl:value-of select="$institution"/>
            </datacite:affiliation>
          </xsl:if>
          <xsl:if test="my:isNotEmpty($orcidID)">
            <datacite:nameIdentifier>
              <xsl:attribute name="nameIdentifierScheme">ORCID</xsl:attribute>
              <xsl:attribute name="schemeURI">https://orcid.org</xsl:attribute>
              <xsl:value-of select="$orcidID"/>
            </datacite:nameIdentifier>
          </xsl:if>
        </datacite:creator>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- datacite:contributors ================================================================================ [DONE] -->
  <!-- This repository is considered a contributor of this work a contributor of type HostingInstitution -->
  <xsl:template match="doc:element[@name='repository']" mode="contributor">
    <datacite:contributor contributorType="HostingInstitution">
      <xsl:variable name="mail" select="./doc:field[@name='mail']"/>
      <datacite:contributorName nameType="Organizational"><xsl:value-of select="./doc:field[@name='name']"/></datacite:contributorName>
      <xsl:if test="$mail">
        <datacite:nameIdentifier>
          <xsl:attribute name="nameIdentifierScheme"><xsl:text>e-mail</xsl:text></xsl:attribute>
          <xsl:attribute name="schemeURI"><xsl:value-of select="concat('mailto:',$mail)"/></xsl:attribute>
          <xsl:value-of select="$mail"/>
        </datacite:nameIdentifier>
      </xsl:if>
    </datacite:contributor>
  </xsl:template>

  <!-- oaire:fundingReferences ============================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='funding']/doc:element[@name='organization']" mode="oaire">
    <xsl:variable name="programs" select="//doc:metadata/doc:element[@name='funding']/doc:element[@name='program']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="projects" select="//doc:metadata/doc:element[@name='funding']/doc:element[@name='project']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="grantIDs" select="//doc:metadata/doc:element[@name='funding']/doc:element[@name='number']/doc:element/doc:field[@name='value']"/>
    <oaire:fundingReferences>
      <xsl:for-each select="./doc:element/doc:field[@name='value']">
        <xsl:variable name="pos" select="position()"/>
        <xsl:variable name="program" select="normalize-space($programs[$pos])"/>
        <xsl:variable name="project" select="normalize-space($projects[$pos])"/>
        <xsl:variable name="grantID" select="normalize-space($grantIDs[$pos])"/>
        <oaire:fundingReference>
          <xsl:call-template name="funding_funder">
            <xsl:with-param name="funderName" select="normalize-space(.)"/>
          </xsl:call-template>
          <xsl:if test="my:isNotEmpty($program)">
            <oaire:fundingStream><xsl:value-of select="$program"/></oaire:fundingStream>
          </xsl:if>
          <xsl:if test="my:isNotEmpty($project)">
            <oaire:awardTitle><xsl:value-of select="$project"/></oaire:awardTitle>
          </xsl:if>
          <xsl:if test="my:isNotEmpty($grantID)">
            <oaire:awardNumber><xsl:value-of select="$grantID"/></oaire:awardNumber>
          </xsl:if>
        </oaire:fundingReference>
      </xsl:for-each>
    </oaire:fundingReferences>
  </xsl:template>
  <xsl:template name="funding_funder">
    <xsl:param name="funderName"/>
    <oaire:funderName><xsl:value-of select="$funderName"/></oaire:funderName>
    <xsl:choose>
      <xsl:when test="$funderName='F.R.S.-FNRS - Fund for Scientific Research [BE]'">
        <oaire:funderIdentifier funderIdentifierType="ISNI">https://isni.org/isni/0000000406472148</oaire:funderIdentifier>
      </xsl:when>
      <xsl:when test="$funderName='EU'">
        <oaire:funderIdentifier funderIdentifierType="ISNI">https://isni.org/isni/0000000474620421</oaire:funderIdentifier>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- datacite:identifier ================================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='others']/doc:field[@name='handle']" mode="datacite">
    <datacite:identifier identifierType="Handle">https://hdl.handle.net/<xsl:value-of select="normalize-space(.)"/></datacite:identifier>
  </xsl:template>

  <!-- datacite:alternateIdentifiers ======================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='identifier']" mode="datacite">
    <datacite:alternateIdentifiers>
      <xsl:apply-templates select="./doc:element" mode="datacite_identifiers"/>
    </datacite:alternateIdentifiers>
  </xsl:template>
  <xsl:template name="alternateIdentifierTemplate">
    <xsl:param name="value"/>
    <xsl:param name="idType"/>
    <xsl:if test="my:isNotEmpty($value) and my:isNotEmpty($idType)">
      <datacite:alternateIdentifier>
        <xsl:attribute name="alternateIdentifierType"><xsl:value-of select="$idType"/></xsl:attribute>
        <xsl:value-of select="$value"/>
      </datacite:alternateIdentifier>
    </xsl:if>
  </xsl:template>
  <!-- datacite:alternateIdentifier :: handling "dc.identifier.doi" -->
  <xsl:template match="doc:element[@name='doi']" mode="datacite_identifiers">
    <xsl:for-each select=".//doc:field[@name='value']">
      <xsl:call-template name="alternateIdentifierTemplate">
        <xsl:with-param name="value" select="normalize-space(.)"/>
        <xsl:with-param name="idType" select="'DOI'"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>
  <!-- datacite:alternateIdentifier :: handling "dc.identifier.isbn" -->
  <xsl:template match="doc:element[@name='isbn']" mode="datacite_identifiers">
    <xsl:for-each select=".//doc:field[@name='value']">
      <xsl:call-template name="alternateIdentifierTemplate">
        <xsl:with-param name="value" select="normalize-space(.)"/>
        <xsl:with-param name="idType" select="'ISBN'"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>
  <!-- datacite:alternateIdentifier :: handling "dc.identifier.pmid" -->
  <xsl:template match="doc:element[@name='pmid']" mode="datacite_identifiers">
    <xsl:for-each select=".//doc:field[@name='value']">
      <xsl:call-template name="alternateIdentifierTemplate">
        <xsl:with-param name="value" select="normalize-space(.)"/>
        <xsl:with-param name="idType" select="'PMID'"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>
  <!-- datacite:alternateIdentifier :: handling "dc.identifier.arxiv" -->
  <xsl:template match="doc:element[@name='arxiv']" mode="datacite_identifiers">
    <xsl:for-each select=".//doc:field[@name='value']">
      <xsl:call-template name="alternateIdentifierTemplate">
        <xsl:with-param name="value" select="normalize-space(.)"/>
        <xsl:with-param name="idType" select="'arXiv'"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>
  <!-- datacite:alternateIdentifier :: handling "dc.identifier.isi" -->
  <xsl:template match="doc:element[@name='isi']" mode="datacite_identifiers">
    <xsl:for-each select=".//doc:field[@name='value']">
      <xsl:call-template name="alternateIdentifierTemplate">
        <xsl:with-param name="value" select="normalize-space(.)"/>
        <xsl:with-param name="idType" select="'WOS'"/>
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>
  <!-- datacite:alternateIdentifier :: handle: dc.identifier.* -->
  <xsl:template match="doc:element" mode="datacite_identifiers"/>

  <!-- datacite.dates ======================================================================================= [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='date']" mode="datacite">
    <datacite:dates>
      <xsl:apply-templates select="./doc:element" mode="datacite_dates"/>
    </datacite:dates>
  </xsl:template>
  <!-- datacite.date :: handle dc.date.issued -->
  <xsl:template match="doc:element[@name='issued']" mode="datacite_dates">
    <xsl:variable name="dateValue" select="my:firstValue(doc:element/doc:field[@name='value'])"/>
    <xsl:if test="my:isNotEmpty($dateValue)">
      <datacite:date dateType="Accepted"><xsl:value-of select="$dateValue"/></datacite:date>
      <datacite:date dateType="Issued"><xsl:value-of select="$dateValue"/></datacite:date>
    </xsl:if>
  </xsl:template>
  <!-- datacite.date :: handle dc.date.accessioned -->
  <xsl:template match="doc:element[@name='accessioned']" mode="datacite_dates"/> <!-- do nothing -->
  <!-- datacite.date :: handle dc.date.* -->
  <xsl:template match="doc:element" mode="datacite_dates">
    <xsl:variable name="dateType">
      <xsl:call-template name="getDateType">
        <xsl:with-param name="elementName" select="./@name"/>
      </xsl:call-template>
    </xsl:variable>
    <!-- only consider elements with valid date types -->
    <xsl:variable name="dateValue" select="my:firstValue(doc:element/doc:field[@name='value'])"/>
    <xsl:if test="$dateType != '' and my:isNotEmpty($dateValue)">
      <datacite:date>
        <xsl:attribute name="dateType"><xsl:value-of select="$dateType"/></xsl:attribute>
        <xsl:value-of select="$dateValue"/>
      </datacite:date>
    </xsl:if>
  </xsl:template>

  <!-- conference template ================================================================================== [DONE] -->
  <!--   oaire:citationTitle -->
  <!--   oaire:citationConferencePlace -->
  <!--   oaire:citationConferenceDate -->
  <xsl:template match="doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='name']" mode="oaire">
    <xsl:variable name="value" select="my:firstValue(doc:element/doc:field[@name='value'])"/>
    <xsl:if test="my:isNotEmpty($value)">
      <oaire:citationTitle><xsl:value-of select="$value"/></oaire:citationTitle>
    </xsl:if>
  </xsl:template>
  <xsl:template match="doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='location']" mode="oaire">
    <xsl:variable name="value" select="my:firstValue(doc:element/doc:field[@name='value'])"/>
    <xsl:if test="my:isNotEmpty($value)">
      <oaire:citationConferencePlace><xsl:value-of select="$value"/></oaire:citationConferencePlace>
    </xsl:if>
  </xsl:template>
  <xsl:template match="doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='startDate']" mode="oaire">
    <xsl:variable name="start" select="my:firstValue(doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="end" select="my:firstValue(//doc:element[@name='publication']/doc:element[@name='conference']/doc:element[@name='endDate']/doc:element/doc:field[@name='value'])"/>
    <xsl:choose>
      <xsl:when test="my:isNotEmpty($start) and my:isNotEmpty($end)">
        <oaire:citationConferenceDate><xsl:value-of select="$start"/><xsl:text> - </xsl:text><xsl:value-of select="$end"/></oaire:citationConferenceDate>
      </xsl:when>
      <xsl:when test="my:isNotEmpty($start)">
        <oaire:citationConferenceDate><xsl:value-of select="$start"/></oaire:citationConferenceDate>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- serial template ====================================================================================== [DONE] -->
  <!--   oaire:citationTitle -->
  <!--   oaire:citationVolume -->
  <!--   oaire:citationIssue -->
  <!--   oaire:citationStartPage -->
  <!--   oaire:citationEndPage -->
  <!--   datacite:relatedIdentifier[ISSN|EISSN] -->
  <xsl:template name="serial_published">
    <xsl:variable name="journalTitle" select="my:firstValue(//doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='journal']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="journalVolume" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='volume']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="journalIssue" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='issue']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="journalPages" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='pages']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="journalIssn" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='issn']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="journalEissn" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='serial']/doc:element[@name='eissn']/doc:element/doc:field[@name='value'])"/>

    <xsl:if test="my:isNotEmpty($journalTitle)">
      <oaire:citationTitle><xsl:value-of select="$journalTitle"/></oaire:citationTitle>
      <xsl:if test="my:isNotEmpty($journalVolume)">
        <oaire:citationVolume><xsl:value-of select="$journalVolume"/></oaire:citationVolume>
      </xsl:if>
      <xsl:if test="my:isNotEmpty($journalIssue)">
        <oaire:citationIssue><xsl:value-of select="$journalIssue"/></oaire:citationIssue>
      </xsl:if>
      <xsl:if test="my:isNotEmpty($journalPages)">
        <xsl:choose>
          <xsl:when test="contains($journalPages, '-') and normalize-space(substring-after($journalPages, '-')) != ''">
            <xsl:analyze-string select="$journalPages" regex="^(.*)-(.*)$">
              <xsl:matching-substring>
                <oaire:citationStartPage><xsl:value-of select="normalize-space(regex-group(1))"/></oaire:citationStartPage>
                <oaire:citationEndPage><xsl:value-of select="normalize-space(regex-group(2))"/></oaire:citationEndPage>
              </xsl:matching-substring>
            </xsl:analyze-string>
          </xsl:when>
          <xsl:otherwise>
            <oaire:citationStartPage><xsl:value-of select="$journalPages"/></oaire:citationStartPage>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <xsl:if test="my:isNotEmpty($journalIssn) or my:isNotEmpty($journalEissn)">
        <datacite:relatedIdentifiers>
          <xsl:if test="my:isNotEmpty($journalIssn)">
            <datacite:relatedIdentifier relatedIdentifierType="ISSN" relationType="IsPartOf"><xsl:value-of select="$journalIssn"/></datacite:relatedIdentifier>
          </xsl:if>
          <xsl:if test="my:isNotEmpty($journalEissn)">
            <datacite:relatedIdentifier relatedIdentifierType="EISSN" relationType="IsPartOf"><xsl:value-of select="$journalEissn"/></datacite:relatedIdentifier>
          </xsl:if>
        </datacite:relatedIdentifiers>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <!-- book published ======================================================================================= [DONE] -->
  <!--   oaire:citationTitle -->
  <!--   oaire:citationStartPage -->
  <!--   oaire:citationEndPage -->
  <!--   datacite:relatedIdentifier[ISBN] -->
  <xsl:template name="book_published">
    <xsl:variable name="hostTitle" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='title']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="hostPages" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='pages']/doc:element/doc:field[@name='value'])"/>
    <xsl:variable name="hostIsbn" select="my:firstValue(//doc:metadata/doc:element[@name='publication']/doc:element[@name='host']/doc:element[@name='isbn']/doc:element/doc:field[@name='value'])"/>

    <xsl:if test="my:isNotEmpty($hostTitle)">
      <oaire:citationTitle><xsl:value-of select="$hostTitle"/></oaire:citationTitle>
      <xsl:if test="my:isNotEmpty($hostPages)">
        <xsl:choose>
          <xsl:when test="contains($hostPages, '-') and normalize-space(substring-after($hostPages, '-')) != ''">
            <xsl:analyze-string select="$hostPages" regex="^(.*)-(.*)$">
              <xsl:matching-substring>
                <oaire:citationStartPage><xsl:value-of select="normalize-space(regex-group(1))"/></oaire:citationStartPage>
                <oaire:citationEndPage><xsl:value-of select="normalize-space(regex-group(2))"/></oaire:citationEndPage>
              </xsl:matching-substring>
            </xsl:analyze-string>
          </xsl:when>
          <xsl:otherwise>
            <oaire:citationStartPage><xsl:value-of select="$hostPages"/></oaire:citationStartPage>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
      <xsl:if test="my:isNotEmpty($hostIsbn)">
        <datacite:relatedIdentifiers>
          <datacite:relatedIdentifier relatedIdentifierType="ISBN" relationType="IsPartOf"><xsl:value-of select="$hostIsbn"/></datacite:relatedIdentifier>
        </datacite:relatedIdentifiers>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <!-- dc:publisher ========================================================================================= [DONE] -->
  <xsl:template match="doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='name']" mode="dc">
    <xsl:variable name="locations" select="//doc:element[@name='publication']/doc:element[@name='editor']/doc:element[@name='location']/doc:element/doc:field[@name='value']"/>
    <xsl:for-each select="doc:element/doc:field[@name='value']">
      <xsl:variable name="pos" select="position()"/>
      <xsl:variable name="name" select="normalize-space(.)"/>
      <xsl:variable name="location" select="normalize-space($locations[$pos])"/>
      <xsl:if test="my:isNotEmpty($name)">
        <dc:publisher>
          <xsl:value-of select="$name"/>
          <xsl:if test="my:isNotEmpty($location)">
            <xsl:text> (</xsl:text><xsl:value-of select="$location"/><xsl:text>)</xsl:text>
          </xsl:if>
        </dc:publisher>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- dc:language ========================================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']" mode="dc">
    <xsl:for-each select="./doc:element/doc:field[@name='value']">
      <dc:language><xsl:value-of select="./text()"/></dc:language>
    </xsl:for-each>
  </xsl:template>

  <!-- dc:description ======================================================================================= [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element" mode="dc">
    <dc:description>
      <xsl:call-template name="xmlLanguage">
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
      <xsl:value-of select="./doc:field[@name='value']"/>
    </dc:description>
  </xsl:template>
  <!-- datacite:subjects ==================================================================================== [DONE] -->
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='subject']" mode="datacite">
    <datacite:subjects>
      <xsl:apply-templates select="./doc:element" mode="datacite_subject"/>
    </datacite:subjects>
  </xsl:template>
  <xsl:template match="doc:element" mode="datacite_subject">
    <xsl:for-each select="./doc:field[@name='value']">
      <xsl:variable name="subject" select="normalize-space(.)"/>
      <xsl:if test="my:isNotEmpty($subject)">
        <datacite:subject><xsl:value-of select="$subject"/></datacite:subject>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- AccessRights ========================================================================================= [DONE] -->
  <xsl:template name="AccessRights">
    <xsl:param name="accessType"/>
    <datacite:rights>
      <xsl:choose>
        <xsl:when test="$accessType='administrator'">
          <xsl:attribute name="rightsURI">http://purl.org/coar/access_right/c_14cb</xsl:attribute>
          <xsl:text>metadata only access</xsl:text>
        </xsl:when>
        <xsl:when test="$accessType='restricted'">
          <xsl:attribute name="rightsURI">http://purl.org/coar/access_right/c_16ec</xsl:attribute>
          <xsl:text>restricted access</xsl:text>
        </xsl:when>
        <xsl:when test="$accessType='embargo'">
          <xsl:attribute name="rightsURI">http://purl.org/coar/access_right/c_f1cf</xsl:attribute>
          <xsl:text>embargoed access</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="rightsURI">http://purl.org/coar/access_right/c_abf2</xsl:attribute>
          <xsl:text>open access</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </datacite:rights>
  </xsl:template>

  <!-- dc:format ============================================================================================ [DONE] -->
  <xsl:template match="doc:element[@name='bundles']/doc:element[@name='bundle']" mode="dc">
    <xsl:if test="doc:field[@name='name' and text()='ORIGINAL']">
      <xsl:for-each select="doc:element[@name='bitstreams']/doc:element[@name='bitstream']">
        <xsl:apply-templates select="." mode="dc"/>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
  <xsl:template match="doc:element[@name='bitstreams']/doc:element[@name='bitstream']" mode="dc">
    <dc:format><xsl:value-of select="doc:field[@name='format']"/></dc:format>
  </xsl:template>

  <!-- oaire:file =========================================================================================== [DONE] -->
  <xsl:template name="downloadFileUrls">
    <xsl:for-each select="$openAccessBitstreams | $restrictedBitstreams">
      <oaire:file>
        <xsl:attribute name="objectType">fulltext</xsl:attribute>
        <xsl:attribute name="mimeType"><xsl:value-of select="doc:field[@name='format']/text()"/></xsl:attribute>
        <xsl:choose>
          <xsl:when test="exists(. intersect $openAccessBitstreams)">
            <xsl:attribute name="accessRightsURI">http://purl.org/coar/access_right/c_abf2</xsl:attribute>
          </xsl:when>
          <xsl:when test="exists(. intersect $restrictedBitstreams)">
            <xsl:attribute name="accessRightsURI">http://purl.org/coar/access_right/c_16ec</xsl:attribute>
          </xsl:when>
        </xsl:choose>
        <xsl:value-of select="doc:field[@name='url']/text()" />
      </oaire:file>
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='dataset']">
    <xsl:for-each select="./doc:element/doc:field[@name='value']">
      <oaire:file>
        <xsl:attribute name="objectType">dataset</xsl:attribute>
        <xsl:value-of select="normalize-space(.)"/>
      </oaire:file>
    </xsl:for-each>
  </xsl:template>

  <!-- OTHER USEFUL TEMPLATES ====================================================================================== -->
  <xsl:template name="xmlLanguage">
    <xsl:param name="name"/>
    <xsl:variable name="lc_name">
        <xsl:call-template name="lowercase">
            <xsl:with-param name="value" select="$name"/>
        </xsl:call-template>
    </xsl:variable>
    <xsl:if test="$lc_name!='none' and $name!=''">
      <xsl:attribute name="xml:lang"><xsl:value-of select="$name"/></xsl:attribute>
    </xsl:if>
  </xsl:template>
  <xsl:template name="lowercase">
    <xsl:param name="value"/>
    <xsl:value-of select="translate($value, $uppercase, $lowercase)"/>
  </xsl:template>
  <xsl:template name="getTitleType">
    <xsl:param name="elementName"/>
    <xsl:variable name="lc_title_type">
      <xsl:call-template name="lowercase">
        <xsl:with-param name="value" select="$elementName"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$lc_title_type = 'alternativetitle' or $lc_title_type = 'alternative'">
        <xsl:text>AlternativeTitle</xsl:text>
      </xsl:when>
      <xsl:when test="$lc_title_type = 'subtitle'">
        <xsl:text>Subtitle</xsl:text>
      </xsl:when>
      <xsl:when test="$lc_title_type = 'translatedtitle'">
        <xsl:text>TranslatedTitle</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>Other</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="getDateType">
    <xsl:param name="elementName"/>
    <xsl:variable name="lc_dc_date_type">
      <xsl:call-template name="lowercase">
        <xsl:with-param name="value" select="$elementName"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$lc_dc_date_type = 'embargo'">
        <!-- Indicates the end of the embargo period. -->
        <!-- https://openaire-guidelines-for-literature-repository-managers.readthedocs.io/en/4.0.1/field_embargoenddate.html -->
        <xsl:text>Available</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- ignore all non specified text values or attributes -->
  <xsl:template match="text()|@*"/>
  <xsl:template match="text()|@*" mode="oaire"/>
  <xsl:template match="text()|@*" mode="datacite"/>
  <xsl:template match="text()|@*" mode="dc"/>
  <xsl:template match="text()|@*" mode="entity_author"/>
  <xsl:template match="text()|@*" mode="entity_funding"/>

</xsl:stylesheet>
