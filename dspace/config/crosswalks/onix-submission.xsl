<?xml version="1.0" encoding="UTF-8"?>
<!--
    ONIX 3.0 (long tags, one <Product> per message) -> DSpace DIM, for the PUL book catalogue import.

    Registered as `crosswalk.submission.ONIX.stylesheet` (see dspace/config/modules/pul.cfg) and executed by
    org.dspace.content.crosswalk.XSLTIngestionCrosswalk. Every <Product> is a book: `dc.type.maintype`,
    `dc.type.subtype` and `dcterms.source` are constants.

    Mapping (XPath relative to /ONIXMessage/Product)                                     -> DSpace field
    ================================================================================================================
    RelatedMaterial/RelatedWork/WorkIdentifier[IDTypeName='GCOI']/IDValue                -> dc.identifier.gcoi
    ProductIdentifier[ProductIDType='15']/IDValue                                         -> dc.identifier.isbn
    RelatedMaterial/RelatedProduct[ProductRelationCode in 06,13,27]/ProductIdentifier[15|03]
                                                                                          -> dc.identifier.isbn
        (other format of the same book: print <-> e-book; needed to match both records on one item)
    DescriptiveDetail/TitleDetail[TitleType='01']/TitleElement[TitleElementLevel='01']
        TitleText, or TitlePrefix + TitleWithoutPrefix; " : " + Subtitle when present    -> dc.title
    DescriptiveDetail/Contributor (ordered by SequenceNumber)
        PersonNameInverted | CorporateName                                                -> dc.contributor.author
        ContributorRole (ONIX list 17, see `role` template)                               -> authors.role
        (contributors without a name, e.g. UnnamedPersons, are skipped)
    DescriptiveDetail/Language[LanguageRole='01']/LanguageCode (ISO 639-2, as in the form) -> dc.language.iso
    CollateralDetail/TextContent[TextType='03' and ContentAudience 00 or absent]/Text, @language -> @lang
                                                                                          -> dc.description.abstract
        (HTML kept as-is on purpose: stripped by the import script, not by the crosswalk;
         identical texts emitted once; without @language the book language is used)
    DescriptiveDetail/Subject[SubjectSchemeIdentifier='24']/SubjectHeadingText (PUL headings, deduplicated)
                                                                                          -> dc.subject
    DescriptiveDetail/Collection[CollectionType='10']/TitleDetail/TitleElement[TitleElementLevel='02']/TitleText
                                                                                          -> publication.collection.name
    DescriptiveDetail/Collection[CollectionType='10']/CollectionSequence/CollectionSequenceNumber
                                                                                          -> publication.collection.number
    PublishingDetail/Publisher[PublishingRole='01']/PublisherName                         -> publication.editor.name
    PublishingDetail/CityOfPublication                                                    -> publication.editor.location
    PublishingDetail/PublishingDate[PublishingDateRole='01']/Date (YYYYMMDD -> YYYY-MM-DD) -> dc.date.issued
    DescriptiveDetail/Extent[ExtentType='00' (else '07')]/ExtentValue                     -> publication.numberOfPages
    DescriptiveDetail/EpubLicense: EpubLicenseExpression/EpubLicenseExpressionLink, else EpubLicenseName
                                                                                          -> dcterms.license
        (digital products only; its mere presence makes the PDF open access, see pul-import)
    constant                                                                              -> dc.type.maintype = text::book
    constant                                                                              -> dc.type.subtype = book
    constant                                                                              -> dcterms.source = PUL

    Deliberately ignored: SKU, GTIN, Barcode, Measure, Audience, EditionNumber (always 1), ProductForm,
    Language[LanguageRole='02'] (original language), TextType 02/04/16/17, Subject schemes 10/12/29/93/95/96
    (BISAC, BIC, CLIL, Thema), Collection TitleElementLevel 03, TitleElement/PartNumber, SupportingResource (cover,
    TOC), SalesRights, ProductSupply (prices), NotificationType (handled by the script).
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                version="1.0">
    <xsl:output indent="yes" method="xml" encoding="UTF-8"/>
    <xsl:strip-space elements="*"/>

    <!-- Same placeholder as org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE: keeps author/role
         positions aligned when a role is unknown; the `defaultauthorrole` consumer then falls back to `author`. -->
    <xsl:variable name="PLACEHOLDER">#PLACEHOLDER_PARENT_METADATA_VALUE#</xsl:variable>

    <xsl:template match="/ONIXMessage">
        <dim:dim>
            <xsl:apply-templates select="Product"/>
        </dim:dim>
    </xsl:template>

    <xsl:template match="Product">
        <xsl:variable name="descriptive" select="DescriptiveDetail"/>
        <xsl:variable name="bookLanguage" select="$descriptive/Language[LanguageRole='01'][1]/LanguageCode"/>

        <!-- IDENTIFIERS ============================================================================================ -->
        <xsl:for-each select="RelatedMaterial/RelatedWork/WorkIdentifier[IDTypeName='GCOI']/IDValue">
            <xsl:call-template name="field">
                <xsl:with-param name="element">identifier</xsl:with-param>
                <xsl:with-param name="qualifier">gcoi</xsl:with-param>
                <xsl:with-param name="value" select="."/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:for-each select="ProductIdentifier[ProductIDType='15']/IDValue">
            <xsl:call-template name="field">
                <xsl:with-param name="element">identifier</xsl:with-param>
                <xsl:with-param name="qualifier">isbn</xsl:with-param>
                <xsl:with-param name="value" select="."/>
            </xsl:call-template>
        </xsl:for-each>
        <!-- ISBN of the other format of the same book (06 alternative format, 13 e-publication based on print,
             27 electronic version available as). ISBN-13 (15) preferred, GTIN-13 (03) carries the same number. -->
        <xsl:for-each select="RelatedMaterial/RelatedProduct[ProductRelationCode='06' or ProductRelationCode='13'
                                                             or ProductRelationCode='27']">
            <xsl:variable name="relatedIsbn"
                          select="(ProductIdentifier[ProductIDType='15']/IDValue | ProductIdentifier[ProductIDType='03']/IDValue)[1]"/>
            <xsl:if test="string($relatedIsbn) != ''">
                <xsl:call-template name="field">
                    <xsl:with-param name="element">identifier</xsl:with-param>
                    <xsl:with-param name="qualifier">isbn</xsl:with-param>
                    <xsl:with-param name="value" select="$relatedIsbn"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each>

        <!-- TITLE ================================================================================================== -->
        <xsl:for-each select="$descriptive/TitleDetail[TitleType='01'][1]/TitleElement[TitleElementLevel='01'][1]">
            <xsl:variable name="mainTitle">
                <xsl:choose>
                    <xsl:when test="string(TitleText) != ''">
                        <xsl:value-of select="normalize-space(TitleText)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:variable name="prefix" select="normalize-space(TitlePrefix)"/>
                        <xsl:value-of select="$prefix"/>
                        <!-- "L'Art", but "La Mise": no space after an elided article -->
                        <xsl:if test="$prefix != '' and not(contains(&quot;'’&quot;, substring($prefix, string-length($prefix))))">
                            <xsl:text> </xsl:text>
                        </xsl:if>
                        <xsl:value-of select="normalize-space(TitleWithoutPrefix)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="field">
                <xsl:with-param name="element">title</xsl:with-param>
                <xsl:with-param name="value">
                    <xsl:value-of select="$mainTitle"/>
                    <xsl:if test="string(Subtitle) != ''">
                        <xsl:text> : </xsl:text>
                        <xsl:value-of select="normalize-space(Subtitle)"/>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>

        <!-- CONTRIBUTORS =========================================================================================== -->
        <!-- Two fields per contributor, always together, so that the nth author gets the nth role. -->
        <xsl:for-each select="$descriptive/Contributor[string(PersonNameInverted) != '' or string(CorporateName) != '']">
            <xsl:sort select="SequenceNumber" data-type="number"/>
            <xsl:call-template name="field">
                <xsl:with-param name="element">contributor</xsl:with-param>
                <xsl:with-param name="qualifier">author</xsl:with-param>
                <xsl:with-param name="value" select="normalize-space((PersonNameInverted | CorporateName)[1])"/>
            </xsl:call-template>
            <xsl:call-template name="field">
                <xsl:with-param name="schema">authors</xsl:with-param>
                <xsl:with-param name="element">role</xsl:with-param>
                <xsl:with-param name="value">
                    <xsl:call-template name="role">
                        <xsl:with-param name="code" select="normalize-space(ContributorRole)"/>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>

        <!-- LANGUAGE =============================================================================================== -->
        <xsl:if test="string($bookLanguage) != ''">
            <xsl:call-template name="field">
                <xsl:with-param name="element">language</xsl:with-param>
                <xsl:with-param name="qualifier">iso</xsl:with-param>
                <xsl:with-param name="value" select="normalize-space($bookLanguage)"/>
            </xsl:call-template>
        </xsl:if>

        <!-- ABSTRACTS ============================================================================================== -->
        <!-- Public descriptions only (ContentAudience 00 or absent); identical texts are emitted once. -->
        <xsl:for-each select="CollateralDetail/TextContent[TextType='03' and (not(ContentAudience) or ContentAudience='00')]
                              /Text[string(.) != '']">
            <xsl:if test="not(preceding::TextContent[TextType='03' and (not(ContentAudience) or ContentAudience='00')]
                                  /Text[normalize-space(.) = normalize-space(current())])">
            <xsl:variable name="textLanguage">
                <xsl:choose>
                    <xsl:when test="string(@language) != ''"><xsl:value-of select="@language"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="$bookLanguage"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:call-template name="field">
                <xsl:with-param name="element">description</xsl:with-param>
                <xsl:with-param name="qualifier">abstract</xsl:with-param>
                <xsl:with-param name="lang">
                    <xsl:call-template name="iso639-1">
                        <xsl:with-param name="code" select="$textLanguage"/>
                    </xsl:call-template>
                </xsl:with-param>
                <xsl:with-param name="value" select="normalize-space(.)"/>
            </xsl:call-template>
            </xsl:if>
        </xsl:for-each>

        <!-- SUBJECTS =============================================================================================== -->
        <xsl:for-each select="$descriptive/Subject[SubjectSchemeIdentifier='24' and string(SubjectHeadingText) != '']">
            <xsl:if test="not(preceding-sibling::Subject[SubjectSchemeIdentifier='24'
                                                          and normalize-space(SubjectHeadingText) = normalize-space(current()/SubjectHeadingText)])">
                <xsl:call-template name="field">
                    <xsl:with-param name="element">subject</xsl:with-param>
                    <xsl:with-param name="value" select="normalize-space(SubjectHeadingText)"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each>

        <!-- PUBLISHER COLLECTION (series) ========================================================================== -->
        <xsl:for-each select="$descriptive/Collection[CollectionType='10'][1]">
            <xsl:variable name="collectionName"
                          select="TitleDetail[TitleType='01'][1]/TitleElement[TitleElementLevel='02'][1]/TitleText"/>
            <xsl:if test="string($collectionName) != ''">
                <xsl:call-template name="field">
                    <xsl:with-param name="schema">publication</xsl:with-param>
                    <xsl:with-param name="element">collection</xsl:with-param>
                    <xsl:with-param name="qualifier">name</xsl:with-param>
                    <xsl:with-param name="value" select="normalize-space($collectionName)"/>
                </xsl:call-template>
                <xsl:if test="string(CollectionSequence[1]/CollectionSequenceNumber) != ''">
                    <xsl:call-template name="field">
                        <xsl:with-param name="schema">publication</xsl:with-param>
                        <xsl:with-param name="element">collection</xsl:with-param>
                        <xsl:with-param name="qualifier">number</xsl:with-param>
                        <xsl:with-param name="value" select="normalize-space(CollectionSequence[1]/CollectionSequenceNumber)"/>
                    </xsl:call-template>
                </xsl:if>
            </xsl:if>
        </xsl:for-each>

        <!-- PUBLISHER ============================================================================================== -->
        <xsl:for-each select="PublishingDetail/Publisher[PublishingRole='01'][1]/PublisherName[string(.) != '']">
            <xsl:call-template name="field">
                <xsl:with-param name="schema">publication</xsl:with-param>
                <xsl:with-param name="element">editor</xsl:with-param>
                <xsl:with-param name="qualifier">name</xsl:with-param>
                <xsl:with-param name="value" select="normalize-space(.)"/>
            </xsl:call-template>
        </xsl:for-each>
        <xsl:for-each select="PublishingDetail/CityOfPublication[string(.) != ''][1]">
            <xsl:call-template name="field">
                <xsl:with-param name="schema">publication</xsl:with-param>
                <xsl:with-param name="element">editor</xsl:with-param>
                <xsl:with-param name="qualifier">location</xsl:with-param>
                <xsl:with-param name="value" select="normalize-space(.)"/>
            </xsl:call-template>
        </xsl:for-each>

        <!-- PUBLICATION DATE ======================================================================================= -->
        <xsl:for-each select="PublishingDetail/PublishingDate[PublishingDateRole='01'][1]/Date[string-length(normalize-space(.)) >= 4]">
            <xsl:variable name="date" select="normalize-space(.)"/>
            <xsl:call-template name="field">
                <xsl:with-param name="element">date</xsl:with-param>
                <xsl:with-param name="qualifier">issued</xsl:with-param>
                <xsl:with-param name="value">
                    <xsl:value-of select="substring($date, 1, 4)"/>
                    <xsl:if test="string-length($date) >= 6">-<xsl:value-of select="substring($date, 5, 2)"/></xsl:if>
                    <xsl:if test="string-length($date) >= 8">-<xsl:value-of select="substring($date, 7, 2)"/></xsl:if>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:for-each>

        <!-- PAGES ================================================================================================== -->
        <xsl:variable name="pages"
                      select="($descriptive/Extent[ExtentType='00']/ExtentValue | $descriptive/Extent[ExtentType='07']/ExtentValue)[1]"/>
        <xsl:if test="string($pages) != ''">
            <xsl:call-template name="field">
                <xsl:with-param name="schema">publication</xsl:with-param>
                <xsl:with-param name="element">numberOfPages</xsl:with-param>
                <xsl:with-param name="value" select="normalize-space($pages)"/>
            </xsl:call-template>
        </xsl:if>

        <!-- LICENSE ================================================================================================ -->
        <xsl:for-each select="$descriptive/EpubLicense[1]">
            <xsl:variable name="license">
                <xsl:choose>
                    <xsl:when test="string(EpubLicenseExpression/EpubLicenseExpressionLink) != ''">
                        <xsl:value-of select="EpubLicenseExpression/EpubLicenseExpressionLink[string(.) != ''][1]"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="EpubLicenseName"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:if test="string($license) != ''">
                <xsl:call-template name="field">
                    <xsl:with-param name="schema">dcterms</xsl:with-param>
                    <xsl:with-param name="element">license</xsl:with-param>
                    <xsl:with-param name="value" select="normalize-space($license)"/>
                </xsl:call-template>
            </xsl:if>
        </xsl:for-each>

        <!-- CONSTANTS ============================================================================================== -->
        <xsl:call-template name="field">
            <xsl:with-param name="element">type</xsl:with-param>
            <xsl:with-param name="qualifier">maintype</xsl:with-param>
            <xsl:with-param name="value">text::book</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="field">
            <xsl:with-param name="element">type</xsl:with-param>
            <xsl:with-param name="qualifier">subtype</xsl:with-param>
            <xsl:with-param name="value">book</xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="field">
            <xsl:with-param name="schema">dcterms</xsl:with-param>
            <xsl:with-param name="element">source</xsl:with-param>
            <xsl:with-param name="value">PUL</xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- HELPERS ==================================================================================================== -->

    <!-- One DIM field. `schema` defaults to `dc`; empty `qualifier` and `lang` are omitted. -->
    <xsl:template name="field">
        <xsl:param name="schema">dc</xsl:param>
        <xsl:param name="element"/>
        <xsl:param name="qualifier"/>
        <xsl:param name="lang"/>
        <xsl:param name="value"/>
        <xsl:element name="dim:field">
            <xsl:attribute name="mdschema"><xsl:value-of select="$schema"/></xsl:attribute>
            <xsl:attribute name="element"><xsl:value-of select="$element"/></xsl:attribute>
            <xsl:if test="string($qualifier) != ''">
                <xsl:attribute name="qualifier"><xsl:value-of select="$qualifier"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="string($lang) != ''">
                <xsl:attribute name="lang"><xsl:value-of select="$lang"/></xsl:attribute>
            </xsl:if>
            <xsl:value-of select="$value"/>
        </xsl:element>
    </xsl:template>

    <!-- ONIX list 17 contributor role -> `publication_roles_book` value-pairs (submission-forms.xml).
         Codes seen in the PUL catalogue: A01 By (author), A13 Photographs by, A14 Text by, A15 Preface by,
         A32 Contributions by, B01 Edited by, B09 Series edited by, B15 Editorial coordination by,
         B16 Managing editor, C01 Compiled by, D03 Conductor, D99 Other. -->
    <xsl:template name="role">
        <xsl:param name="code"/>
        <xsl:choose>
            <xsl:when test="$code = 'A01' or $code = 'A14'">author</xsl:when>
            <xsl:when test="$code = 'B01' or $code = 'B09' or $code = 'B15' or $code = 'B16' or $code = 'C01'">scientific_director_editor</xsl:when>
            <xsl:when test="$code = 'A15'">preface_writer</xsl:when>
            <xsl:when test="$code = 'A32' or $code = 'A13' or $code = 'D03' or $code = 'D99'">collaborator</xsl:when>
            <xsl:otherwise><xsl:value-of select="$PLACEHOLDER"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- ISO 639-2 (ONIX) -> ISO 639-1, the form used by the `lang` attribute of metadata values. Unknown codes are
         dropped rather than guessed. Same table as mods-pr-submission.xsl. -->
    <xsl:template name="iso639-1">
        <xsl:param name="code"/>
        <xsl:variable name="c" select="normalize-space($code)"/>
        <xsl:choose>
            <xsl:when test="$c = 'fre'">fr</xsl:when>
            <xsl:when test="$c = 'eng'">en</xsl:when>
            <xsl:when test="$c = 'dut'">nl</xsl:when>
            <xsl:when test="$c = 'ger'">de</xsl:when>
            <xsl:when test="$c = 'spa'">es</xsl:when>
            <xsl:when test="$c = 'ita'">it</xsl:when>
            <xsl:when test="$c = 'gre'">el</xsl:when>
            <xsl:when test="$c = 'por'">pt</xsl:when>
            <xsl:when test="$c = 'rus'">ru</xsl:when>
            <xsl:when test="$c = 'lat'">la</xsl:when>
            <xsl:when test="$c = 'pol'">pl</xsl:when>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
