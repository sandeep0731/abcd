<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes"/>
	<xsl:variable name="newServiceName" select="'getCustomerListInquiryBySsnCassandra'"/>
	<xsl:include href="FacadeServiceHeaderRequest.xsl"/>
	<xsl:template match="serviceRequest">
			<serviceRequest>
					<xsl:if test="string-length(/service/serviceBody/serviceRequest/accountNo)>0">
						<customerId><xsl:value-of select="substring-before(/service/serviceBody/serviceRequest/accountNo,'-')" /></customerId>
						<accountNumber><xsl:value-of  select="substring-after(/service/serviceBody/serviceRequest/accountNo,'-')" /></accountNumber>
					</xsl:if>
						<securityTokensKeyId><xsl:value-of select="/service/serviceBody/serviceRequest/ssnPieEncryptionDetails/keyId"/></securityTokensKeyId>
						<securityTokensPhaseId><xsl:value-of select="/service/serviceBody/serviceRequest/ssnPieEncryptionDetails/phaseId"/></securityTokensPhaseId>	 
						<lastNme><xsl:value-of select="/service/serviceBody/serviceRequest/lastName"/></lastNme>
						<firstNme><xsl:value-of select="/service/serviceBody/serviceRequest/firstName"/></firstNme>
						<cityNme><xsl:value-of select="/service/serviceBody/serviceRequest/cityName"/></cityNme>
						<stateCd><xsl:value-of select="/service/serviceBody/serviceRequest/stateCode"/></stateCd>
						<addrZipCode><xsl:value-of select="/service/serviceBody/serviceRequest/zipCode"/></addrZipCode>
						<ssn><xsl:value-of select="/service/serviceBody/serviceRequest/ssn"/></ssn>
						<federalTaxId><xsl:value-of select="/service/serviceBody/serviceRequest/federalTaxId"/></federalTaxId>
						<lineMdn><xsl:value-of select="/service/serviceBody/serviceRequest/mtn"/></lineMdn>
						<activeCustomerFlag><xsl:value-of select="/service/serviceBody/serviceRequest/activeCustomerFlag"/></activeCustomerFlag>
						<accountActiveInactiveFlag><xsl:value-of select="/service/serviceBody/serviceRequest/accountActiveInactiveFlag"/></accountActiveInactiveFlag>
						<billTypeActiveInactiveFlag><xsl:value-of select="/service/serviceBody/serviceRequest/billTypeActiveInactiveFlag"/></billTypeActiveInactiveFlag>
						<invoiceNumber><xsl:value-of select="/service/serviceBody/serviceRequest/invoiceNumber"/></invoiceNumber>
						<creditApprovalNo><xsl:value-of select="/service/serviceBody/serviceRequest/creditApprovalNo"/></creditApprovalNo>
						<oldAccountNumber><xsl:value-of select="/service/serviceBody/serviceRequest/oldAccountNumber"/></oldAccountNumber>
						<ecpdId><xsl:value-of select="/service/serviceBody/serviceRequest/ecpdId"/></ecpdId>
						<keyCustomerInfo>
							<scrollType><xsl:value-of select="/service/serviceBody/serviceRequest/scrollType"/></scrollType>
							<customerId><xsl:value-of select="/service/serviceBody/serviceRequest/customerId"/></customerId>
							<firstName><xsl:value-of select="/service/serviceBody/serviceRequest/firstName"/></firstName>
							<lastName><xsl:value-of select="/service/serviceBody/serviceRequest/lastName"/></lastName>
							<city><xsl:value-of select="/service/serviceBody/serviceRequest/city"/></city>
							<state><xsl:value-of select="/service/serviceBody/serviceRequest/state"/></state>
							<zipCode><xsl:value-of select="/service/serviceBody/serviceRequest/zipCode"/></zipCode>
						</keyCustomerInfo> 
			</serviceRequest>
	</xsl:template>
</xsl:stylesheet>