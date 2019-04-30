package com.x.agile.px.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.agile.api.APIException;
import com.agile.api.IAgileSession;
import com.agile.api.IAttachmentFile;
import com.agile.api.IChange;
import com.agile.api.IDataObject;
import com.agile.api.IItem;
import com.agile.api.IManufacturer;
import com.agile.api.IManufacturerPart;
import com.agile.api.IRow;
import com.agile.api.ITable;
import com.agile.api.ITwoWayIterator;
import com.agile.api.IUser;
import com.agile.api.ManufacturerPartConstants;
import com.x.agile.px.bo.AttachmentBO;
import com.x.agile.px.utility.Utility;

@WebServlet(description = "Download File From The Server", urlPatterns = { "/downloadServlet" })
public class FileDownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	static Logger logger = Logger.getLogger(FileDownloadServlet.class);
	static Properties prop = AttachmentBO.getProperties();
	static String [] positions = {"TOP_LEFT","TOP_CENTER","TOP_RIGHT","BOTTOM_LEFT","BOTTOM_CENTER","BOTTOM_RIGHT"};
	static final  String CURDATEFRMT = prop.getProperty("CURRENT_DATE_FORMAT") == null ? "yyyy-MM-dd HH:mm:ss" :prop.getProperty("CURRENT_DATE_FORMAT");
	static final  String AGLDATEFRMT = prop.getProperty("AGILE_DATE_FORMAT") == null ? "yyyy-MM-dd HH:mm:ss zzz" :prop.getProperty("AGILE_DATE_FORMAT");
	static final  String AGLDATE_TIMEZONE = prop.getProperty("AGILE_DATE_TIMEZONE") == null ? "UTC" :prop.getProperty("AGILE_DATE_TIMEZONE");

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			handleRequest(request, response);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String aglObjName = request.getParameter("objname");
		String fileName = request.getParameter("filename");
		File orgFile = null;
		File wmFile = null;
		logger.info("Download file: " + fileName + " from Obj:" + aglObjName);
		try {
			IDataObject aglObj = getAgileObject(request, aglObjName);
			String curUser = getCurrentUser(request);
			if (aglObj != null) {
				orgFile = getAttFile(aglObj, fileName);
				logger.info("Attachment File null?: " + (orgFile == null));
				if (orgFile != null && orgFile.exists()) {
					logger.info("Attachment File: " + orgFile.getAbsolutePath());
					Path outDir = Paths.get(orgFile.getParent()).resolve("ourdir");
					if (fileName.toLowerCase().endsWith(".pdf")) {
						Map<String, String> paramMap = getMetaData(aglObj,fileName, curUser);
						wmFile(orgFile.getAbsolutePath(), outDir.toAbsolutePath().toString(), paramMap);
					}
					Path wmFilePath = outDir.resolve(fileName);
					wmFile = wmFilePath != null && wmFilePath.toFile().exists() ? wmFilePath.toFile() : orgFile;
					logger.info("WM File: " + wmFile.getAbsolutePath());
					if (wmFile.exists()) {
						sendFile(response, wmFile);
						return;
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		response.setContentType("text/html");
		response.getWriter().println("<h3>File " + fileName + " is not available in Agile, Please contact Agile administrator.</h3>");
	}

	private String getCurrentUser(HttpServletRequest request) throws APIException {
		String curUser = "";
		AttachmentBO boObj = (AttachmentBO) request.getSession().getAttribute("AGL_OBJ");
		logger.debug("if BO Obj null?" + (boObj == null));
		if (boObj != null) {
			IAgileSession session = boObj.getAgileSessionFromRequest(request);
			curUser = session.getCurrentUser().getName();
		}
		return curUser;
	}

	private Map<String, String> getMetaData(IDataObject aglObj, String fileName, String curUser) {
		Map<String, String> paramMap = new HashMap<>();
		if (aglObj != null) {
			try {
				String propPreFix = getAglClassPrefix(aglObj);
				String strBaseId;
				for (String pos : positions) {
					strBaseId = prop.getProperty(propPreFix + pos);
					if (!StringUtils.isEmpty(strBaseId)) {
						String val;
						if (strBaseId.indexOf(':') > -1) {
							val = strBaseId.split(":")[0] + ":" + getAttrVal(aglObj, strBaseId.split(":")[1], fileName, curUser);
						} else {
							val = getAttrVal(aglObj, strBaseId, fileName, curUser);
						}
						paramMap.put(pos, val);
					}
				}
			} catch (NumberFormatException | APIException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return paramMap;
	}

	private String getAttrVal(IDataObject aglObj, String strBaseId, String fileName, String curUser) {
		Object val = null;
		if (StringUtils.isNotEmpty(strBaseId)) {
			switch (strBaseId) {
			case ("$USER"):
				val = curUser;
				break;
			case ("$FILENAME"):
				val = fileName;
				break;
			case ("$DATE"):
				Date date = new Date();
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(CURDATEFRMT);
				val = simpleDateFormat.format(date);
				break;
			default:
				try {
					val = StringUtils.isNumeric(strBaseId) ? aglObj.getValue(Integer.parseInt(strBaseId))
							: aglObj.getValue(strBaseId);
					if(val instanceof Date) {
						Date datVal = (Date)val;
						logger.info("Is a date Agile Date: "+datVal.toString());
						TimeZone timeZone = TimeZone.getTimeZone(AGLDATE_TIMEZONE);
						SimpleDateFormat df = new SimpleDateFormat(AGLDATEFRMT);
						df.setTimeZone(timeZone);
						val = df.format(datVal);
						logger.info("Is a date Format Date: "+val.toString());
					}
				} catch (NumberFormatException | APIException e) {
					logger.error(e.getMessage(), e);
				}
				break;
			}
		}
		
		return val == null ? "" : val.toString();
	}

	private String getAglClassPrefix(IDataObject aglObject) throws APIException {
		String propPreFix;
		logger.info(aglObject.getAgileClass().getClass() + "--" + aglObject.getAgileClass().getSuperClass());
		propPreFix = aglObject.getAgileClass().getSuperClass().getName().replace(" ", "_").toUpperCase() + "_";
		logger.info("Agile sub CLass Prefix: " + propPreFix);
		return propPreFix;
	}

	private void sendFile(HttpServletResponse response, File wmFile) throws IOException {
		String mimeType = "application/octet-stream";
		response.setContentType(mimeType);

		String headerKey = "Content-Disposition";
		String headerValue = String.format("attachment; filename=\"%s\"", wmFile.getName());
		response.setHeader(headerKey, headerValue);

		try (OutputStream outStream = response.getOutputStream();
				FileInputStream inputStream = new FileInputStream(wmFile)) { 
			IOUtils.copy(inputStream, outStream);
			outStream.flush();
		}
	}

	private File getAttFile(IDataObject aglObj, String fileName) throws IOException, APIException {
		File orgFile = null;

		ITable attTable = Utility.getAttTable(aglObj, "","");
		if (attTable != null) {
			ITwoWayIterator itr = attTable.getTableIterator();
			IRow row;
			while(itr.hasNext()) {
				row = (IRow)itr.next();
				if(fileName.equals(row.getCell(1046).getValue().toString())) {
					IAttachmentFile rowObj = ((IAttachmentFile) row);
					orgFile = Paths.get(prop.getProperty("TMP_DIR")).resolve(fileName).toFile();
					try (InputStream inputStream = rowObj.getFile(); FileOutputStream fos = new FileOutputStream(orgFile)) {
						fos.write(IOUtils.toByteArray(inputStream));
						fos.flush();
					}
				}
			}
			
		}
		return orgFile;
	}

	private IDataObject getAgileObject(HttpServletRequest request, String aglObjName) throws APIException {
		IDataObject dataObj = null;
		AttachmentBO boObj = (AttachmentBO) request.getSession().getAttribute("AGL_OBJ");
		logger.debug("if BO Obj null?" + (boObj == null));
		if (boObj != null) {
			IAgileSession session = boObj.getAgileSessionFromRequest(request);
			switch (boObj.getAglObjType()) {
			case (IItem.OBJECT_TYPE):
				dataObj = (IItem) session.getObject(IItem.OBJECT_TYPE, aglObjName);
				break;
			case (IChange.OBJECT_TYPE):
				dataObj = (IChange) session.getObject(IChange.OBJECT_TYPE, aglObjName);
				break;
			case (IManufacturerPart.OBJECT_TYPE):
				Map<Integer,String> paramMap = new HashMap<>();
				paramMap.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_NAME, aglObjName.split("~")[0]);
				paramMap.put(ManufacturerPartConstants.ATT_GENERAL_INFO_MANUFACTURER_PART_NUMBER, aglObjName.split("~")[1]);
				dataObj = (IManufacturerPart) session.getObject(IManufacturerPart.OBJECT_TYPE, paramMap);
				break;
			case (IUser.OBJECT_TYPE):
				dataObj = (IUser) session.getObject(IUser.OBJECT_TYPE, aglObjName);
				break;
			case (IManufacturer.OBJECT_TYPE):
				dataObj = (IManufacturer) session.getObject(IManufacturer.OBJECT_TYPE, aglObjName);
				break;
			default:
				logger.info(boObj.getAglobjName() + " is not supported Agile Types!!!");
				break;
			}
		}
		return dataObj;
	}

	public void wmFile(String inFile, String outDir, Map<String, String> paramMap) {
		List<String> commands = new ArrayList<>();
		logger.info("------WM Start-----");
		if (Paths.get(prop.getProperty("WM_UTIL_PATH")).toFile().exists()) {
			commands.add(prop.getProperty("WM_UTIL_PATH"));
			commands.add(inFile);
			commands.add(outDir);
			commands.add(prop.getProperty("WM_IMG_FILE_PATH"));
			paramMap.forEach((key, value) -> commands.add(key + "#" + value.replace("#", "")));

			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.redirectErrorStream(true);
			Process process;
			try {
				process = pb.start();
				logger.info("WM Util App Output: ");
				logger.info(output(process, pb));
				process.waitFor();
			} catch (IOException | InterruptedException e) {
				logger.error(e.getMessage(), e);
				Thread.currentThread().interrupt();
			}
			logger.info("------WM complete-----");
		} else {
			logger.info("WM util doesn't exist, skipping Watermarking....");
		}
	}

	public String output(Process process, ProcessBuilder pb) throws IOException {
		String sOut = "";
		if (pb != null) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				sOut = br.lines().collect(Collectors.joining(System.lineSeparator()));
			}
		}
		return sOut;
	}
}