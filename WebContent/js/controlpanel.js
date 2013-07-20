var endpoint = "https://www.huzon.tv/endpoint";
//var endpoint = "https://localhost:8443/huzontv/endpoint";

var docCookies = {
		  getItem: function (sKey) {
		    if (!sKey || !this.hasItem(sKey)) { return null; }
		    return unescape(document.cookie.replace(new RegExp("(?:^|.*;\\s*)" + escape(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=\\s*((?:[^;](?!;))*[^;]?).*"), "$1"));
		  },
		  setItem: function (sKey, sValue, vEnd, sPath, sDomain, bSecure) {
		    if (!sKey || /^(?:expires|max\-age|path|domain|secure)$/i.test(sKey)) { return; }
		    var sExpires = "";
		    if (vEnd) {
		      switch (vEnd.constructor) {
		        case Number:
		          sExpires = vEnd === Infinity ? "; expires=Tue, 19 Jan 2038 03:14:07 GMT" : "; max-age=" + vEnd;
		          break;
		        case String:
		          sExpires = "; expires=" + vEnd;
		          break;
		        case Date:
		          sExpires = "; expires=" + vEnd.toGMTString();
		          break;
		      }
		    }
		    document.cookie = escape(sKey) + "=" + escape(sValue) + sExpires + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "") + (bSecure ? "; secure" : "");
		  },
		  removeItem: function (sKey, sPath) {
		    if (!sKey || !this.hasItem(sKey)) { return; }
		    document.cookie = escape(sKey) + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT" + (sPath ? "; path=" + sPath : "");
		  },
		  hasItem: function (sKey) {
		    return (new RegExp("(?:^|;\\s*)" + escape(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=")).test(document.cookie);
		  }
		};

var devel = true;

//document.addEventListener('DOMContentLoaded', function () {
$(window).load(function () {	
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	var station = ""; 
	if(location.protocol !== "https:")
	{
		$("#message_div").html("<span style=\"font-size:18;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.huzon.tv/controlpanel.html\">https://www.huzon.tv/controlpanel.html</a> instead.</span>");
	}
	else if(twitter_handle === null)
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">Please log in with your Twitter account.</span>");
		$("#main_div").html("<a href=\"#\" id=\"sign_in_with_twitter_link\">Sign in with Twitter</a>");
		$("#sign_in_with_twitter_link").click(
				function (event) {
					var oauth_token = null;
					$("#message_div").html("<span style=\"font-size:16;color:blue\">Twitter authentication started. Please wait. </span>");
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "startTwitterAuthentication"
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        	{
				        		$("#message_div").html("<span style=\"font-size:16;color:red\">startTwitterAuthentication error: " + data.message + "</span>");
				        	}
				        	else
				        	{
				        		oauth_token = data.oauth_token;
				        		window.location.href = "https://api.twitter.com/oauth/authenticate?oauth_token=" + oauth_token;
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("<span style=\"font-size:16;color:red\">startTwitterAuthentication ajax error</span>");
				            console.log(textStatus, errorThrown);
				        }
					});
				return false;
				}
			);	
	}	
	else if(twitter_handle !== null && twitter_access_token !== null)
	{
		$.ajax({
			type: 'GET',
			url: endpoint,
			data: {
	            method: "getSelf",
	            twitter_handle: twitter_handle,
	            twitter_access_token: twitter_access_token
			},
	        dataType: 'json',
	        async: true,
	        success: function (data, status) {
	        	if (data.response_status == "error")
	        	{
	        		$("#message_div").html("<span style=\"font-size:16;color:red\">getSelf error: " + data.message + "</span>");
	        		if(data.error_code && data.error_code == "07734") // the twitter cookie credentials were invalid. Delete them and reload page (start over).
	        		{
	        			docCookies.removeItem("twitter_access_token");
	        			docCookies.removeItem("twitter_handle");
	        			window.location.href = "https://www.huzon.tv/registration.html";
	        		}
	        	}
	        	else // getSelf was successful, meaning twitter_handle and twitter_access_token were OK
	        	{
	        		
	        		if(data.user_jo.stations_as_admin_ja.length <= 0)
	        		{
	        			$("#message_div").html("Sorry, you do not have admin privileges.");
	        		}
	        		else
	        		{	
	        			if(data.user_jo.stations_as_admin_ja.length > 1)
		        		{
		        			$("#message_div").html("Multiple stations as admin. Using administering_station cookie.");
		        			station = docCookies.getItem("administering_station");
		        		}	
	        			else
	        				station = data.user_jo.stations_as_admin_ja[0];
	        			
	        			getStationInformation(twitter_handle, twitter_access_token, station);
    	        		
	        			$("#graph_div").html("getSelf successful, this is the graph_div");
    	        		$("#rankings_div").html("getSelf successful, this is the rankings_div");
	        			
    	        		getActiveReporterDesignations(twitter_handle, twitter_access_token, station);
	        			
	        			$("#administrators_div").html("getSelf successful, this is the administrators_div");
    	        		$("#alerts_div").html("getSelf successful, this is the alerts_div");
	        		}
	        	}
	        },
	        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        	$("#message_div").html("<span style=\"font-size:16;color:red\">getSelf ajax error</span>");
	            console.log(textStatus, errorThrown);
	        }
		});
	}
});

function getStationInformation(twitter_handle, twitter_access_token, station)
{
	var general_string = "";
	$("#general_div").html("Loading general station information <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
	        method: "getStation",
	        station: station,
	        twitter_handle: twitter_handle,
	        twitter_access_token: twitter_access_token
		},
	    dataType: 'json',
	    async: true,
	    success: function (data, status) {
	    	if (data.response_status == "error")
	    		general_string = general_string + "<div style=\"font-size:16;color:red\">getStationInformation error: " + data.message + "</div>";
	    	else
	    	{
	    		general_string = JSON.stringify(data);
	    		$("#general_div").html(general_string);
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}

function getActiveReporterDesignations(twitter_handle, twitter_access_token, station)
{
	var reporters_string = "";
	$("#reporters_div").html("Loading reporter information <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
	        method: "getActiveReporterDesignations",
	        station: station,
	        twitter_handle: twitter_handle,
	        twitter_access_token: twitter_access_token
		},
	    dataType: 'json',
	    async: true,
	    success: function (data, status) {
	    	if (data.response_status == "error")
	    		reporters_string = reporters_string + "<div style=\"font-size:16;color:red\">getActiveReporterDesignations error: " + data.message + "</div>";
	    	else
	    	{
	    		var reporters_ja = data.reporters_ja;
				reporters_string = reporters_string + "<table style=\"width:100%\">";
				reporters_string = reporters_string + "	<tr>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			h.tv designation";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Display name";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Roles";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW handle";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW active";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW linked";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW CD (hrs)";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW followers";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Tweets 30 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB linked";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB page linked";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB active";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB cooldown (hrs)";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB likes";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB 30 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "	</tr>";
				var cooldown_in_hours = 0;
				for(var x=0; x < reporters_ja.length; x++)
				{
					var user = getUser(reporters_ja[x]);
					reporters_string = reporters_string + "	<tr>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_designation_td\">" + reporters_ja[x] + "</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_display_name_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_roles_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_handle_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_active_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_cooldown_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_followers_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_alert_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_page_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_active_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_cooldown_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_likes_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_facebook_alert_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "	</tr>";
				}
				reporters_string = reporters_string + "</table>";
				$("#reporters_div").html(reporters_string);
				
				for(var x=0; x < reporters_ja.length; x++)
				{
					getUser(reporters_ja[x]);
					//getTwitterProfile(reporters_ja[x]);
					//getFacebookProfile(reporters_ja[x]);
					//getFacebookPage(reporters_ja[x]);
				}
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}

function getUser(designation)
{
	/*
	 * String return_tokens_param = request.getParameter("return_tokens");
									String return_tw_profile_param = request.getParameter("return_tw_profile");
									String return_fb_profile_param = request.getParameter("return_fb_profile");
									String return_fb_page_param = request.getParameter("return_fb_page");
									String return_alerts_param = request.getParameter("return_alerts");
	 */
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getUser",
            designation: designation,
            return_tokens: true,
            return_tw_profile: true,
            return_fb_profile: true, 
            return_fb_page: true, 
            return_alerts: true,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	
        	if (data.response_status === "error")
        	{
        		$("#" + designation + "_display_name_td").html("error");
        	}
        	else if(data.response_status === "success")
        	{
        		$("#" + designation + "_display_name_td").html(data.user_jo.display_name);
        		
        		var rolestring = "";
        		if(data.user_jo.anchor === true)
        			rolestring = rolestring + "			A";
				if(data.user_jo.weather === true)
					rolestring = rolestring + "			W";
				if(data.user_jo.sports === true)
					rolestring = rolestring + "			S";
				if(data.user_jo.reporter === true)
					rolestring = rolestring + "			R";
				$("#" + designation + "_roles_td").html(rolestring);
        		
				$("#" + designation + "_twitter_handle_td").html(data.user_jo.twitter_handle);
        		
				if(data.user_jo.twitter_active)
        			$("#" + designation + "_twitter_active_td").html("yes");
        		else
        			$("#" + designation + "_twitter_active_td").html("no");
        		
        		var twitter_linked = "";
        		if(typeof data.user_jo.twitter_jo === undefined || data.user_jo.twitter_jo == null || (data.user_jo.twitter_jo.response_status != null && data.user_jo.twitter_jo.response_status === "error"))
        			twitter_linked = "<span style=\"color:red\">no</span>";
				else
					twitter_linked = "<span style=\"color:green\">yes</span>";
        		$("#" + designation + "_twitter_linked_td").html(twitter_linked);
        		
        		var twitter_followers = "";
        		if(typeof data.user_jo.twitter_jo === undefined || data.user_jo.twitter_jo == null || (data.user_jo.twitter_jo.response_status != null && data.user_jo.twitter_jo.response_status === "error"))
        			twitter_followers = "<span style=\"color:red\">---</span>";
				else
					twitter_followers = "<span style=\"color:green\">" + data.user_jo.twitter_jo.followers_count + "</span>";
        		$("#" + designation + "_twitter_followers_td").html(twitter_followers);
        		
        		$("#" + designation + "_twitter_cooldown_td").html(Math.round(data.user_jo.twitter_cooldown/360)/10);
        		
        		$("#" + designation + "_twitter_alert_count_td").html(data.user_jo.twitter_alert_history_ja.length);
        		
        		if(data.user_jo.facebook_active)
        			$("#" + designation + "_facebook_active_td").html("yes");
        		else
        			$("#" + designation + "_facebook_active_td").html("no");
        		$("#" + designation + "_facebook_cooldown_td").html(Math.round(data.user_jo.facebook_cooldown/360)/10);
        		
        		var facebook_linked = "";
        		if(typeof data.user_jo.facebook_jo === undefined || data.user_jo.facebook_jo == null || data.user_jo.facebook_jo.error != null)
        			facebook_linked = "<span style=\"color:red\">no</span>";
				else
					facebook_linked = "<span style=\"color:green\">yes</span>";
        		$("#" + designation + "_facebook_linked_td").html(facebook_linked);
        		
        		var facebook_page_linked = "";
        		if(typeof data.user_jo.facebook_jo === undefined || data.user_jo.facebook_jo == null || data.user_jo.facebook_jo.error != null)
        			facebook_page_linked = "<span style=\"color:red\">no</span>";
				else
					facebook_page_linked = "<span style=\"color:green\">yes</span>";
        		$("#" + designation + "_facebook_page_linked_td").html(facebook_page_linked);
        	
        		var facebook_likes = "";
        		if(typeof data.user_jo.facebook_page_jo === undefined || data.user_jo.facebook_page_jo == null || typeof data.user_jo.facebook_page_jo.likes === undefined || data.user_jo.facebook_page_jo.likes == null)
        			facebook_likes = "<span style=\"color:red\">---</span>";
				else
					facebook_likes = "<span style=\"color:green\">" + data.user_jo.facebook_page_jo.likes + "</span>";
        		$("#" + designation + "_facebook_likes_td").html(facebook_likes);
        		
        		$("#" + designation + "_facebook_alert_count_td").html(data.user_jo.facebook_alert_history_ja.length);
        		
        	}
        	else
        	{
        		//alert("success/none verifying twitter creds for " + designation);
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#" + designation + "_twitter_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
            console.log(textStatus, errorThrown);
        }
	});
}

/*
function verifyTwitterCredentials(designation)
{

	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "verifyTwitterCredentials",
            designation: designation,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	
        	if (data.response_status === "error")
        	{
        		//alert("success/error verifying twitter creds for " + designation);
        		$("#" + designation + "_twitter_valid_td").html(data.message);
        	}
        	else if(data.response_status === "success")
        	{
        		//alert("success/success verifying twitter creds for " + designation);
        		if(data.valid === true)
        			$("#" + designation + "_twitter_valid_td").html("<span style=\"color:green\">VALID</span>");
        		else
        			$("#" + designation + "_twitter_valid_td").html("<span style=\"color:red\">NOT VALID</span>");
        	}
        	else
        	{
        		//alert("success/none verifying twitter creds for " + designation);
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#" + designation + "_twitter_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
            console.log(textStatus, errorThrown);
        }
	});
}

function verifyTopLevelFBCredentials(designation)
{
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "verifyTopLevelFBCredentials",
            designation: designation,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#" + designation + "_fb_valid_td").html(data.message);
        	else if(data.response_status == "success")
        	{
        		if(data.valid == true)
        			$("#" + designation + "_fb_valid_td").html("<span style=\"color:green\">VALID</span>");
        		else
        			$("#" + designation + "_fb_valid_td").html("<span style=\"color:red\">NOT VALID</span>");
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#" + designation + "_fb_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
            console.log(textStatus, errorThrown);
        }
	});
}

function verifyPageFBCredentials(designation)
{
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "verifyPageFBCredentials",
            designation: designation,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#" + designation + "_fbpage_valid_td").html(data.message);
        	else if(data.response_status == "success")
        	{
        		if(data.valid == true)
        			$("#" + designation + "_fbpage_valid_td").html("<span style=\"color:green\">VALID</span>");
        		else
        			$("#" + designation + "_fbpage_valid_td").html("<span style=\"color:red\">NOT VALID</span>");
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#" + designation + "_fbpage_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
            console.log(textStatus, errorThrown);
        }
	});
}*/

/*
 * reporters_string = reporters_string + "	<tr>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_designation_td\">";
					reporters_string = reporters_string + "			" + reporters_ja[x].designation;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_display_name_td\">";
					reporters_string = reporters_string + "			" + reporters_ja[x].display_name;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_roles_td\">";
					if(reporters_ja[x].anchor === true)
						reporters_string = reporters_string + "			A";
					if(reporters_ja[x].weather === true)
						reporters_string = reporters_string + "			W";
					if(reporters_ja[x].sports === true)
						reporters_string = reporters_string + "			S";
					if(reporters_ja[x].reporter === true)
						reporters_string = reporters_string + "			R";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_twitter_handle_td\">";
					reporters_string = reporters_string + "			" + reporters_ja[x].twitter_handle;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_twitter_active_td\">";
					if(reporters_ja[x].twitter_active)
						reporters_string = reporters_string + "			<span style=\"color:green\">yes</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:red\">no</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_twitter_linked_td\">";
					if(typeof reporters_ja[x].twitter_jo === undefined || reporters_ja[x].twitter_jo == null || (reporters_ja[x].twitter_jo.response_status != null && reporters_ja[x].twitter_jo.response_status === "error"))
						reporters_string = reporters_string + "			<span style=\"color:red\">not linked</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">linked</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_twitter_cooldown_td\">";
					cooldown_in_hours = Math.round(reporters_ja[x].twitter_cooldown / 360)/10; // displays as nearest tenth
					reporters_string = reporters_string + "			" + cooldown_in_hours;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_twitter_twitter_followers_td\">";
					if(typeof reporters_ja[x].twitter_jo === undefined || reporters_ja[x].twitter_jo == null || 
							typeof reporters_ja[x].twitter_jo_followers_count === undefined || reporters_ja[x].twitter_jo.followers_count == null)
						reporters_string = reporters_string + "			<span style=\"color:red\">---</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">" + reporters_ja[x].twitter_jo.followers_count + "</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_twitter_alert_count_td\">";
					if(reporters_ja[x].twitter_alert_history_ja)
						reporters_string = reporters_string + "		  " + reporters_ja[x].twitter_alert_history_ja.length;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_facebook_linked_td\">";
					if(typeof reporters_ja[x].facebook_jo === undefined || reporters_ja[x].facebook_jo == null || reporters_ja[x].facebook_jo.error != null) // || 
							//typeof reporters_ja[x].facebook_jo_followers_count === undefined || reporters_ja[x].facebook_jo.followers_count == null)
						reporters_string = reporters_string + "			<span style=\"color:red\">not linked</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">linked</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_facebook_active_td\">";
					if(reporters_ja[x].facebook_active)
						reporters_string = reporters_string + "			<span style=\"color:green\">yes</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:red\">no</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_facebook_page_linked_td\">";
					if(typeof reporters_ja[x].facebook_page_jo === undefined || reporters_ja[x].facebook_page_jo == null || reporters_ja[x].facebook_page_jo.error != null) 
						reporters_string = reporters_string + "			<span style=\"color:red\">not linked</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">linked</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_facebook_cooldown_td\">";
					cooldown_in_hours = Math.round(reporters_ja[x].facebook_cooldown / 360)/10; // displays as nearest tenth
					reporters_string = reporters_string + "			" + cooldown_in_hours;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_facebook_likes_td\">";
					if(typeof reporters_ja[x].facebook_page_jo === undefined || reporters_ja[x].facebook_page_jo == null || reporters_ja[x].facebook_page_jo.error != null) 
						reporters_string = reporters_string + "			<span style=\"color:red\">---</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">"+ reporters_ja[x].facebook_page_jo.likes + "</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x].designation + "_facebook_alert_count_td\">";
					if(reporters_ja[x].facebook_alert_history_ja)
						reporters_string = reporters_string + "		  " + reporters_ja[x].facebook_alert_history_ja.length;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "	</tr>";*/
 


