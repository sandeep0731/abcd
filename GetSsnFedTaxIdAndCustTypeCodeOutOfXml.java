package com.vzw.cops.action.adapt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.Element;

import com.vzw.common.crypto.pie.PIESecureDataUtils;
import com.vzw.common.crypto.pie.PIESecureDataVO;
import com.vzw.common.crypto.pie.SsnSpiData;
import com.vzw.common.crypto.pie.impl.PIESecureDataImplUtils;
import com.vzw.common.crypto.vo.CommonGenericVO;
import com.vzw.common.crypto.vo.PieSecurityDetailsVO;
import com.vzw.cops.api.common.ServiceHeader;
import com.vzw.cops.api.common.ServiceRequest;
import com.vzw.cops.constants.ApplicationConstants;
import com.vzw.cops.engine.EngineFactory;
import com.vzw.cops.engine.exception.EngineException;
import com.vzw.cops.entity.Customer;
import com.vzw.cops.entity.CustomerType;
import com.vzw.cops.entity.SecurityTokens;
import com.vzw.cops.entity.list.CustomerList;
import com.vzw.cops.exception.ActionValidationException;
import com.vzw.cops.exception.ApplicationException;
import com.vzw.cops.exception.DataNotFoundException;
import com.vzw.cops.exception.ServiceValidationException;
import com.vzw.cops.util.DataModelUtil;
import com.vzw.cops.util.RoutingUtil;
import com.vzw.cops.util.StringUtils;
import com.vzw.cops.util.DataModelUtil.Hint;

public class GetSsnFedTaxIdAndCustTypeCodeOutOfXml extends GetEntitiesOutOfXml {
	private SecurityTokens securityTokens ;
	private String feTokenInd = null;
	private String custType= ApplicationConstants.EMPTY_STRING;
	private String ssn = ApplicationConstants.EMPTY_STRING;
	private Customer customer;
	private String ssnReturnCode = "0";
	private String[] ssnArray ;
	private ServiceHeader serviceHeader;
	private String  ssnFedTaxID= ApplicationConstants.EMPTY_STRING;
	private String  ssnToken= ApplicationConstants.EMPTY_STRING;
	private String  fedTaxIDToken= ApplicationConstants.EMPTY_STRING;
	private  PieSecurityDetailsVO ssnPieSecurityDtls;
	private Properties aeConfiguration;
	private boolean isVtgSsnValidationFlowEnabled = true;
	private String ssnTaxidReturnCode = "";
	@Override
	protected void postProcess() throws EngineException, ServiceValidationException, ActionValidationException  {
		if (checkIfInContext(Properties.class,ApplicationConstants.AE_CONFIG_PROPERTIES)) {
			aeConfiguration = getInput(Properties.class,
					ApplicationConstants.AE_CONFIG_PROPERTIES);
		} else {
			aeConfiguration = new Properties();
		}
		isVtgSsnValidationFlowEnabled = Boolean.valueOf(aeConfiguration.getProperty("VTG_SSN_VALIDATION_ENABLED", "true"));
			customer=new Customer(ApplicationConstants.EMPTY_STRING);	
			ServiceRequest serviceRequest = getInput(ServiceRequest.class);
			CustomerList custList = new CustomerList();
			if (checkIfInContext(ServiceHeader.class)) {
					serviceHeader = getInput(ServiceHeader.class);
					customer.setBillingSystem(RoutingUtil.getDataGridBillSysFromVisionBillSys(serviceHeader.getBillingSys()));
					feTokenInd = (serviceHeader.getFeTokenIndicator() !=null && serviceHeader.getFeTokenIndicator().trim().length()>0)?serviceHeader.getFeTokenIndicator().trim():"N";
				}
			if (checkIfInContext(SecurityTokens.class)) {
				securityTokens = getInput(SecurityTokens.class);
			}else{
				securityTokens = new SecurityTokens("");
			}
			if (null != serviceRequest) {
				List<Element> requestElements = serviceRequest.getInputs();
				if (null != requestElements) {
					Map<String, String> map = new HashMap<String, String>();
					for (Element element : requestElements) {
						map.put( element.getTagName(), element.getTextContent());
					}
					if(map!=null && map.size() >0){
						custType = map.get("custType");
						ssn = map.get("ssn");
						if(custType!=null && !custType.isEmpty()){
							if(custType.length() > 1 ){
								throw new ServiceValidationException("8075","IWAA0124E: String " +"'"+custType+"'" +" exceeds the maximum length of 1 for parameter cstcc475__cust__type.");
							}
							if(!custType.matches("C|B") ){
								throw new ServiceValidationException("3036","CUST TYPE IS INCORRECT");
							}
							CustomerType type = new CustomerType(custType);
							customer.setType(type);
						}	
						else {
							throw new ActionValidationException("3036","CUST TYPE IS INCORRECT");
						}
						if( !isVtgSsnValidationFlowEnabled && ssn != null && !ssn.isEmpty()){
							ssnArray=getTokenizedOrClearTextValue(ssn.trim()); 
							ssn = ssnArray[1];
							if(!(ssn.matches("[0-9]+"))){
								throw new ServiceValidationException("3037","SSN/TAX ID IS REQUIRED");
							}
							if(null != ssn){
								ssn=  ssn.length() < 9 ? ("000000000" + ssn).substring(ssn.length()) : ssn;
							}
							ssnValidation();
							ssnFedTaxID = ssnArray[0];
							fedTaxIDToken = (custType.matches("B") && !StringUtils.isBlank(ssnFedTaxID) ? ssnFedTaxID.toLowerCase():"''");
							ssnToken = (custType.matches("C") && !StringUtils.isBlank(ssnFedTaxID) ? ssnFedTaxID.toLowerCase():"''");
							customer.setFedTxId(fedTaxIDToken);
							customer.setSsn(ssnToken);
							}
						else if(isVtgSsnValidationFlowEnabled && ssn != null && !ssn.isEmpty()){
							ssnArray=getTokenizedOrClearTextValue(ssn.trim()); 
							ssnTaxidReturnCode = ssnArray[3];
							 if(null != ssnTaxidReturnCode && !ssnTaxidReturnCode.equals("") ) {
								ssnValidationUsingRespCode();
								ssnFedTaxID = ssnArray[0];
								fedTaxIDToken = (custType.matches("B") && !StringUtils.isBlank(ssnFedTaxID) ? ssnFedTaxID.toLowerCase():"''");
								ssnToken = (custType.matches("C") && !StringUtils.isBlank(ssnFedTaxID) ? ssnFedTaxID.toLowerCase():"''");
								customer.setFedTxId(fedTaxIDToken);
								customer.setSsn(ssnToken);
						    }else{
								throw new ServiceValidationException("3045", "ENTERED SSN/ FED TAX ID IS DUMMY, PLEASE ENTER VALID SSN/ FED TAX ID");
							}
							
						}
						else{
							throw new ActionValidationException("3028","Ssn is a required field");
						}
						
					}
					

				}
				setOutput(customer);
				setOutput(custList);	
			}
	}

	private void ssnValidationUsingRespCode() throws  ServiceValidationException {
		 
		if(custType.matches("C|B")){
			 if( ssnTaxidReturnCode.equals("0") || ("B".equalsIgnoreCase(custType) && ssnTaxidReturnCode.equals("11"))){//exception for "B" cust-type
				 //don't throw any exception
			} else if(ssnTaxidReturnCode.matches("3|4|5|2|6|11")) {
				throw new ServiceValidationException("3045", "ENTERED SSN/ FED TAX ID IS DUMMY, PLEASE ENTER VALID SSN/ FED TAX ID");
			}else {
				throw new ServiceValidationException("3045", "ENTERED SSN/ FED TAX ID IS DUMMY, PLEASE ENTER VALID SSN/ FED TAX ID");
			}
		}
	}

	private String[] getTokenizedOrClearTextValue(String input) {
		getSsnPieSecurityDtls().setKeyId(StringUtils.trim(securityTokens.getKeyId()));
		getSsnPieSecurityDtls().setPhaseId(StringUtils.trim(securityTokens.getPhaseId()));
		String[] arr;
		try {
			
			CommonGenericVO commonGenericVo = PIESecureDataImplUtils.populatePieCommonGenericVOFromCommonVO(serviceHeader.getClientId(),serviceHeader.getCorrelationId(),serviceHeader.getUserId());
			String corrId = serviceHeader.getCorrelationId();
			SsnSpiData ssnSpiData = new SsnSpiData(input,getSsnPieSecurityDtls().getKeyId(),getSsnPieSecurityDtls().getPhaseId());
			HashMap<String, PIESecureDataVO> respsecmap = PIESecureDataImplUtils.getVTG_pieSpiDataToClearAndTokenValue(commonGenericVo, PIESecureDataImplUtils.SOURCE_PROGRAM.OTHER, corrId, ssnSpiData);
			arr=PIESecureDataUtils.getVTG_pieSpiTokenizedClearDataFromSpiDataCollection(input, respsecmap);
		} catch (com.vzw.common.crypto.utils.exception.ApplicationException e) {
			throw new SecurityException(e.getMessage());
		}
		return arr;
	}
	
	
	private PieSecurityDetailsVO getSsnPieSecurityDtls() {
		if(null==ssnPieSecurityDtls)ssnPieSecurityDtls=new PieSecurityDetailsVO();
		return ssnPieSecurityDtls;
	}
	private void setSsnPieSecurityDtls(PieSecurityDetailsVO ssnPieSecurityDtls) {
		this.ssnPieSecurityDtls = ssnPieSecurityDtls;
	}

	private void ssnValidation() throws ServiceValidationException {

		if((ssn.length()<9) && ssn.matches("[0-9]+")){
			throw new ServiceValidationException("3045", "ENTERED SSN/ FED TAX ID IS DUMMY, PLEASE ENTER VALID SSN/ FED TAX ID");
		}
		if(custType.matches("C|B")){
			if(ssn.substring(0, 3).matches("000|666|800|900") || ssn.substring(0, 3).equals("")){
				ssnReturnCode = "3";
			}
			else if(ssn.substring(3, 5).matches("00") || ssn.substring(3,5).equals("")){
				ssnReturnCode = "4";
			}
			else if(ssn.substring(5, 9).matches("0000") || ssn.substring(5,9).equals("")){
				ssnReturnCode = "5";
			}
			else if(ssn.matches("000000000|000000001|121212121|123454321|111223333|123121234|333224444|123123123|010101010|111223333|321321321|001001001|987654321|999999998|123456789|123450000" +
					"111111111|222222222|333333333|444444444|555555555|666666666|777777777|888888888|999999999")){
				ssnReturnCode = "2";
			}
			else{
				if(ssn.substring(0, 5).matches("11111|22222|33333|44444|66666|77777|88888|99999")){
					ssnReturnCode = "6";
				}
			}
		}
		if(!ssnReturnCode.equals("0")){
			if(custType.matches("B")){
				if(ssn.substring(0,9).matches("[0-9]+")){
					if(ssn.substring(0,3).matches("800|900")){
					}
					else{
						if(ssn.substring(3,5).matches("00") && (!ssn.equals("000000001") && !ssn.equals("001001001"))){
						}
						else{
							throw new ServiceValidationException("3045", "ENTERED SSN/ FED TAX ID IS DUMMY, PLEASE ENTER VALID SSN/ FED TAX ID");
						}
					}
				}
			}
			else{
				if(!custType.matches("B") || !(ssn.substring(0, 9).matches("[0-9]+"))){
					throw new ServiceValidationException("3045", "ENTERED SSN/ FED TAX ID IS DUMMY, PLEASE ENTER VALID SSN/ FED TAX ID");
				}
			}
		}
	}
}