<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<title>huzon.tv</title>
<head>
	<link rel="stylesheet" type="text/css" href="css/style.css">
	<link rel="icon" type="image/png" href="images/huzon_logo_16x16.png" />
	<script src="js/jquery-1.8.2.js"></script>
	<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
</head>

<body class="teaser-page">
<!--  <%=request.getProtocol() %> -->
<!--  <%=request.getServerPort() %> -->
<%
/*
	if(!request.getServerName().equals("chrome.crasher.com"))
		response.sendRedirect("http://chrome.crasher.com/");
*/
%> 
	<div class="bg"></div>
	<div class="outer">
		<div class="middle">
			<div class="inner">
				<a href="#" id="main_link" style="font-size:30px;color:white"><img id="tv_normal" src="images/huzon_retrotv_withglow_441x392.png" style="width:441px;height:392px;">
				<img id="tv_qmark" src="images/huzon_retrotv_withglowqmark_441x392.png" style="width:441px;height:392px;display:none"></a>
			</div>
		</div>
	</div>
	<footer>
		<p>huzon.tv &copy; 2013 Adkitech, LLC</p>
		<ul>
			<li><a href="whatis.html">About</a></li>
			<li><a href="http://twitter.com/huzontv">Twitter</a></li>
			<li><a href="https://www.facebook.com/pages/huzontv/531229596923718">Facebook</a></li>
			<li><a href="contact.html">Contact</a></li>
		</ul>
	</footer>
	<script>
	$("#main_link").mouseover( function() {
		$("#tv_normal").hide();
		$("#tv_qmark").show();
		return false;
	});
	$("#main_link").mouseout( function() {
		$("#tv_normal").show();
		$("#tv_qmark").hide();
		return false;
	});
	$("#main_link").click( function() {
		window.location = "whatis.html";
		return false;
	});
	</script>
	<!-- 
<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-36811780-2']);
  _gaq.push(['_setDomainName', 'crasher.com']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>
 -->
</body>
</html>