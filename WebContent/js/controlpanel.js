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
	        method: "getActiveReporters",
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
				reporters_string = reporters_string + "			TW alerts on";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW acct";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW cooldown (hrs)";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			TW followers";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Tweets 30 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Tweets 10 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Tweets 3 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Tweets 24 hr";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			Top-level FB";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB alerts on";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB page";
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
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB 10 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB 3 days";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "			FB 24 hr";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "	</tr>";
				var cooldown_in_hours = 0;
				for(var x=0; x < reporters_ja.length; x++)
				{
					reporters_string = reporters_string + "	<tr>";
					reporters_string = reporters_string + "		<td>";
					reporters_string = reporters_string + "			" + reporters_ja[x].designation;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					reporters_string = reporters_string + "			" + reporters_ja[x].display_name;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(reporters_ja[x].anchor === true)
						reporters_string = reporters_string + "			A";
					if(reporters_ja[x].weather === true)
						reporters_string = reporters_string + "			W";
					if(reporters_ja[x].sports === true)
						reporters_string = reporters_string + "			S";
					if(reporters_ja[x].reporter === true)
						reporters_string = reporters_string + "			R";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					reporters_string = reporters_string + "			" + reporters_ja[x].twitter_handle;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(reporters_ja[x].twitter_active)
						reporters_string = reporters_string + "			<span style=\"color:green\">yes</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:red\">no</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(typeof reporters_ja[x].twitter_jo === undefined || reporters_ja[x].twitter_jo == null || (reporters_ja[x].twitter_jo.response_status != null && reporters_ja[x].twitter_jo.response_status === "error"))
						reporters_string = reporters_string + "			<span style=\"color:red\">not linked</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">linked</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					cooldown_in_hours = Math.round(reporters_ja[x].twitter_cooldown / 360)/10; // displays as nearest tenth
					reporters_string = reporters_string + "			" + cooldown_in_hours;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(typeof reporters_ja[x].twitter_jo === undefined || reporters_ja[x].twitter_jo == null || 
							typeof reporters_ja[x].twitter_jo_followers_count === undefined || reporters_ja[x].twitter_jo.followers_count == null)
						reporters_string = reporters_string + "			<span style=\"color:red\">---</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">" + reporters_ja[x].twitter_jo.followers_count + "</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(reporters_ja[x].twitter_alert_history_ja)
						reporters_string = reporters_string + "		  " + reporters_ja[x].twitter_alert_history_ja.length;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					//reporters_string = reporters_string + "			Tweets 10 days";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					//reporters_string = reporters_string + "			Tweets 3 days";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					//reporters_string = reporters_string + "			Tweets 24 hr";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(typeof reporters_ja[x].facebook_jo === undefined || reporters_ja[x].facebook_jo == null || reporters_ja[x].facebook_jo.error != null) // || 
							//typeof reporters_ja[x].facebook_jo_followers_count === undefined || reporters_ja[x].facebook_jo.followers_count == null)
						reporters_string = reporters_string + "			<span style=\"color:red\">not linked</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">linked</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(reporters_ja[x].facebook_active)
						reporters_string = reporters_string + "			<span style=\"color:green\">yes</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:red\">no</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(typeof reporters_ja[x].facebook_page_jo === undefined || reporters_ja[x].facebook_page_jo == null || reporters_ja[x].facebook_page_jo.error != null) 
						reporters_string = reporters_string + "			<span style=\"color:red\">not linked</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">linked</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					cooldown_in_hours = Math.round(reporters_ja[x].facebook_cooldown / 360)/10; // displays as nearest tenth
					reporters_string = reporters_string + "			" + cooldown_in_hours;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(typeof reporters_ja[x].facebook_page_jo === undefined || reporters_ja[x].facebook_page_jo == null || reporters_ja[x].facebook_page_jo.error != null) 
						reporters_string = reporters_string + "			<span style=\"color:red\">---</span>";
					else
						reporters_string = reporters_string + "			<span style=\"color:green\">"+ reporters_ja[x].facebook_page_jo.likes + "</span>";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					if(reporters_ja[x].facebook_alert_history_ja)
						reporters_string = reporters_string + "		  " + reporters_ja[x].facebook_alert_history_ja.length;
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					//reporters_string = reporters_string + "			FB 10 days";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					//reporters_string = reporters_string + "			FB 3 days";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "		<td>";
					//reporters_string = reporters_string + "			FB 24 hr";
					reporters_string = reporters_string + "		</td>";
					reporters_string = reporters_string + "	</tr>";
				}
				reporters_string = reporters_string + "</table>";
				$("#reporters_div").html(reporters_string);
				
				//for(var x=0; x < reporters_ja.length; x++)
				//{
					//verifyTwitterCredentials(reporters_ja[x].designation);
					//verifyTopLevelFBCredentials(reporters_ja[x].designation);
				//	verifyPageFBCredentials(reporters_ja[x].designation);
				//}
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}


