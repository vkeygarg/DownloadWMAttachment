<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="com.x.agile.px.bo.AttachmentBO"%>
<%@page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Get Attachments with Watermark</title>

<link rel="stylesheet" href="resource/css/main.css" />
<%
	AttachmentBO attBOObj = new AttachmentBO();
	request.getSession().setAttribute("AGL_OBJ", attBOObj);
	List<Map<String, String>> attList = attBOObj.getAttachmentFiles(request);
%>
</head>
<body>
	<div class="panel">
		<h1><%=attBOObj.getAglobjName() %>: Attachment Files
		</h1>
		<table class="bordered_table">
			<thead>
				<tr align="center">
					<th>File Name</th>
					<th>File Description</th>
					<th>File Type</th>
					<th>File Size</th>
				</tr>
			</thead>
			<tbody>
				<%
					if (attList != null && attList.size() > 0) {
						for (int i = 0; i < attList.size(); i++) {
				%>
				<tr>
					<td align="center"><span id="fileName"><a
							id="downloadLink" class="hyperLink"
							href="<%=request.getContextPath()%>/downloadServlet?objname=<%=attList.get(i).get("objname")%>&filename=<%=attList.get(i).get("filename")%>">
								<%=attList.get(i).get("filename")%></a></span></td>
					<td align="center"><span id="fileDescription"><%=attList.get(i).get("filedescription")%></span></td>
					<td align="center"><span id="fileType"><%=attList.get(i).get("filetype")%></span></td>
					<td align="center"><span id="fileSize"><%=attList.get(i).get("filesize")%></span></td>
				</tr>
				<%
					}
					} else {
				%>
				<tr>
					<td colspan="4" align="center"><span id="noFiles">No
							Attachments available.....!</span></td>
				</tr>
				<%
					}
				%>
			</tbody>
		</table>
	</div>
</body>
</html>