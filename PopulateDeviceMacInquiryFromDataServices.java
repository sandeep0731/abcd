package com.vzw.cops.action.dao.dataservices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vzw.cops.action.dao.base.DataServicesDao;
import com.vzw.cops.api.common.ServiceError;
import com.vzw.cops.api.common.ServiceHeader;
import com.vzw.cops.api.common.ServiceWarning;
import com.vzw.cops.constants.ApplicationConstants;
import com.vzw.cops.constants.DataServicesConstants.MapName;
import com.vzw.cops.constants.DataServicesConstants.COR_DeviceMacInquiryKeys;
import com.vzw.cops.constants.DataServicesConstants.COR_DeviceMacInquiryMapKeys;
import com.vzw.cops.engine.exception.EngineException;
import com.vzw.cops.entity.Account;
import com.vzw.cops.entity.CommonBodyError;
import com.vzw.cops.entity.CommonBodyMessage;
import com.vzw.cops.entity.CommonBodyWarning;
import com.vzw.cops.entity.Customer;
import com.vzw.cops.entity.Device;
import com.vzw.cops.entity.Line;
import com.vzw.cops.entity.list.CustomerList;
import com.vzw.cops.entity.list.LineList;
import com.vzw.cops.exception.ActionValidationException;
import com.vzw.cops.exception.ApplicationException;
import com.vzw.cops.exception.BusinessOverrideException;
import com.vzw.cops.exception.BusinessValidationException;
import com.vzw.cops.exception.DataNotFoundException;
import com.vzw.cops.exception.ExternalResourceException;
import com.vzw.cops.exception.InternalResourceException;
import com.vzw.cops.exception.SysDataNotFoundException;
import com.vzw.cops.util.DataGridExpression;
import com.vzw.cops.util.RoutingUtil;
import com.vzwcorp.dagv.data.model.LnOfSvcCustP1P2Model;
import com.vzwcorp.dagv.data.model.NonReferenceXrefModel;
import com.vzwcorp.dagv.data.service.Response;

public class PopulateDeviceMacInquiryFromDataServices extends DataServicesDao {

	protected String deviceId = EMPTY_STRING;
	protected Device device;
	protected String deviceIdTyp = EMPTY_STRING;
	private final String DEVICE_ID = "deviceId";
	private String CMAXTIMESTAMP = "9999-12-31-23.59.59.999999";
	private CustomerList customerList;
	
	private CommonBodyMessage commonBodyMessage;
	private CommonBodyError commonBodyError;
	private CommonBodyWarning commonBodyWarning;
	protected ServiceHeader serviceHeader;
	
	public enum BillingSystem {
		VISION_EAST,
		VISION_WEST,
		VISION_NORTH,
		VISION_B2B
	}
	
	@Override
	protected String getTypedefId() {
		return MapName.COR_DeviceMacInquiry.toString();
	}

	@Override
	protected List<DataGridExpression> getTypedefExpressionList()
			throws ActionValidationException {
		List<DataGridExpression> inputParamList = new ArrayList<DataGridExpression>();
		inputParamList.add(new DataGridExpression(
				COR_DeviceMacInquiryKeys.deviceId, deviceId));
		return inputParamList;
	}

	@Override
	protected void populateEntities(Response resp)
			throws DataNotFoundException, ActionValidationException,
			ExternalResourceException, InternalResourceException,
			EngineException, SysDataNotFoundException,
			BusinessValidationException, BusinessOverrideException {
		
	
		Map<String, Object> resultMap = resp.getResults();
		if (resultMap == null) {
			return;
		}
		
		List<NonReferenceXrefModel> lnDvcInfoList =  convertToListValue(resultMap.get(COR_DeviceMacInquiryMapKeys.lnPrimIdDvcOnly.toString()),
				new TypeReference<ArrayList<NonReferenceXrefModel>>() {});
		
		List<LnOfSvcCustP1P2Model> lnOfSvcP2P1List =  convertToListValue(resultMap.get(COR_DeviceMacInquiryMapKeys.lnOfSvcP2P1.toString()),
				new TypeReference<ArrayList<LnOfSvcCustP1P2Model>>() {});
		
		Device device =null;
		Customer customer = null;
		customerList = new CustomerList();
		if(null!=lnOfSvcP2P1List && !lnOfSvcP2P1List.isEmpty() && null!=lnDvcInfoList && !lnDvcInfoList.isEmpty()) {
			Set<String> billingSystems = new HashSet<String>();
			for(LnOfSvcCustP1P2Model lnOfSvcP2P1ListInfo :lnOfSvcP2P1List) {
					for (NonReferenceXrefModel lnDvcInfoListInfo :lnDvcInfoList ) {
						if(lnOfSvcP2P1ListInfo.getLnOfSvcIdNoP2().toString().equals(lnDvcInfoListInfo.getCk1Value()) 
								&& lnOfSvcP2P1ListInfo.getLnOfSvcIdNoP1().toString().equals(lnDvcInfoListInfo.getCk3Value())
								&& billingSystems.add(RoutingUtil.getVisionBillSysFromDataGridBillSysId(lnDvcInfoListInfo.getBillSysId().toString()))) {
							customer = new Customer(lnOfSvcP2P1ListInfo.getCustIdNo().toString());
							Account acct = new Account(customer,lnOfSvcP2P1ListInfo.getAcctNo().toString());
							customer.addAccount(acct);
							customer.setBillingSystem(lnDvcInfoListInfo.getBillSysId().toString());
							customer.setVisionBillingSystem(RoutingUtil.getVisionBillSysFromDataGridBillSysId(customer.getBillingSystem()));
							device = new Device(lnDvcInfoListInfo.getPk1Value());
							String timeStamp= lnDvcInfoListInfo.getNk2Value();
							device.setDeviceIndicator("Y");
							if(null!= timeStamp && timeStamp.equals(CMAXTIMESTAMP)) {
								device.setDeviceActiveIndicator("Y");
								errorAndWarning("1507", customer, acct);   
							} else {
								device.setDeviceActiveIndicator("N");
							}
							LineList lineList = new LineList();
							Line line = new Line(ApplicationConstants.EMPTY_STRING);
						
							line.setDevice(device);														
							lineList.add(line);
							acct.setLineList(lineList);
							customerList.add(customer);
						}
					}
			}
			for(BillingSystem bs : BillingSystem.values()) {
				if(billingSystems.add(bs.toString())) {
					customerList.add(createEmptyCustomer(bs.toString()));
				}
			}
		} else {
			customerList.add(createEmptyCustomer(BillingSystem.VISION_EAST.toString()));
			customerList.add(createEmptyCustomer(BillingSystem.VISION_WEST.toString()));
			customerList.add(createEmptyCustomer(BillingSystem.VISION_NORTH.toString()));
			customerList.add(createEmptyCustomer(BillingSystem.VISION_B2B.toString()));
		}
		setOutput(customerList);
	}
	
	private void errorAndWarning(String errorCode, Customer cust, Account acct ) {		
		commonBodyMessage = new CommonBodyMessage(cust);
		commonBodyError = new CommonBodyError(commonBodyMessage);
		commonBodyWarning = new CommonBodyWarning(commonBodyMessage);
		
		commonBodyError.setErrorCode("00");
		commonBodyWarning.setWarningCode("0");
		
		if (!errorCode.isEmpty()) {
			commonBodyError.setErrorCode(errorCode);
		} 
		
		commonBodyMessage.setCommonBodyError(commonBodyError);
		commonBodyMessage.setCommonBodyWarning(commonBodyWarning);
		cust.setCommonBodyMessage(commonBodyMessage);
		ServiceWarning warning = new ServiceWarning();
		warning.setWarningCode(errorCode);
		String warningMessage = "MAC ID IS ALREADY ACTIVE FOR CUSTOMER " + cust.getId() + "-" + acct.getNumber();
		warning.setWarningMsg(warningMessage);
		serviceHeader.addWarning(warning);
	}

	@Override
	protected void prevalidate() throws ApplicationException, EngineException {
		serviceHeader = getInput(ServiceHeader.class); 
		device = getInput(Device.class);
		deviceId = device.getId();
		if(deviceId.length() > 14){
			String errorMessage = "String '" + deviceId + "' exceeds the maximum length of 14 for parameter cstcc622__device__id.";
			errorAndWarning("3067", errorMessage);
		}
	}

	private void errorAndWarning(String errorCode, String errorMsg) {
		commonBodyMessage = new CommonBodyMessage(device);
		commonBodyError = new CommonBodyError(commonBodyMessage);
		commonBodyWarning = new CommonBodyWarning(commonBodyMessage);
		
		commonBodyError.setErrorCode("00");
		commonBodyWarning.setWarningCode("0");
		
		if (!errorCode.isEmpty()) {
			commonBodyError.setErrorCode(errorCode);
		} 
		if (!errorMsg.isEmpty()) {
			commonBodyError.setErrorMsg(errorMsg);
		}
		commonBodyMessage.setCommonBodyError(commonBodyError);
		commonBodyMessage.setCommonBodyWarning(commonBodyWarning);
		device.setCommonBodyMessage(commonBodyMessage);
		ServiceWarning warning = new ServiceWarning();
		warning.setWarningCode(errorCode);
		serviceHeader.addWarning(warning);
		serviceHeader.setStatusCode("99");
		ServiceError error = new ServiceError();
		error.setErrorCode(errorCode);
		error.setErrorMsg(errorMsg);
		List<ServiceError> errors = new ArrayList<ServiceError>();
		errors.add(error);
		serviceHeader.setError(errors);
		warning.setWarningMsg(errorMsg);
	}
	
	private Customer createEmptyCustomer(String billingSystem) {
		Customer customer = new Customer("");
		customer.setVisionBillingSystem(billingSystem);
		if (BillingSystem.VISION_EAST.toString().equals(billingSystem.toString())) {
			customer.setBillingSystem("1");
		} else if (BillingSystem.VISION_WEST.toString().equals(billingSystem.toString())) {
			customer.setBillingSystem("2");
		} else if (BillingSystem.VISION_NORTH.toString().equals(billingSystem.toString())) {
			customer.setBillingSystem("7");
		} else {
			customer.setBillingSystem("8");
		}
		return customer;
	}
}