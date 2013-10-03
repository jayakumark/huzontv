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


function isIE () {
	  var myNav = navigator.userAgent.toLowerCase();
	  return (myNav.indexOf('msie') != -1) ? parseInt(myNav.split('msie')[1]) : false;
	}

var devel = true;

document.addEventListener('DOMContentLoaded', function () {
	
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	if(isIE())
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">For security reasons, this page does not support Microsoft Internet Explorer. Please use Chrome, Firefox, Safari or Opera instead. Thanks!</span>");
	}
	else if(location.protocol !== "https:")
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.huzon.tv/futurepost.html\">https://www.huzon.tv/futurepost.html</a> instead.</span>");
	}
	else if(twitter_handle === null)
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">You are not logged in. Click <a href=\"https://www.huzon.tv/registration.html\">here</a> to login/register.</span>");
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
			async: false,
			success: function (data, status) {
				if (data.response_status === "error")
				{
					if(data.error_code && data.error_code === "07734") // the twitter cookie credentials were invalid. Delete them and reload page (start over).
					{
						docCookies.removeItem("twitter_access_token");
						docCookies.removeItem("twitter_handle");
						window.location.href = "https://www.huzon.tv/registration.html";
					}
					else
					{
						// shouldn't something happen here?
					}
				}
				else // getSelf was successful, meaning twitter_handle and twitter_access_token were OK
				{
					var mds = "";
					mds = mds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:30px\">";
					mds = mds + "	<tr>";
					mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-size:20px\">";
					mds = mds + "			CURRENT SETTINGS FOR <span style=\"color:green\">" + data.user_jo.display_name + "</span>";
					mds = mds + "		</td>";
					mds = mds + "	</tr>";
					mds = mds + "	<tr>";
					mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-size:16px\">";
					mds = mds + "			This is where a user's current future TW post settings would be displayed, including the option to delete it.";
					mds = mds + "		</td>";
					mds = mds + "	</tr>";
					mds = mds + "	<tr>";
					mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-size:16px\">";
					mds = mds + "			This is where a user's current future FB post settings would be displayed, including the option to delete it.";
					mds = mds + "		</td>";
					mds = mds + "	</tr>";
					mds = mds + "	<tr>";
					mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-size:16px\">";
					mds = mds + "			<p>SET A NEW FUTURE POST HERE:</p>";
					mds = mds + "			<p>Dear huzon.tv,</p>";
					mds = mds + "			<p>If you see me on MONTH, DAY, YEAR, between HH:MM and HH:MM, please use the following text:</p>";
					mds = mds + "			<p><textarea rows=10 cols=60>MY NAME is on the air.</textarea></p>";
					mds = mds + "			<p>Thanks, USERNAME.</p>";
					mds = mds + "			<p><input type=submit></p>";
					mds = mds + "		</td>";
					mds = mds + "	</tr>";
					mds = mds + "</table>";
					$("#main_div").html(mds);
				}
			}
		});
	}
});