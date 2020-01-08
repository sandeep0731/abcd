<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes"/>
	<xsl:variable name="newServiceName" select="'getEcreditCustomerSearchDupeApplicationCheckCassandra'"/>
	<xsl:include href="FacadeServiceHeaderRequest.xsl"/>
	<xsl:template match="serviceRequest">
		<serviceRequest>
			<securityTokensKeyId><xsl:value-of select="/service/serviceBody/serviceRequest/ssnPieEncryptionDetails/keyId"/></securityTokensKeyId>
		    <securityTokensPhaseId><xsl:value-of select="/service/serviceBody/serviceRequest/ssnPieEncryptionDetails/phaseId"/></securityTokensPhaseId>	
			<custType><xsl:value-of select="/service/serviceBody/serviceRequest/customerType"/></custType> 
			<ssn><xsl:value-of select="/service/serviceBody/serviceRequest/ssn"/></ssn>
		</serviceRequest>
	</xsl:template>
</xsl:stylesheet>