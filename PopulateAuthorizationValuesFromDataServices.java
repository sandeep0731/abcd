package com.vzw.cops.action.dao.dataservices;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vzw.cops.action.dao.base.DataServicesDao;
import com.vzw.cops.api.common.ServiceHeader;
import com.vzw.cops.constants.ErrorConstants;
import com.vzw.cops.constants.DataServicesConstants.COR_RetrieveShareLinesInAcctLevelPPlanMap;
import com.vzw.cops.constants.DataServicesConstants.SfOffrMap;
import com.vzw.cops.engine.exception.EngineException;
import com.vzw.cops.entity.Customer;
import com.vzw.cops.exception.ApplicationException;
import com.vzw.cops.exception.BusinessValidationException;
import com.vzw.cops.exception.ExternalResourceException;
import com.vzw.cops.exception.ServiceValidationException;
import com.vzw.cops.exception.SysDataNotFoundException;
import com.vzw.cops.util.DataGridExpression;
import com.vzw.cops.util.DataServicesMapUtil;
import com.vzw.cops.util.DataValidateUtil;
import com.vzw.cops.util.DateUtil;
import com.vzw.cops.util.RoutingUtil;
import com.vzw.cops.util.DateUtil.Format;
import com.vzwcorp.dagv.data.model.OrdLnItemOrdNoModel;
import com.vzwcorp.dagv.data.model.ReferenceXrefModel;
import com.vzwcorp.dagv.data.model.SfMtnModel;
import com.vzwcorp.dagv.data.service.Response;


public class PopulateAuthorizationValuesFromDataServices extends DataServicesDao {
	protected ServiceHeader serviceHeader;
	protected Customer customer;
	protected String currentDate;
	protected String requestType;
	protected String functionName;
	protected String subFuctionName;
	private JSONArray rollIdMap;

	@Override
	protected void prevalidate() throws ApplicationException, EngineException {
		
		serviceHeader = getInput(ServiceHeader.class);
		customer = getInput(Customer.class);		
		String billingSystem = RoutingUtil.getDataGridBillSysFromVisionBillSys(serviceHeader.getBillingSys());
		currentDate = DateUtil.getCurrent(billingSystem, Format.DG_DATE);
	}

	@Override
	protected String getTypedefId() { 
		return "COR_securityFunctionsForCustList";
	}

	@Override 
	protected List<DataGridExpression> getTypedefExpressionList() {
		List<DataGridExpression> paramList = new ArrayList<DataGridExpression>();
		paramList.add(new DataGridExpression("requestType","LV"));
		paramList.add(new DataGridExpression("functionName", "functionAllowCustomerTypes"));
		paramList.add(new DataGridExpression("currentDate", currentDate));
		paramList.add(new DataGridExpression("jobTitleId",serviceHeader.getSecurityClass()));//securityClass
		return paramList;
	}
	
	@Override
	protected void populateEntities(Response resp) throws BusinessValidationException, ExternalResourceException, EngineException{ 
		
		Map<String,Object> resultMap = resp.getResults();
		if(resultMap == null){
			return;
		}
		List<ReferenceXrefModel> secRoleList = convertToListValue(resultMap.get("secRole"), new TypeReference<ArrayList<ReferenceXrefModel>>(){});
		JSONObject jsonObj=DataServicesMapUtil.getJsonObject(resultMap);
		Object[] rollIdKeyList = {"ck1Value"};
		rollIdMap=DataServicesMapUtil.getJsonArray(jsonObj, rollIdKeyList,"secRoleFunctionAllowedGeneric");
		int secRollFunValCnt = 0;
		String allowIndicator = "P";
		Set<String> functionValues = new HashSet<String>();
		if(secRoleList!=null && !secRoleList.isEmpty()){
			for(ReferenceXrefModel secRolId:secRoleList){
				String rollIdKey = secRolId.getPk1Value()!= null?  secRolId.getPk1Value():EMPTY_STRING;
				JSONArray secRollFunValArr = DataServicesMapUtil.getJsonArrayUsingKey(rollIdMap,rollIdKey.toString());
				if (null != secRollFunValArr && !EMPTY_ARRAY.equals(secRollFunValArr.toString())) {
					List<ReferenceXrefModel> secRollFunValElements = DataServicesMapUtil.convertToListValueFromJsonArray(secRollFunValArr, ReferenceXrefModel.class);
					if(secRollFunValElements != null && !secRollFunValElements.isEmpty()){
						secRollFunValCnt = secRollFunValCnt + secRollFunValElements.size();
							if(secRollFunValCnt > 501){
								break;
							}
							for(ReferenceXrefModel finalSecFunc: secRollFunValElements){
								String subFuncValue = finalSecFunc.getNk2Value();
								functionValues.add(subFuncValue);
						   }
					}	
				}
			}
		}
		
		if(secRollFunValCnt > 1){
			allowIndicator = "A";
		}
		
		if("A".equalsIgnoreCase(allowIndicator)){ 
			if(functionValues.size() > 50){
				throw new BusinessValidationException("1462", "INTERNAL TABLE OVERFLOW.  CONTACT IT");
			}
		}
		
		
		setOutput(allowIndicator, "allowIndicator"); 
		setOutput(functionValues, "functionValues"); 
	}
}
