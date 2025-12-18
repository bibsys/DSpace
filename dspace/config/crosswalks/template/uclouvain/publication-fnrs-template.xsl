<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xsll="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="fo">

    <!-- PARAMS & VARIABLES ======================================================================================== -->
    <xsl:param name="assetsDir" />
    <xsl:param name="imageDir" />
    <xsl:param name="currentDate" />
    <xsl:param name="highlightText"/>
    <xsl:param name="documentTitle" select="'Liste des publications'"/>

    <xsl:param name="fontFamily" />
    <xsl:param name="fontSize" select="'10pt'"/>
    <xsl:param name="smFontSize" select="'9pt'"/>
    <xsl:param name="lgFontSize" select="'12pt'"/>
    <xsl:param name="xlFontSize" select="'20pt'"/>

    <xsl:param name="primaryColor" select="'#494f56'"/>  <!-- default DIAL gray color -->
    <xsl:param name="secondaryColor" select="'#002f5e'"/>  <!-- default UCLouvain blue color -->


    <xsl:attribute-set name="cell.topLeft">
        <xsl:attribute name="display-align">before</xsl:attribute>
        <xsl:attribute name="text-align">left</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="badge.attrs">
        <xsl:attribute name="content-height"><xsl:value-of select="$fontSize"/></xsl:attribute>
        <xsl:attribute name="scaling">uniform</xsl:attribute>
        <xsl:attribute name="alignment-baseline">middle</xsl:attribute>
    </xsl:attribute-set>

    <xsl:key name="categorySection" match="Publication" use="normalize-space(Category)"/>
    
    <!-- FUNCTIONS LIKE ============================================================================================ -->
    <!-- Allows to translate a publication category name into a human-readable label -->
    <xsl:template name="translate-category-name">
        <xsl:param name="name"/>
        <xsl:choose>
            <xsl:when test="$name='fnrs.category.1'">Ouvrages publiés comme auteur, co-auteur ou éditeur</xsl:when>
            <xsl:when test="$name='fnrs.category.2'">Parties d'ouvrages publiés comme auteur ou co-auteur</xsl:when>
            <xsl:when test="$name='fnrs.category.3'">Articles publiés dans des journaux à comité de lecture</xsl:when>
            <xsl:when test="$name='fnrs.category.4'">Articles publiés dans des actes de conférences</xsl:when>
            <xsl:when test="$name='fnrs.category.5'">Présentations orales dans des conférences avec comité scientifique de sélection</xsl:when>
            <xsl:when test="$name='fnrs.category.6'">Brevet</xsl:when>
            <xsl:otherwise>Aucune catégorie</xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <!-- Allow to highlight all occurrences of a specific token into a text. -->
    <xsl:template name="boldify-occurrences">
        <xsl:param name="text"/>
        <xsl:param name="token"/>
        <xsl:choose>
            <xsl:when test="contains($text, $token)">
                <xsl:variable name="before" select="substring-before($text, $token)"/>
                <xsl:variable name="after"  select="substring-after($text, $token)"/>
                <xsl:value-of select="$before"/>
                <fo:inline font-weight="bold">
                    <xsl:value-of select="$token"/>
                </fo:inline>
                <xsl:call-template name="boldify-occurrences">
                    <xsl:with-param name="text" select="$after"/>
                    <xsl:with-param name="token" select="$token"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- MASTER PAGE LAYOUT ======================================================================================== -->
    <xsl:template match="Publications">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <xsl:attribute name="font-family"><xsl:value-of select="$fontFamily"/></xsl:attribute>
            <fo:layout-master-set>
				<fo:simple-page-master master-name="simpleA4"
					                   page-height="29.7cm" page-width="21cm"
                                       margin="0cm">
                    <fo:region-body margin-top="4cm" margin-bottom="2cm" margin-left="1cm" margin-right="1cm"/>
                    <fo:region-before extent="4cm"/>
                    <fo:region-after extent="2cm"/>
				</fo:simple-page-master>
			</fo:layout-master-set>
            <!-- PAGE LAYOUT ======================================================================================= -->
			<fo:page-sequence master-reference="simpleA4">
                <!-- HEADER REGION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
                <fo:static-content flow-name="xsl-region-before">
                    <fo:block>
                        <fo:external-graphic
                            height="3.67cm"
                            content-width="21cm"
                            scaling="non-uniform"
                            src="file:{$assetsDir}/PDF-header.png"/>
                    </fo:block>
                </fo:static-content>
                <!-- FOOTER REGION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block border-top="1px solid {$primaryColor}" margin-bottom="0.25cm"/>
                    <fo:block margin-left="0.5cm" margin-right="0.5cm" margin-top="0cm">
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="80%"/>
                            <fo:table-column column-width="20%"/>
                            <fo:table-body>
                                <fo:table-row font-size="{$smFontSize}" display-align="before">
                                    <fo:table-cell>
                                        <fo:block font-size="{$smFontSize}">
                                            <xsl:value-of select="$currentDate"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell text-align="right">
                                        <fo:block font-size="{$smFontSize}">
                                            <fo:page-number/>
                                            <fo:inline color="{$secondaryColor}" padding-left="0.1cm" padding-right="0.1cm">/</fo:inline>
                                            <fo:page-number-citation ref-id="last-page"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:static-content>
                <!-- BODY REGION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
				<fo:flow flow-name="xsl-region-body" font-size="{$fontSize}">
		         	<fo:block text-align="center">
                        <fo:inline font-size="{$xlFontSize}" font-weight="bold">
                            <xsl:value-of select="$documentTitle"/>
                        </fo:inline>
                    </fo:block>
					<xsl:for-each select="Publication[generate-id()=generate-id(key('categorySection', normalize-space(Category))[1])]">
                        <xsl:sort select="normalize-space(Category)" order="ascending"/>
                        <!-- Category header -->
                        <fo:block font-size="{$lgFontSize}"
                                  space-before="1.25cm"
                                  space-after="0.75cm"
                                  border-bottom="1px solid {$secondaryColor}"
                                  color="{$secondaryColor}"
                                  keep-with-next.within-page="always">
                            <xsl:call-template name="translate-category-name">
                                <xsl:with-param name="name" select="normalize-space(Category)"/>
                            </xsl:call-template>
                        </fo:block>
                        <!-- Category publication -->
                        <xsl:for-each select="key('categorySection', normalize-space(Category))">
                            <xsl:sort select="string(number(normalize-space(Issued)))" data-type="number" order="descending"/>
                            <xsl:apply-templates select="." mode="publication">
                                <xsl:with-param name="position" select="position()"/>
                            </xsl:apply-templates>
                        </xsl:for-each>
                    </xsl:for-each>
                    <fo:block id="last-page"/>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <!-- TEMPLATES ================================================================================================= -->
    <!-- Publication ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
    <!--   For each publication, we want to display
             * rank/position of the publication in the category (left column)
             * icon corresponding to the publication access type (left column)
             * the publication citation (right column)
             * the publication handle link (right column)
    -->
    <xsl:template match="Publication" mode="publication">
        <xsl:param name="position"/>
        <fo:block keep-together.within-page="always" margin-bottom="5mm">
            <fo:table table-layout="fixed" width="100%">
                <fo:table-column column-width="1cm"/>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell xsl:use-attribute-sets="cell.topLeft" padding-right="4pt">
                            <fo:block color="{$secondaryColor}" font-weight="bold">
                                <xsl:value-of select="$position"/>
                                <xsl:text>.</xsl:text>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="cell.topLeft">
                            <fo:block>
                                <xsl:apply-templates select="Citation"/>
                            </fo:block>
                            <fo:block>
                                <fo:basic-link external-destination="url('{normalize-space(Handle)}')" text-decoration="underline" color="{$secondaryColor}">
                                    <xsl:value-of select="Handle"/>
                                </fo:basic-link>
                                <fo:leader leader-pattern="space" leader-length="0.3cm"/>
                                <xsl:apply-templates select="Access"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:block>
	</xsl:template>
    <!-- Citation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
    <!--   As CSL engine doesn't provide any FO driver, the publication citation is rendered using HTML driver.
           We need to convert some HTML tag to FO tags/directive to get the correct render :
              * span[@style="font-style: italic"]
              * span[@style="font-weight: bold"]
           We also need highlight the specific publication author name specifies into global XSL parameter
    -->
    <xsl:template match="Citation">
        <xsl:apply-templates select="node()"/>
    </xsl:template>
    <xsl:template match="span[contains(@style, 'font-style: italic')]" priority="10">
        <fo:inline font-style="italic">
            <xsl:apply-templates select="node()"/>
        </fo:inline>
    </xsl:template>
    <xsl:template match="span[contains(@style, 'font-weight: bold')]" priority="10">
        <fo:inline font-weight="bold">
            <xsl:apply-templates select="node()"/>
        </fo:inline>
    </xsl:template>
    <xsl:template match="Citation//*" priority="-1">
        <xsl:apply-templates select="node()"/>
    </xsl:template>
    <xsl:template match="Citation//text()" priority="-1">
        <xsl:choose>
            <xsl:when test="string-length($highlightText) = 0 or not(contains(., $highlightText))">
                <xsl:value-of select="."/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="boldify-occurrences">
                    <xsl:with-param name="text" select="."/>
                    <xsl:with-param name="token" select="$highlightText"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- Access ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
    <xsl:template match="Access">
        <xsl:choose>
            <xsl:when test=".='openaccess'">
                <fo:external-graphic src="file:{$assetsDir}/openaccess-badge.png" xsl:use-attribute-sets="badge.attrs"/>
            </xsl:when>
            <xsl:when test=".='restricted'">
                <fo:external-graphic src="file:{$assetsDir}/restricted-badge.png" xsl:use-attribute-sets="badge.attrs"/>
            </xsl:when>
            <xsl:when test=".='adminitrator'">
                <fo:external-graphic src="file:{$assetsDir}/closed-badge.png" xsl:use-attribute-sets="badge.attrs"/>
            </xsl:when>
            <xsl:when test=".='embargo'">
                <fo:external-graphic src="file:{$assetsDir}/embargo-badge.png" xsl:use-attribute-sets="badge.attrs"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
