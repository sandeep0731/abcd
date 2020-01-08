package com.vzw.cops.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import com.vzw.cops.api.common.ServiceHeader;
import com.vzw.cops.engine.StartupBean;
import com.vzw.cops.exception.ActionValidationException;

public final class RoutingUtil extends StartupBean {

	public static final String BILLING_SYS_CONTEXT_NAME = "billingSys";
	
	public static final String DATAGRID_BILLING_SYSTEM_EAST = "1";
	public static final String DATAGRID_BILLING_SYSTEM_WEST = "2";
	public static final String DATAGRID_BILLING_SYSTEM_NORTH = "7";
	public static final String DATAGRID_BILLING_SYSTEM_B2B = "8";
	
	
	private static final String CIDB_DB_INSTANCE_EAST = "E";
	private static final String CIDB_DB_INSTANCE_WEST = "W";
	private static final String CIDB_DB_INSTANCE_NORTH = "N";
	private static final String CIDB_DB_INSTANCE_B2B = "B";
	

	private int northExceptionList[];
	private int eastExceptionList[];
	private int westExceptionList[];
	private int b2bExceptionList[];
	
	protected void initialize() {
		try {
			load("b2bcustomers.txt", BillingSystem.VISION_B2B);
			load("exceptionAccounts.txt", BillingSystem.VISION_NORTH);
			load("eastcustomers.txt", BillingSystem.VISION_EAST);
			load("westcustomers.txt", BillingSystem.VISION_WEST);
		} catch(Throwable t) {
			logger.error("Exception while initializing", t);
		}
	}

	public enum BillingSystem {
		VISION_EAST,
		VISION_WEST,
		VISION_NORTH,
		VISION_B2B
	}

	public String getAndUpdateBillingSystemForCustomerId(String customerId, ServiceHeader serviceHeader) {
		int customerIdIntValue = Integer.parseInt(customerId);

		if (checkException(customerIdIntValue, BillingSystem.VISION_B2B)) {
			serviceHeader.setBillingSys(BillingSystem.VISION_B2B.name());
			return DATAGRID_BILLING_SYSTEM_B2B;
		} else if (checkException(customerIdIntValue, BillingSystem.VISION_EAST)) {
			serviceHeader.setBillingSys(BillingSystem.VISION_EAST.name());
			return DATAGRID_BILLING_SYSTEM_EAST;
		} else if (checkException(customerIdIntValue, BillingSystem.VISION_WEST)) {
			serviceHeader.setBillingSys(BillingSystem.VISION_WEST.name());
			return DATAGRID_BILLING_SYSTEM_WEST;		
		}
		
		int custIdInt = Integer.parseInt(customerId);		
		
		//Only the right most 8 digits of the custId are taken in to consideration for calculation of Billing System 
		custIdInt = custIdInt % 100000000;
			 
        if (40000000 <= custIdInt && custIdInt <= 49999999) {
        	serviceHeader.setBillingSys(BillingSystem.VISION_B2B.name());
        	return DATAGRID_BILLING_SYSTEM_B2B;
        } else if (1 <= custIdInt && custIdInt <= 39999999) {
        	serviceHeader.setBillingSys(BillingSystem.VISION_EAST.name());
			return DATAGRID_BILLING_SYSTEM_EAST;
        } else if (60000000 <= custIdInt && custIdInt <= 79999999) {
        	serviceHeader.setBillingSys(BillingSystem.VISION_WEST.name());
			return DATAGRID_BILLING_SYSTEM_WEST;
        } else if (80000000 <= custIdInt && custIdInt <= 99999999) {
			if (checkException(customerIdIntValue, BillingSystem.VISION_NORTH)) {
				serviceHeader.setBillingSys(BillingSystem.VISION_EAST.name());
				return DATAGRID_BILLING_SYSTEM_EAST;
			}
			
			serviceHeader.setBillingSys(BillingSystem.VISION_NORTH.name());
			return DATAGRID_BILLING_SYSTEM_NORTH;
		}
        
		serviceHeader.setBillingSys(BillingSystem.VISION_EAST.name());
		return DATAGRID_BILLING_SYSTEM_EAST; // Should never get here - Will
	}

	private boolean checkException(int customerId, BillingSystem billingSystem) {
		switch(billingSystem) {
		case VISION_B2B:
			return Arrays.binarySearch(b2bExceptionList, customerId) >= 0;
		case VISION_NORTH:
			return Arrays.binarySearch(northExceptionList, customerId) >= 0;
		case VISION_EAST:
			return Arrays.binarySearch(eastExceptionList, customerId) >= 0;
		case VISION_WEST:
			return Arrays.binarySearch(westExceptionList, customerId) >= 0;
		default:
			return false;
		}
	}

	public void load(String fileName, BillingSystem billingSystem) throws IOException, URISyntaxException {
		ClassLoader cl = RoutingUtil.class.getClassLoader();

		// Load file
		logger.info("Loading file " + fileName);
		File file = new File(cl.getResource(fileName).toURI());

		// Approximate entries in the file
		logger.info("Approximating file line count");
		int approxLineCount = approximateLineCount(file);
		
		// Create temp array
		logger.info("Creating array of size " + approxLineCount);
		int fileEntries[] = new int[approxLineCount];

		// Read all the entries
		logger.info("Reading file entries into array");
		int count = 0;
		FileReader fin = new FileReader(file);      //sonar fix
		BufferedReader in = new BufferedReader(fin);  
		//BufferedReader in = new BufferedReader(new FileReader(file));  //old
		try {
			String inputLine = "";

			while ((inputLine = in.readLine()) != null){
				inputLine = inputLine.trim();
				if (inputLine.length() > 0) {
					fileEntries[count++] = Integer.parseInt(inputLine);
				}
			}
		} catch (Exception e) {
			logger.info("Exception while reading file entries", e);
		} finally {
			try {
				fin.close(); //sonar fix
				in.close();
			} catch(IOException ioe){
				logger.info("Exception while closing the open reader", ioe);
			}
		}

		// Compact
		logger.info("Compacting the exception list from " + fileEntries.length + " to exact size " + count);
		int exceptions[] = new int[count];
		System.arraycopy(fileEntries, 0, exceptions, 0, count);
		fileEntries = null;

		// Sort
		logger.info("Sorting exception list");
		Arrays.sort(exceptions);

		// Persist
		synchronized(RoutingUtil.class) {
			switch(billingSystem) {
			case VISION_B2B:
				logger.info("Persisting as B2B exception list");
				b2bExceptionList = exceptions;
				break;
			case VISION_NORTH:
				logger.info("Persisting as North exception list");
				northExceptionList = exceptions;
				break;
			case VISION_EAST:
				logger.info("Persisting as East exception list");
				eastExceptionList = exceptions;
				break;
			case VISION_WEST:
				logger.info("Persisting as West exception list");
				westExceptionList = exceptions;
				break;
			}
		}

	}

	private static int approximateLineCount(File file) throws IOException {
		long fileSizeInBytes = file.length();

		// Sample 1/20th of the file
		long sampleSizeInBytes = fileSizeInBytes / 10;

		int lines = 0;
		long bytesRead = 0;

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			byte[] buffer = new byte[8 * 1024];
			int read;
	
			// Count the line feeds in the sample
			while (((read = fis.read(buffer)) != -1) && (bytesRead < sampleSizeInBytes)) {
				bytesRead += read;
				for (int i = 0; i < read; i++) {
					if (buffer[i] == '\n')
						lines++;
				}
			}
		} finally {
			IOUtils.closeQuietly(fis);
		}

		// Multiply the sample by 12 (gives us ~20% wiggle room, it's okay we'll compact the array...)
		lines *= 12;

		return lines;
	}
	
	public static String getDataGridBillSysFromVisionBillSys(BillingSystem billingSystem) throws ActionValidationException {
		ValidateUtil.validateNotNull(billingSystem, "Billing system");
		switch(billingSystem) {
		case VISION_EAST:
			return DATAGRID_BILLING_SYSTEM_EAST;
		case VISION_NORTH:
			return DATAGRID_BILLING_SYSTEM_NORTH;
		case VISION_WEST:
			return DATAGRID_BILLING_SYSTEM_WEST;
		case VISION_B2B:
			return DATAGRID_BILLING_SYSTEM_B2B;
		}
		throw new ActionValidationException("Unexpected billing system supplied");
	}
	
	public static String getDataGridBillSysFromVisionBillSys(String billingSystem) throws ActionValidationException {
		ValidateUtil.validateNotNull(billingSystem, "Billing system");
		if (billingSystem.equalsIgnoreCase(BillingSystem.VISION_EAST.name())) {
			return DATAGRID_BILLING_SYSTEM_EAST;
		} else if (billingSystem.equalsIgnoreCase(BillingSystem.VISION_NORTH.name())) {
			return DATAGRID_BILLING_SYSTEM_NORTH;
		} else if (billingSystem.equalsIgnoreCase(BillingSystem.VISION_WEST.name())) {
			return DATAGRID_BILLING_SYSTEM_WEST;
		} else if (billingSystem.equalsIgnoreCase(BillingSystem.VISION_B2B.name())) {
			return DATAGRID_BILLING_SYSTEM_B2B;
		} 
		throw new ActionValidationException("Unexpected billing system supplied");
	}
	
	public static String getVisionBillSysFromDataGridBillSysId(String billSysId) throws ActionValidationException {
		ValidateUtil.validateNotNull(billSysId, "billSysId");
		if (billSysId.equalsIgnoreCase(DATAGRID_BILLING_SYSTEM_EAST)) {
			return BillingSystem.VISION_EAST.name();
		} else if (billSysId.equalsIgnoreCase(DATAGRID_BILLING_SYSTEM_NORTH)) {
			return BillingSystem.VISION_NORTH.name();
		} else if (billSysId.equalsIgnoreCase(DATAGRID_BILLING_SYSTEM_WEST)) {
			return BillingSystem.VISION_WEST.name();
		} else if (billSysId.equalsIgnoreCase(DATAGRID_BILLING_SYSTEM_B2B)) {
			return BillingSystem.VISION_B2B.name();
		} 
		throw new ActionValidationException("Unexpected billSysId supplied");
	}
	
	public static String getDbInstanceForCidb(String billingSystem){
		if(billingSystem.equals(DATAGRID_BILLING_SYSTEM_B2B)) {
			return CIDB_DB_INSTANCE_B2B;
		}
		if(billingSystem.equals(DATAGRID_BILLING_SYSTEM_EAST)) {
			return CIDB_DB_INSTANCE_EAST;
		}
		if(billingSystem.equals(DATAGRID_BILLING_SYSTEM_WEST)) {
			return CIDB_DB_INSTANCE_WEST;
		}
		if(billingSystem.equals(DATAGRID_BILLING_SYSTEM_NORTH)) {
			return CIDB_DB_INSTANCE_NORTH;
		}		
		return null;
	}
}
