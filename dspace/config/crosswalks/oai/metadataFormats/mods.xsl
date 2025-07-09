<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:doc="http://www.lyncode.com/xoai"
	version="1.0">
	<xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>
	
	<xsl:template match="/">
		<mods:mods xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-1.xsd">
			<!-- document type -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
				<xsl:choose>
					<xsl:when test=".='text::thesis::master thesis'">
						<mods:genre valueURI="{.}">master thesis</mods:genre>
					</xsl:when>
				</xsl:choose>
			</xsl:for-each>
			<!-- authors & supervisors -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
				<mods:name type="personal">
					<mods:namePart><xsl:value-of select="." /></mods:namePart>
				</mods:name>
			</xsl:for-each>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='advisor']/doc:element/doc:field[@name='value']">
				<mods:name type="supervisor">
					<mods:namePart><xsl:value-of select="." /></mods:namePart>
				</mods:name>
			</xsl:for-each>
			<!-- title -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
				<mods:titleInfo>
					<mods:title><xsl:value-of select="." /></mods:title>
				</mods:titleInfo>
			</xsl:for-each>
			<!-- date issued -->
			<xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
			<mods:originInfo>
				<mods:dateIssued encoding="iso8601">
					<xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
				</mods:dateIssued>
			</mods:originInfo>
			</xsl:if>
			<!-- abstract -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
				<mods:abstract><xsl:value-of select="." /></mods:abstract>
			</xsl:for-each>
			<!-- language -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso-639-2']/doc:element/doc:field[@name='value']">
			<mods:language>
				<mods:languageTerm type="code" authority="iso639-2b"><xsl:value-of select="." /></mods:languageTerm>
			</mods:language>
			</xsl:for-each>
			<!-- keywords -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
				<mods:subject>
					<mods:topic><xsl:value-of select="." /></mods:topic>
				</mods:subject>
			</xsl:for-each>
			<!-- identifier -->
			<mods:identifier type="hdl">hdl:<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"/></mods:identifier>
			<xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element">
				<mods:identifier>
					<xsl:attribute name="type"><xsl:value-of select="@name"/></xsl:attribute>
					<xsl:value-of select="doc:element/doc:field[@name='value']/text()" />
				</mods:identifier>
			</xsl:for-each>
			<!-- access rights -->
			<xsl:for-each select="doc:metadata/doc:element[@name='dcterms']/doc:element[@name='accessRights']/doc:element/doc:field[@name='value']">
				<xsl:if test="contains('|openaccess|restricted|administator|embargo', concat('|', ., '|'))">
					<mods:accessCondition>
						<xsl:attribute name="type">restriction on access</xsl:attribute>
						<xsl:attribute name="authorityURI">https://purl.archive.org/purl/eu-repo/semantics/</xsl:attribute>
						<xsl:choose>
							<xsl:when test=".='openaccess'">info:eu-repo/semantics/openAccess</xsl:when>
							<xsl:when test=".='restricted'">info:eu-repo/semantics/restrictedAccess</xsl:when>
							<xsl:when test=".='administrator'">info:eu-repo/semantics/closedAccess</xsl:when>
							<xsl:when test=".='embargo'">info:eu-repo/semantics/embargoedAccess</xsl:when>
						</xsl:choose>
					</mods:accessCondition>
				</xsl:if>
			</xsl:for-each>
		</mods:mods>
	</xsl:template>
</xsl:stylesheet>
