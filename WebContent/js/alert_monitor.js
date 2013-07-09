var endpoint = "https://www.huzon.tv/endpoint";
//var endpoint = "https://localhost:8443/huzontv/endpoint";

var reA = /[^a-zA-Z]/g;
var reN = /[^0-9]/g;
function sortAlphaNum(a,b) {
    var aA = a.replace(reA, "");
    var bA = b.replace(reA, "");
    if(aA === bA) {
        var aN = parseInt(a.replace(reN, ""), 10);
        var bN = parseInt(b.replace(reN, ""), 10);
        return aN === bN ? 0 : aN > bN ? 1 : -1;
    } else {
        return aA > bA ? 1 : -1;
    }
}

function getParameterByName(name) {
    name = name.replace(/[\[]/, "\\\[")
        .replace(/[\]]/, "\\\]");
    var regexS = "[\\?&]" + name + "=([^&#]*)";
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.search);
    if (typeof results === "undefined" || results == null || results === "") return "";
    else return decodeURIComponent(results[1].replace(/\+/g, " "));
}


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

document.addEventListener('DOMContentLoaded', function () {
	
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	var station = docCookies.getItem("administering_station");
	if(location.protocol !== "https:")
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.huzon.tv/alert_monitor.html\">https://www.huzon.tv/alert_monitor.html</a> instead.</span>");
	}
	else if(twitter_handle === null)
	{
		var oauth_verifier = getParameterByName("oauth_verifier");
		if(typeof oauth_verifier !== undefined && oauth_verifier != null && oauth_verifier !== "") // no twitter_handle cookie, but oauth_verifier is present, signifying response from twitter after user has granted access
		{
			$.ajax({
				type: 'GET',
				url: endpoint,
				data: {
		            method: "getTwitterAccessTokenFromAuthorizationCode",
		            oauth_verifier: oauth_verifier,
		            oauth_token: getParameterByName("oauth_token")
				},
		        dataType: 'json',
		        async: false,
		        success: function (data, status) {
		        	if (data.response_status == "error")
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:red\">gTATFAC Error: " + data.message + " </span>");
		        	}
		        	else
		        	{
		        		//$("#message_div").html("<span style=\"font-size:16;color:blue\">You have successfully linked your Twitter account (" +  data.twitter_handle + ") to huzon.tv!<br><br>Please wait. Reloading page...</span>");
		        		docCookies.setItem("twitter_handle", data.twitter_handle, 31536e3);
		        		docCookies.setItem("twitter_access_token", data.twitter_access_token, 31536e3);
		        		window.location.href = "https://www.huzon.tv/alert_monitor.html";
		        	}
		        }
		        ,
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#main_td").html("ajax error");
		            console.log(textStatus, errorThrown);
		        }
			});
		}	
		else // no twitter_handle cookie, no oauth_verifier from twitter (signifying that the user is coming back from twitter with credentials)
		{	
			$("#message_div").html("<span style=\"font-size:16;color:red\">Please log in with your Twitter account.</span>");
			$("#main_div").html("<a href=\"#\" id=\"sign_in_with_twitter_link\">Sign in with Twitter</a>");
			
			$("#sign_in_with_twitter_link").click(
				function (event) {
					var oauth_token = null;
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "startTwitterAuthentication"
						},
				        dataType: 'json',
				        async: false,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        	{
				        		$("#message_div").html("<span style=\"font-size:16;color:red\">sTA Error: " + data.message + "</span>");
				        	}
				        	else
				        	{
				        		$("#message_div").html("<span style=\"font-size:16;color:blue\">Twitter authentication started...</span>");
				        		//$("#main_div").html("");
				        		oauth_token = data.oauth_token;
				        		window.location.href = "https://api.twitter.com/oauth/authenticate?oauth_token=" + oauth_token;
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
				            console.log(textStatus, errorThrown);
				        }
					});
				return false;
				}
			);
		}
	}	
	else if(twitter_handle !== null && twitter_access_token !== null)
	{
		// this is an administrator call to get the most recent alerts. Will not work for non global admins.
		$.ajax({
			type: 'GET',
			url: endpoint,
			data: {
	            method: "getMostRecentAlerts",
	            station: station,
	            twitter_handle: twitter_handle,
	            twitter_access_token: twitter_access_token
			},
	        dataType: 'json',
	        async: false,
	        success: function (data, status) {
	        	if (data.response_status == "error")
	        	{
	        		$("#message_div").html("<span style=\"font-size:16;color:red\">gS Error: " + data.message + "</span>");
	        	}
	        	else // getMostRecentAlerts was successful, meaning twitter_handle and twitter_access_token were OK
	        	{
	        		var alerts_ja = data.alerts_ja;
	        		var mds = "";
	        		mds = mds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:10px\">";
	        		/*mds = mds + "	<tr>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-weight:bold;font-size:20px\">";
	        		mds = mds + "			Alert";
	        		mds = mds + "		</td>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-weight:bold;font-size:20px\">";
	        		mds = mds + "			Time<br>Designation<br>Station<br>Type<br>Created by";
	        		mds = mds + "		</td>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-weight:bold;font-size:20px\">";
	        		mds = mds + "			Delete?";
	        		mds = mds + "		</td>";
	        		mds = mds + "	</tr>";*/
	        		for(var x = 0; x < alerts_ja.length; x++)
	        		{	
	        			mds = mds + "	<tr>";
		        		mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-weight:bold;font-size:20px\">";
		        		mds = mds + "			<img src=\"" + alerts_ja[x].image_url + "\" style=\"width:400px;height:225px\">";
		        		mds = mds + "		</td>";
		        		mds = mds + "		<td style=\"vertical-align:middle;text-align:center;font-weight:bold;font-size:20px\">";
		        		mds = mds + "			" + alerts_ja[x].creation_timestamp + "<br>" +  alerts_ja[x].designation + "<br>" + alerts_ja[x].station + "<br>" + alerts_ja[x].social_type + "<br>" + alerts_ja[x].created_by;;
		        		mds = mds + "		</td>";
		        		mds = mds + "		<td style=\"vertical-align:middle;text-align:center;font-weight:bold;font-size:20px\">";
		        		mds = mds + "			<a href=\"#\" id=\"delete_link_" + x + "\">DELETE</a>";
		        		mds = mds + "		</td>";
		        		mds = mds + "	</tr>";
	        		}
	        		mds = mds + "</table>";
	        		$("#main_div").html(mds);
	        		for(var x = 0; x < alerts_ja.length; x++)
	        		{	
	        			$("#delete_link_" + x).click({value1: x},
	        					function (event) {
		        					//alert("deleting index=" + event.data.value1);
		        					$.ajax({
		        						type: 'GET',
		        						url: endpoint,
		        						data: {
		        				            method: "deleteSocialItem",
		        				            id: alerts_ja[event.data.value1].id, 
		        				            twitter_handle: twitter_handle,
		        				            twitter_access_token: twitter_access_token
		        						},
		        				        dataType: 'json',
		        				        async: false,
		        				        success: function (data, status) {
		        				        	if (data.response_status == "error")
		        				        		$("#message_div").html("error message=" + data.message);
		        				        	else
		        				        	{
		        				        		if(data.social_response === true)
		        				        			window.location.reload();
		        				        	}
		        				        }
		        				        ,
		        				        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        				        	$("#message_div").html("ajax error");
		        				            console.log(textStatus, errorThrown);
		        				        }
		        					});
		        					return false;
	        					}
	        				);
	        		}
	        		
	        	}
	        }
	        ,
	        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
	            console.log(textStatus, errorThrown);
	        }
		});
	}
	else
	{
		alert("incorrect state");
	}	
	
	
});
	
/*
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">NO</a><br>";
mds = mds + "3. Facebook <i>reporter page</i> linked? ";
var fb_subaccounts_ja = null;
if(data.user_jo.facebook_page_id && data.user_jo.facebook_access_token != null && data.user_jo.facebook_access_token != "" 
&& data.user_jo.facebook_page_access_token != null && data.user_jo.facebook_page_access_token != "")
{
mds = mds + "			<span style=\"color:blue\">YES -- You're finished. Thanks!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
}
else
{
mds = mds + "			<span style=\"color:red\">NO</span> (select below)";
mds = mds + "		</td>";
mds = mds + "	</tr>";

$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getFacebookSubAccountInfoFromFacebook",
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: false,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#message_div").html("<span style=\"font-size:16;color:red\">Error getting facebook subaccount info. message= " + data.message + "</span>");
        	else
        	{
        		//$("#message_div").html("<span style=\"font-size:16;color:blue\">Brand pages successfully retrieved from Facebook. Select the correct one below.</span>");
        		fb_subaccounts_ja = data.fb_subaccounts_ja;
        		mds = mds + "	<tr>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
        		mds = mds + "			facebook brand pages:";
        		mds = mds + "		</td>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
        		mds = mds + " 			<table>";
        		for(var x=0; x < fb_subaccounts_ja.length; x++)
        		{
        			mds = mds + " 			<tr><td style=\"valign:middle\"><input type=radio id=\"" + fb_subaccounts_ja[x].id + "_radio\"></td><td>" + fb_subaccounts_ja[x].name + "(" + fb_subaccounts_ja[x].id + ")</td></tr>";
        		}
        		mds = mds + "			</table>";
        		mds = mds + "		</td>";
        		mds = mds + "	</tr>";
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
            console.log(textStatus, errorThrown);
        }
	});
}
*/
/*mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-weight:bold;font-size:20px\" colspan=2>";

mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			designation:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			" + data.user_jo.designation;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			stations as reporter:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
for(var x = 0; x < data.user_jo.stations_as_reporter.length; x++)
{
mds = mds + "			" + data.user_jo.stations_as_reporter[x];
}
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			stations as admin:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
for(var x = 0; x < data.user_jo.stations_as_administrator.length; x++)
{
mds = mds + "		" + data.user_jo.stations_as_administrator[x];
}
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			news roles:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\"";
if(data.user_jo.anchor)
mds = mds + "anchor";
if(data.user_jo.reporter)
mds = mds + " reporter";
if(data.user_jo.sports)
mds = mds + " sports";
if(data.user_jo.weather)
mds = mds + " weather";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			studio homogeneity:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "		 	"  + data.user_jo.homogeneity_studio;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			natural homogeneity:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "		 	"  + data.user_jo.homogeneity_natural;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			artificial homogeneity:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "		 	"  + data.user_jo.homogeneity_artificial;
mds = mds + "		</td>";
mds = mds + "	</tr>";



mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			twitter handle:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			"  + data.user_jo.twitter_handle;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			twitter access token:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			<i>hidden</i>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			twitter access token secret:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			<i>hidden</i>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			twitter alert waiting period:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			"  + data.user_jo.twitter_alert_waiting_period;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			twitter delete after:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			"  + data.user_jo.twitter_delete_after;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			twitter last alert:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			"  + data.user_jo.twitter_last_alert;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook uid:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_uid)
mds = mds + data.user_jo.facebook_uid;
else
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB acct not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook access token:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_access_token)
mds = mds + "<i>hidden</i>";
else
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB acct not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook access token expires:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_access_token_expires)
mds = mds + data.user_jo.facebook_access_token_expires;
else
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB acct not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook alert waiting period:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_alert_waiting_period)
mds = mds + data.user_jo.facebook_alert_waiting_period;
else
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB acct not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook delete after:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_delete_after)
mds = mds + data.user_jo.facebook_delete_after;
else
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB acct not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook last alert:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_last_alert != null)
mds = mds + " " + data.user_jo.facebook_last_alert;
else
mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB acct not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";

// if the top-level facebook account is linked but there is no facebook_page_id...
// get the subaccounts directly from facebook so the user can select one of them
var fb_subaccounts_ja = null;
if(data.user_jo.facebook_access_token != null && data.user_jo.facebook_access_token != "" && data.user_jo.facebook_page_id == null)
{
$.ajax({
	type: 'GET',
	url: endpoint,
	data: {
        method: "getFacebookSubAccountInfoFromFacebook",
        twitter_handle: twitter_handle,
        twitter_access_token: twitter_access_token
	},
    dataType: 'json',
    async: false,
    success: function (data, status) {
    	if (data.response_status == "error")
    		$("#message_div").html("<span style=\"font-size:16;color:red\">Error getting facebook subaccount info. message= " + data.message + "</span>");
    	else
    	{
    		$("#message_div").html("<span style=\"font-size:16;color:blue\">Brand pages successfully retrieved from Facebook. Select the correct one below.</span>");
    		fb_subaccounts_ja = data.fb_subaccounts_ja;
    	}
    }
    ,
    error: function (XMLHttpRequest, textStatus, errorThrown) {
    	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
        console.log(textStatus, errorThrown);
    }
});
}
if(fb_subaccounts_ja == null)
{	
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook page id:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_page_id)
	mds = mds + data.user_jo.facebook_page_id;
else
	mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB reporter page not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook page name:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_page_name)
	mds = mds + data.user_jo.facebook_page_name;
else
	mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB reporter page not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook page access token:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
if(data.user_jo.facebook_page_access_token)
	mds = mds + "<i>hidden</i>";
else
	mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">FB reporter page not linked!</span>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
}
else
{
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			facebook brand pages:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + " 			<table>";
for(var x=0; x < fb_subaccounts_ja.length; x++)
{
	mds = mds + " 			<tr><td style=\"valign:middle\"><input type=radio id=\"" + fb_subaccounts_ja[x].id + "_radio\"></td><td>" + fb_subaccounts_ja[x].name + "(" + fb_subaccounts_ja[x].id + ")</td></tr>";
}
mds = mds + "			</table>";
mds = mds + "		</td>";
mds = mds + "	</tr>";
}
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			weekday expected:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			"  + data.user_jo.weekday_expected_begin + "-" + data.user_jo.weekday_expected_end;
mds = mds + "		</td>";
mds = mds + "	</tr>";
mds = mds + "	<tr>";
mds = mds + "		<td style=\"vertical-align:top;text-align:right;font-weight:bold\">";
mds = mds + "			weekend expected:";
mds = mds + "		</td>";
mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
mds = mds + "			"  + data.user_jo.weekend_expected_begin + "-" + data.user_jo.weekend_expected_end;
mds = mds + "		</td>";
mds = mds + "	</tr>";*/

/*
else if(typeof huzon_auth === undefined || huzon_auth === null || huzon_auth === "")
{
	$("#message_div").html("<span style=\"font-size:16;color:red\">Please enter the password you were provided. </span>");
	var mds = "<table><tr><td style=\"vertical-align:middle\">Password: </td><td style=\"vertical-align:middle\"><input type=password id=\"huzon_auth_input\"></td><td style=\"vertical-align:middle\"><input type=button id=\"huzon_auth_go_button\" value=\"go\"></td></tr></table>";
	$("#main_td").html(mds);
	$("#huzon_auth_go_button").click(function () {
			//docCookies.setItem("huzon_auth", $("#huzon_auth_input").val(), 31536e3);
			$.ajax({
				type: 'GET',
				url: endpoint,
				data: {
		            method: "checkPassword",
		            password: $("#huzon_auth_input").val()
				},
		        dataType: 'json',
		        async: false,
		        success: function (data, status) {
		        	if (data.response_status == "error")
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:red\">Incorrect password. Please try again.</span>");
		        	}
		        	else
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:blue\">Correct! Reloading page...</span>");
		        		docCookies.setItem("huzon_auth", $("#huzon_auth_input").val(), 31536e3);
		        		window.location.reload();
		        	}
		        }
		        ,
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#main_td").html("ajax error");
		            console.log(textStatus, errorThrown);
		        }
			});
			return false;
			}
		);
}
else
{
	var oauth_verifier = getParameterByName("oauth_verifier");
	if(oauth_verifier !== "")
	{
		// this is a response from twitter after user has granted access
		var designation = docCookies.getItem("designation");
		$.ajax({
			type: 'GET',
			url: endpoint,
			data: {
	            method: "getTwitterAccessTokenFromAuthorizationCode",
	            oauth_verifier: oauth_verifier,
	            oauth_token: getParameterByName("oauth_token"),
	            designation: designation,
	            huzon_auth: huzon_auth
			},
	        dataType: 'json',
	        async: false,
	        success: function (data, status) {
	        	if (data.response_status == "error")
	        	{
	        		$("#message_div").html("<span style=\"font-size:16;color:red\">Error: It looks like you're trying to link an Twitter account that doesn't match the Twitter handle we're expecting. Are you logged into the right account? Did you click the right link below? Please try again.<br><br>Detailed error message from server: " + data.message + " </span>");
	        	}
	        	else
	        	{
	        		$("#message_div").html("<span style=\"font-size:16;color:blue\">You have successfully linked your Twitter account to huzon.tv! Thanks!</span>");
	        	}
	        }
	        ,
	        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        	$("#main_td").html("ajax error");
	            console.log(textStatus, errorThrown);
	        }
		});
	}	
	
	
	
	var designations = null;
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getDesignationsAndAccounts",
            station: "wkyt",
            include_master: "yes",
            huzon_auth: huzon_auth
		},
        dataType: 'json',
        async: false,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#message_div").html("<span style=\"font-size:16;color:red\">error getting station designations</span>");
        	else
        	{
        		designations = data.designations;
        		//$("#message_div").html("Designations successfully retrieved.");

        		var mds = "";
        		mds = mds + "<div style=\"font-size:16px;font-weight:bold\">";
        		mds = mds + "<table style=\"margin-left:auto;margin-right:20px;border-spacing:10px\">";
        		mds = mds + "	<tr>";
        		mds = mds + "		<td colspan=5 style=\"vertical-align:top;text-align:left\">";
        		mds = mds + "			<span style=\"color:blue;font-weight:bold\">BLUE</span> = account is linked</span><br>";
        		mds = mds + "			<span style=\"color:red;font-weight:bold\">RED</span> = account is not linked</span><br>";
        		mds = mds + "		</td>";
        		mds = mds + "	</tr>";
        		mds = mds + "	<tr>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;\" colspan=5>";
        		mds = mds + "			<hr>";
        		mds = mds + "		</td>";
        		mds = mds + "	</tr>";
        		mds = mds + "	<tr>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Name</td>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Acct type</td>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Twitter</td>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Facebook</td>";
        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">FB brand page</td>";
        		mds = mds + "	</tr>";
        		designations.sort(function(a,b){
        			aa = a.acct_type;
        			bb = b.acct_type;
        			if(aa > bb)
        				return 1;
        			else if(bb > aa)
        				return -1;
        			else
        			{
        				aaa = a.designation;
        				bbb = b.designation;
        				if(aaa > bbb)
        					return 1;
        				else if(bbb > aaa)
        					return -1;
        				else
        				{
        					//alert("can't have equal designations. They should be unique.")
        				}
        			}
        		});
        		var first_person_encountered = false;
        		for(var a = 0; a < designations.length; a++)
        		{
        			if(designations[a].acct_type === "person" && !first_person_encountered)
        			{
        				mds = mds + "	<tr>";
        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;\" colspan=5>";
        				mds = mds + "			<hr>";
        				mds = mds + "		</td>";
        				mds = mds + "	</tr>";
        				mds = mds + "	<tr>";
        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Name</td>";
        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Acct type</td>";
        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Twitter</td>";
        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Facebook</td>";
        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">FB brand page</td>";
        				mds = mds + "	</tr>";
        				first_person_encountered = true;
        			}
        			mds = mds + "	<tr>";
        			mds = mds + "		<td style=\"vertical-align:top;text-align:left;\">";
        			mds = mds + "							" + designations[a].display_name + "";
        			mds = mds + "		</td>";
        			mds = mds + "		<td style=\"vertical-align:top;text-align:left;\">";
        			mds = mds + "							" + designations[a].acct_type + "";
        			mds = mds + "		</td>";
        			mds = mds + "		<td style=\"vertical-align:top;text-align:left;\">";
        			if(designations[a].twitter_handle)
        			{
        				if(designations[a].twitter_connected && designations[a].twitter_connected === "yes")
        					mds = mds + " 			<span style=\"color:blue\">" + designations[a].twitter_handle + "</span>";
        				else
        					mds = mds + " 			<a href=\"#\" id=\"" + designations[a].designation + "_twitter_link\" style=\"color:red\">" + designations[a].twitter_handle + "</a>";
        			}
        			else
        			{
        				mds = mds + " 			<span style=\"color:red\">UNKNOWN</span>";
        			}
        			mds = mds + "		</td>";
        			mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
        			if(designations[a].facebook_connected && designations[a].facebook_connected === "yes")
        				mds = mds + " 			<span style=\"color:blue\">facebook</span>";
        			else
        				mds = mds + " 			<a href=\"#\" id=\"" + designations[a].designation + "_facebook_link\" style=\"color:red\">facebook</a>";
        			mds = mds + "		</td>";
        			mds = mds + "		<td style=\"vertical-align:top;text-align:left\">";
        			if(designations[a].facebook_connected && designations[a].facebook_connected === "yes")
        			{
        				if(designations[a].facebook_accounts.length > 0)
        				{
        					if(designations[a].facebook_account_id)
        					{	
        						mds = mds + " 			<table> <!-- 1 -->";
	        					for(var b=0; b < designations[a].facebook_accounts.length; b++)
		        				{
	        						// <span style=\"color:red\">
	        						if((designations[a].facebook_account_id+"") === designations[a].facebook_accounts[b].id)
	        							mds = mds + " 			<tr><td style=\"valign:middle\"><input type=radio checked id=\"" + designations[a].designation + "_" + designations[a].facebook_accounts[b].id + "_radio\"></td><td style=\"color:blue\">" + designations[a].facebook_accounts[b].name + "</td></tr>";
	        						else	
	        						{
	        							// do nothing. Don't need to display all this person's accounts to everyone
	        							//mds = mds + " 			<tr><td style=\"valign:middle\"><input type=radio id=\"" + designations[a].facebook_accounts[b].id + "_radio\"></td><td>" + designations[a].facebook_accounts[b].name + "</td></tr>";
	        						}
		        				}
	        					mds = mds + " 			</table>";
        					}
        					else
        					{
        						mds = mds + " 			<table> <!-- 2 -->";
	        					for(var b=0; b < designations[a].facebook_accounts.length; b++)
		        				{
	        						mds = mds + " 			<tr><td style=\"valign:middle\"><input type=radio id=\"" + designations[a].designation + "_" + designations[a].facebook_accounts[b].id + "_radio\"></td><td>" + designations[a].facebook_accounts[b].name + "</td></tr>";
		        				}
	        					mds = mds + " 			</table>";
        					}	
        					
        					
        				}
        				else
        				{
        					mds = mds + " 			<span style=\"color:red\">no accts detected</span>";
        				}
        			}
        			else
        			{
        				mds = mds + " 			<span style=\"color:red\">link fb first</span>";
        			}
        			mds = mds + "		</td>";
        			mds = mds + "	</tr>";
        		}	
        		mds = mds + "</table>";
        		mds = mds + "</div>";
        		$("#main_td").html(mds);
        		
        		for(var a = 0; a < designations.length; a++)
        		{
        			$("#" + designations[a].designation + "_twitter_link").click({value1: designations[a].designation},
        				function (event) {
        				docCookies.setItem("designation", event.data.value1, 31536e3);
        				var oauth_token = null;
        				$.ajax({
        					type: 'GET',
        					url: endpoint,
        					data: {
        			            method: "startTwitterAuthentication",
        			            designation: event.data.value1,
        			            huzon_auth: huzon_auth
        					},
        			        dataType: 'json',
        			        async: false,
        			        success: function (data, status) {
        			        	if (data.response_status == "error")
        			        		$("#message_div").html("<span style=\"font-size:16;color:red\">error getting station designations</span>");
        			        	else
        			        	{
        			        		$("#message_div").html("<span style=\"font-size:16;color:blue\">Twitter authentication started...</span>");
        			        		oauth_token = data.oauth_token;
        			        		//oauth_token_secret = data.oauth_token_secret;
        			        		
        			        		window.location.href = "https://api.twitter.com/oauth/authenticate?oauth_token=" + oauth_token;
        	        		
        			        	}
        			        }
        			        ,
        			        error: function (XMLHttpRequest, textStatus, errorThrown) {
        			        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
        			            console.log(textStatus, errorThrown);
        			        }
        				});
        					return false;
        				}
        			);
        			
        			$("#" + designations[a].designation + "_facebook_link").click({value1: designations[a].designation},
        					function (event) {
        					var randomnumber=Math.floor(Math.random()*1000000);
        					docCookies.setItem("state", randomnumber+"", 31536e3);
        					docCookies.setItem("designation", event.data.value1, 31536e3);
        					window.location.href = "https://www.facebook.com/dialog/oauth?client_id=176524552501035&redirect_uri=https://www.huzon.tv/registration.html&scope=publish_stream,manage_pages&state=" + randomnumber;
        				
        						return false;
        					}
        				);
        			
        			if(designations[a].facebook_connected && designations[a].facebook_connected === "yes")
        			{
        				if(designations[a].facebook_accounts.length > 0)
        				{
        					for(var b=0; b < designations[a].facebook_accounts.length; b++)
	        				{	
	        					$("#" + designations[a].designation + "_" + designations[a].facebook_accounts[b].id + "_radio").click({id: designations[a].facebook_accounts[b].id, designation: designations[a].designation, name: designations[a].facebook_accounts[b].name},
		        					function (event) {
	        							//alert("Setting subaccount for parent account: designation=" + event.data.designation + " fb_name=" + event.data.name + " fb_id=" + event.data.id);
	        							$.ajax({
	    	        						type: 'GET',
	    	        						url: endpoint,
	    	        						data: {
	    	        				            method: "setFacebookSubAccountInfo",
	    	        				            designation: event.data.designation,
	    	        				            id: event.data.id
	    	        						},
	    	        				        dataType: 'json',
	    	        				        async: false,
	    	        				        success: function (data, status) {
	    	        				        	if (data.response_status == "error")
	    	        				        		$("#message_div").html("<span style=\"font-size:16;color:red\">error setting designated account. message= " + data.message + "</span>");
	    	        				        	else
	    	        				        	{
	    	        				        		$("#message_div").html("<span style=\"font-size:16;color:blue\">Designated account is now set. Thanks! (Your non-selected pages will be hidden to others.)</span>");
	    	        				        		window.location.reload();
	    	        				        	}
	    	        				        }
	    	        				        ,
	    	        				        error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	        				        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
	    	        				            console.log(textStatus, errorThrown);
	    	        				        }
	    	        					});
	        						}
	        					);
	        				}
        				}
        			}
        			
        		}
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
            console.log(textStatus, errorThrown);
        }
	});
}*/

