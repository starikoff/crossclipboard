<%@page import="ru.ra.links.LinkInfo"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collection"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.util.Collections"%>
<%@page session="false"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
  pageEncoding="UTF-8"%><!DOCTYPE html>
<html lang="en">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>CrossClipboard: Content</title>
<style>
.error {
  color: #990000;
  text-align: center;
  margin-bottom: 20px;
}
kbd {
  border: 1px solid #bbb; 
  font-size: 120%; 
  padding: 5px; 
  border-radius: 5px;
}
.btn {
  padding: 10px 30px;
  background-color: #060;
  color: white;
  border: 0px;
  border-radius: 5px;
  margin-top: 8px;
}
.btn:hover {
  background-color: #090;
}
.favicon {
  width: 16px;
  height: 16px;
}
a {
  color: #0b79e5;
  text-decoration: none;
}
.delete {
  color: #55aaff;
  font-style: italic;
  margin-left: 0.5em;
}
.item {
  text-align: left;
  margin-bottom: 8px;
  background-color: #e8e8e8;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid #ddd;
}
.wrapped {
  overflow-wrap: break-word;
  word-wrap: break-word;
  word-break: break-word;
}
</style>
<script type="text/javascript">
  function keyPressHandler(event) {
    if ((event.keyCode === 10 || event.keyCode === 13) && event.ctrlKey) {
      var form = document.getElementById("form");
      if (form) { 
        form.submit();
      }
      event.preventDefault();
    }
  }
</script>
</head>
<body style="margin: 0px; font-family: Arial;">
  <div style="height: 10%; text-align: right; margin: 8px 20px">
    <a href="?logout">logout</a>&nbsp;<%=request.getAttribute("login")%>
  </div>
  <div style="max-width: 600px; margin: 10px auto; text-align: center; width: 90%">
    <%
      Collection<String> errors = (Collection<String>) request
          .getAttribute("errors");
      if (errors == null) {
        errors = Collections.emptyList();
      }
      for (String error : errors) {
    %>
    <div class="error"><%=error%></div>
    <%
      }
    %>
    <div style="width: 100%; margin-bottom: 20px">
      <%
        String content = (String) request.getAttribute("content");
        if (content == null) {
          content = "";
        }
        Collection<LinkInfo> urls = (Collection<LinkInfo>) request.getAttribute("notes"); 
        if (urls != null) {
          for (LinkInfo linkInfo : urls) {
            %>
      <div class="item">
	  <div style="float:left">
        <% if (linkInfo.faviconUrl != null) { %>
        <div style="float:left"><img src="<%= linkInfo.faviconUrl %>" class="favicon">&nbsp;</div>
        <% } %>
        <% if (linkInfo.isUrl) { %>
        <a class="wrapped" href="<%= linkInfo.content %>"><%= linkInfo.title %></a>
        <% } else { %>
        <span class="wrapped"><%= linkInfo.content %></span>
        <% } %></div>
        <div style="float:right"><a href="?delete&id=<%= linkInfo.id %>" class="delete">delete</a></div>
        <% if (linkInfo.server != null) { %>
        <div style="float:right;color:#aaa;margin-left:0.2em"><%= linkInfo.server %></div>
        <% } %>
        <div style="clear:both"></div>
      </div>
      <% }
      } %>
    </div>
    <form action="" method="post" id="form">
      <input type="hidden" name="action" value="content" />
      <div>
        <textarea name="content" id="contentTextArea" rows="12" 
          onkeypress="keyPressHandler(event);" 
          autofocus="" style="width: 90%"><%=content%></textarea>
      </div>
      <div>
        <input type="submit" value="post" class="btn">
      </div>
    </form>
    <p style="color: #bbb; line-height: 1.5em">On desktop, you can use <kbd>Ctrl</kbd> + <kbd>Enter</kbd> to send the form.</p>
  </div>
</body>
</html>
