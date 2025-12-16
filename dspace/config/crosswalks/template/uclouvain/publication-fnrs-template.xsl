<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:pt="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"
	xmlns:cerif="https://www.openaire.eu/cerif-profile/1.1/"
	exclude-result-prefixes="fo">
	
	<xsl:param name="imageDir" />
    <xsl:param name="fontFamily" />

    <xsl:template match="Publications">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
            <xsl:attribute name="font-family">
                <xsl:value-of select="$fontFamily" />
            </xsl:attribute>

            <!-- MASTER LAYOUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
			<fo:layout-master-set>
				<fo:simple-page-master
                    master-name="simpleA4"
					page-height="29.7cm" page-width="24cm"
                    margin-top="2cm" margin-bottom="2cm"
                    margin-left="1cm" margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>

            <!-- PAGE LAYOUT ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->
			<fo:page-sequence master-reference="simpleA4">
				<fo:flow flow-name="xsl-region-body">
		         	<fo:block margin-bottom="5mm" padding="2mm">
						<fo:block font-size="26pt" font-weight="bold" text-align="center" >
							<xsl:value-of select="'Export FNRS'" />
						</fo:block>
					</fo:block>
                    <xsl:apply-templates select="cerif:Publication"/>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <xsl:template match="cerif:Publication">
        <!-- TO BE CONTINUED ... -->
        <fo:block margin-bottom="5mm">
            <fo:block font-size="10pt" text-align="justify" >
                <xsl:value-of select="cerif:Citation"/>
            </fo:block>

            <fo:basic-link
                external-destination="url('{normalize-space(cerif:Handle)}')"
                text-decoration="underline"
                color="blue">
                <xsl:value-of select="cerif:Handle"/>
            </fo:basic-link>
        </fo:block>
	</xsl:template>
	
</xsl:stylesheet>
