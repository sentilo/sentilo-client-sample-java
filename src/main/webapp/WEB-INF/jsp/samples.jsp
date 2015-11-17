<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>

<html>

	<head>
		
	</head>

	<body>
		
		<h3>Observations:</h3>
		<p>${observations}</p>

		<br />

		<c:if test="${not empty successMsg}">
			<h3>Success:</h3>
			<p>${successMsg}</p>
		</c:if>

		<c:if test="${not empty errorMsg}">
			<h3>Error:</h3>
			<pre>${errorMsg}</pre>
		</c:if>
		
		<br />
		
		<button onclick="location.reload();">Send observations</button>
		
	</body>

</html>