<%@ page language="java" %>

<%!
// authentication stuff - robert
// -----------------------------
// create DocumentBean instance for all JSP requests
digilib.servlet.DocumentBean docBean = new digilib.servlet.DocumentBean();

// initialize DocumentBean instance in JSP init
public void jspInit() {
    try {
        // set servlet init-parameter
        docBean.setConfig(getServletConfig());
    } catch (javax.servlet.ServletException e) {
        System.out.println(e);
    }
}
%>

<%
// get digilib config
digilib.servlet.DigilibConfiguration dlConfig = docBean.getDlConfig();
// parsing the query
digilib.servlet.DigilibRequest dlRequest = new digilib.servlet.DigilibRequest(request);
// add number of pages
dlRequest.setPt(docBean.getNumPages(dlRequest));
%>

<html>
<head>
<title>Digilib configuration page</title>
</head>

<body>
<h1>Local request information</h1>

<table>
  <tr>
    <td>Pt</td><td><b><%= dlRequest.getPt() %></b></td>
    <td><i>total number of pages (generated by servlet)</i></td>
  </tr>
  <tr>
    <td>RequestPath</td><td><b><%= dlRequest.getRequestPath() %></b></td>
    <td><i>url of the page/document</i></td>
  </tr>
  <tr>
    <td>Fn</td><td><b><b><%= dlRequest.getFn() %></b></td><td><i>url of the page/document</i></td>
  </tr>
  <tr>
    <td>FilePath</td><td><b><%= dlRequest.getFilePath() %></b></td><td><i>effective path of the page/document</i></td>
  </tr>
  <tr>
    <td>Pn</td><td><b><%= dlRequest.getPn() %></b></td><td><i>page number</i></td>
  </tr>
  <tr>
    <td>Dw</td><td><b><%= dlRequest.getDw() %></b></td><td><i>width of client in pixels</i></td>
  </tr>
  <tr>
    <td>Dh</td><td><b><%= dlRequest.getDh() %></b></td><td><i>height of client in pixels</i></td>
  </tr>
  <tr>
    <td>Wx</td><td><b><%= dlRequest.getWx() %></b></td><td><i>left edge of image (float from 0 to 1)</i></td>
  </tr>
  <tr>
    <td>Wy</td><td><b><%= dlRequest.getWy() %></b></td><td><i>top edge in image (float from 0 to 1)</i></td>
  </tr>
  <tr>
    <td>Ww</td><td><b><%= dlRequest.getWw() %></b></td><td><i>width of image (float from 0 to 1)</i></td>
  </tr>
  <tr>
    <td>Wh</td><td><b><%= dlRequest.getWh() %></b></td><td><i>height of image (float from 0 to 1)</i></td>
  </tr>
  <tr>
    <td>Ws</td><td><b><%= dlRequest.getWs() %></b></td><td><i>scale factor</i></td>
  </tr>
  <tr>
    <td>Mo</td><td><b><%= dlRequest.getMo() %></b></td><td><i>special options like 'fit' for gifs</i></td>
  </tr>
  <tr>
    <td>Mk</td><td><b><%= dlRequest.getMk() %></b></td><td><i>marks</i></td>
  </tr>
  <tr>
    <td>BaseURL</td><td colspan="2"><b><%= dlRequest.getBaseURL() %></b></td>
  </tr>
  <tr>
    <td></td><td></td><td><i>base URL (from http:// to below /servlet)</i></td>
  </tr>
</table>


<h1>Global servlet configuration</h1>

<table>
  <tr>
    <td>ServletVersion</td><td><b><%= dlConfig.getServletVersion() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>AuthConfPath</td><td><b><%= dlConfig.getAuthConfPath() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>AuthURLPath</td><td><b><%= dlConfig.getAuthURLPath() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>BaseDirs</td><td><b><%= dlConfig.getBaseDirList() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>DebugLevel</td><td><b><%= dlConfig.getDebugLevel() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>DenyImgFileName</td><td><b><%= dlConfig.getDenyImgFileName() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>DLConfPath</td><td><b><%= dlConfig.getDlConfPath() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>DocuImageType</td><td><b><%= dlConfig.getDocuImageType() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>ErrorImgFileName</td><td><b><%= dlConfig.getErrorImgFileName() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>SendFileAllowed</td><td><b><%= dlConfig.isSendFileAllowed() %></b></td>
    <td></td>
  </tr>
  <tr>
    <td>UseAuthentication</td><td><b><%= dlConfig.isUseAuthentication() %></b></td>
    <td></td>
  </tr>
</table>

</body>
</html>
