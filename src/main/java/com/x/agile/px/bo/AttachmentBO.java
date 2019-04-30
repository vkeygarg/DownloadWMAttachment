package com.x.agile.px.bo;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.AgileSessionFactory;
import com.agile.api.IAgileObject;
import com.agile.api.IAgileSession;
import com.agile.api.IDataObject;
import com.agile.api.IManufacturerPart;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.ManufacturerPartConstants;
import com.x.agile.px.utility.Utility;

public class AttachmentBO {
	static Logger logger = Logger.getLogger(AttachmentBO.class);
	IAgileSession session;
	static Properties props = getProperties();
	public String getAglobjName() {
		return aglobjName;
	}

	public void setAglobjName(String aglobjName) {
		this.aglobjName = aglobjName;
	}

	public int getAglObjType() {
		return aglObjType;
	}

	public void setAglObjType(int aglObjType) {
		this.aglObjType = aglObjType;
	}

	String aglobjName;
	int aglObjType;
	Object aglObjId;
	

	public Object getAglObjId() {
		return aglObjId;
	}

	public void setAglObjId(Object aglObjId) {
		this.aglObjId = aglObjId;
	}

	public List<Map<String, String>> getAttachmentFiles(HttpServletRequest request) {
		List<Map<String, String>> attMapList = null;
		session = getAgileSessionFromRequest(request);
		logger.info("getAttachmentFiles session=null? " + (session == null));
		if (session != null) {
			IAgileObject aglObject;
			try {
				aglObject = session.getObject(null, request);
				logger.info("Agile Object: " + aglObject.getName() + "--" + aglObject.getClass() + " -- type:"
						+ aglObject.getType());
				if (aglObject instanceof IDataObject) {
					IDataObject dataObj = (IDataObject) aglObject;
					setAglobjName(dataObj.getName());
					setAglObjType(dataObj.getType());
					setAglObjId(dataObj.getId());
					String aglObjName = aglObject.getName();
					if (aglObject.getType() == IManufacturerPart.OBJECT_TYPE) {
						aglObjName = ((IManufacturerPart) aglObject)
								.getValue(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_NAME).toString() + "~"
								+ aglobjName;
						logger.info("Its a MPN: " + aglObjName);
					}
					attMapList = getFileList(dataObj, aglObjName);
					logger.info("# of Attachment: " + attMapList.size());
				}
			} catch (APIException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return attMapList == null ? new ArrayList<>() : attMapList;
	}

	private List<Map<String, String>> getFileList(IDataObject dataObj, String aglObjName) throws APIException {
		List<Map<String, String>> attMapList = new ArrayList<>();
		ITable attTab = Utility.getAttTable(dataObj, "", "");
		if (attTab != null) {
			logger.info("AttTab.size(): " + attTab.size());
			ITwoWayIterator itr = attTab.getTableIterator();
			Map<String, String> attMap;
			IRow row;
			int i = 0;
			while (itr.hasNext()) {
				row = (IRow) itr.next();
				String fileType = row.getCell(3624).getValue().toString();
				logger.info("----->" + (++i) + " --" + row.getCell(1046).getValue().toString()+" type: "+fileType);
				
				if (props.getProperty("ATTACHMENT_PDF_TYPE_CRITERIA_VAL").equalsIgnoreCase(fileType)) {
					logger.info("Valid FileType: "+props.getProperty("ATTACHMENT_PDF_TYPE_CRITERIA_VAL"));
					attMap = new HashMap<>();
					attMap.put("filename", row.getCell(1046).getValue().toString());
					attMap.put("filedescription", row.getCell(1045).getValue().toString());
					attMap.put("filetype", row.getCell(3624).getValue().toString());
					attMap.put("filesize", row.getCell(3623).getValue().toString());
					attMap.put("objname", aglObjName);
					attMapList.add(attMap);
				}
			}
		}
		return attMapList;
	}

	public IAgileSession getAgileSessionFromRequest(HttpServletRequest request) {
		IAgileSession algSession = this.session;
		String agUrl = props.getProperty("AGILEURL");
		if (algSession == null) {
			try {
				logger.info("Createing new Agile session...");
				Cookie[] cookies = request.getCookies();
				logger.info("Cookies : ");
				for (Cookie c : cookies) {
					logger.info(c.getName() + "-" + c.getValue());
				}
				logger.info("AGILEURL: "+agUrl);
				AgileSessionFactory factory = AgileSessionFactory.getInstance(agUrl);
				logger.info("Connecting to Agile URL: ["+agUrl+"]");
				

				HashMap<Integer,String> params = new HashMap<>();
				String username = null;
				String pwd = null;
				for (int i = 0; i < cookies.length; i++) {
					if (cookies[i].getName().equals("j_username"))
						username = cookies[i].getValue();
					else if (cookies[i].getName().equals("j_password"))
						pwd = cookies[i].getValue();
				}
				params.put(AgileSessionFactory.PX_USERNAME, username);
				params.put(AgileSessionFactory.PX_PASSWORD, pwd);
				algSession = factory.createSession(params);
				logger.info("Connected!");
			} catch (Exception e) {
				logger.info("FAILURE GETTING SESSION FROM HTTP REQUEST, Stacktrace below:");
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.info("Using existing agile session");
		}

		return algSession;
	}
	
	public static Properties getProperties() {
		logger.info("Utils.getProperties:START");
		Properties props = new Properties();
		try {
			String proFileVar = System.getenv("amo.properties.path") == null ? "/Users/vg950772/Downloads/" : System.getenv("amo.properties.path");
			Path propFileDir = Paths.get(proFileVar);
			Path proFile = null;
			if (propFileDir != null && propFileDir.toFile().exists()){
				proFile = propFileDir.getParent().resolve("DownLoadWMAttachment.properties");
				logger.info("propFilePath: [" + proFile + "]");
				if (proFile ==null)
					proFile = Paths.get("E:/Agile/Agile936/DownLoadWMAttachment.properties");
				if(proFile.toFile().exists())
					props.load(new FileReader(proFile.toFile()));
				else {
					logger.info(proFile+" not found!!!");
				}
			}else{
				logger.info("Env variable (amo.properties.path) was not found!");
			}
		}
		catch(IOException e) {
			logger.error(e.getMessage(),e);
		}
		logger.info("Utils.getProperties:END");
		return props;
	}
}
