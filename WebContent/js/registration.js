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



var endpoint = "https://www.hoozon.tv/endpoint";
//var endpoint = "http://localhost:8080/hoozontv/endpoint";

document.addEventListener('DOMContentLoaded', function () {
	
	var hoozon_auth = docCookies.getItem("hoozon_auth");
	if(location.protocol !== "https:")
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.hoozon.tv/registration.html\">https://www.hoozon.tv/registration.html</a> instead.</span>");
	}
	else if(typeof hoozon_auth === undefined || hoozon_auth === null || hoozon_auth === "")
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">Please enter the password you were provided. </span>");
		var mds = "<input type=password id=\"hoozon_auth_input\"> <input type=button id=\"hoozon_auth_go_button\" value=\"go\">";
		$("#main_td").html(mds);
		$("#hoozon_auth_go_button").click(function () {
				//docCookies.setItem("hoozon_auth", $("#hoozon_auth_input").val(), 31536e3);
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "checkPassword",
			            password: $("#hoozon_auth_input").val()
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
			        		docCookies.setItem("hoozon_auth", $("#hoozon_auth_input").val(), 31536e3);
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
		            hoozon_auth: hoozon_auth
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
		        		$("#message_div").html("<span style=\"font-size:16;color:blue\">You have successfully linked your Twitter account to hoozon.tv! Thanks!</span>");
		        	}
		        }
		        ,
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#main_td").html("ajax error");
		            console.log(textStatus, errorThrown);
		        }
			});
		}	
		
		var facebook_code = getParameterByName("code");
		if(facebook_code !== "")
		{
			var facebook_state = getParameterByName("state");
			var state = docCookies.getItem("state");
			var designation = docCookies.getItem("designation");
			if(state === facebook_state)
			{
				// this is a response from facebook after user has granted access
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "getFacebookAccessTokenFromAuthorizationCode",
			            facebook_code: facebook_code,
			            designation: designation,
			            hoozon_auth: hoozon_auth
					},
			        dataType: 'json',
			        async: false,
			        success: function (data, status) {
			        	if (data.response_status == "error")
			        	{
			        		$("#message_div").html("<span style=\"font-size:14;color:red\">Error: " + data.message + " </span>");
			        	}
			        	else
			        	{
			        		$("#message_div").html("<span style=\"font-size:16;color:blue\">You have successfully linked your Facebook account to hoozon.tv! Thanks!</span>");
			        	}
			        }
			        ,
			        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	$("#message_div").html("ajax error");
			            console.log(textStatus, errorThrown);
			        }
				});
			}	
			else
			{
				$("#message_div").html("<span style=\"font-size:14;color:red\">State variables did not match. Please start over.</span>");
			}
		}	
		
		var designations = null;
		$.ajax({
			type: 'GET',
			url: endpoint,
			data: {
	            method: "getDesignationsAndAccounts",
	            station: "wkyt",
	            include_master: "yes",
	            hoozon_auth: hoozon_auth
			},
	        dataType: 'json',
	        async: false,
	        success: function (data, status) {
	        	if (data.response_status == "error")
	        		$("#message_div").html("error getting station designations");
	        	else
	        	{
	        		designations = data.designations;
	        		//$("#message_div").html("Designations successfully retrieved.");

	        		var mds = "";
	        		mds = mds + "<div style=\"font-size:16px;font-weight:bold\">";
	        		mds = mds + "<table style=\"margin-left:auto;margin-right:20px;border-spacing:10px\">";
	        		mds = mds + "	<tr>";
	        		mds = mds + "		<td colspan=4 style=\"vertical-align:top;text-align:left\">";
	        		mds = mds + "			<span style=\"color:blue;font-weight:bold\">BLUE</span> = account is linked</span><br>";
	        		mds = mds + "			<span style=\"color:red;font-weight:bold\">RED</span> = account is not linked</span><br>";
	        		mds = mds + "		</td>";
	        		mds = mds + "	</tr>";
	        		mds = mds + "	<tr>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;\" colspan=4>";
	        		mds = mds + "			<hr>";
	        		mds = mds + "		</td>";
	        		mds = mds + "	</tr>";
	        		mds = mds + "	<tr>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Name</td>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Acct type</td>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Twitter</td>";
	        		mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Facebook</td>";
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
	        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;\" colspan=4>";
	        				mds = mds + "			<hr>";
	        				mds = mds + "		</td>";
	        				mds = mds + "	</tr>";
	        				mds = mds + "	<tr>";
	        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Name</td>";
	        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Acct type</td>";
	        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Twitter</td>";
	        				mds = mds + "		<td style=\"vertical-align:top;text-align:left;font-weight:bold;\">Facebook</td>";
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
	        			            hoozon_auth: hoozon_auth
	        					},
	        			        dataType: 'json',
	        			        async: false,
	        			        success: function (data, status) {
	        			        	if (data.response_status == "error")
	        			        		$("#message_div").html("error getting station designations");
	        			        	else
	        			        	{
	        			        		$("#message_div").html("Twitter authentication started...");
	        			        		oauth_token = data.oauth_token;
	        			        		//oauth_token_secret = data.oauth_token_secret;
	        			        		
	        			        		window.location.href = "https://api.twitter.com/oauth/authenticate?oauth_token=" + oauth_token;
	        	        		
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
	        			
	        			$("#" + designations[a].designation + "_facebook_link").click({value1: designations[a].designation},
	        					function (event) {
	        					var randomnumber=Math.floor(Math.random()*1000000);
	        					docCookies.setItem("state", randomnumber+"", 31536e3);
	        					docCookies.setItem("designation", event.data.value1, 31536e3);
	        					window.location.href = "https://www.facebook.com/dialog/oauth?client_id=176524552501035&redirect_uri=https://www.hoozon.tv/registration.html&scope=publish_actions&state=" + randomnumber;
	        					/*var oauth_token = null;
	        					$.ajax({
	        						type: 'GET',
	        						url: endpoint,
	        						data: {
	        				            method: "startFacebookAuthentication",
	        				            designation: event.data.value1
	        						},
	        				        dataType: 'json',
	        				        async: false,
	        				        success: function (data, status) {
	        				        	if (data.response_status == "error")
	        				        		$("#message_div").html("error getting station designations");
	        				        	else
	        				        	{
	        				        		$("#message_div").html("Twitter authentication started. jsonresponse=" + JSON.stringify(data));
	        				        		oauth_token = data.oauth_token;
	        				        		oauth_token_secret = data.oauth_token_secret;
	        				        		
	        				        		window.location.href = "https://api.twitter.com/oauth/authenticate?oauth_token=" + oauth_token;
	        		        		
	        				        	}
	        				        }
	        				        ,
	        				        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        				        	$("#message_div").html("ajax error");
	        				            console.log(textStatus, errorThrown);
	        				        }
	        					});*/
	        						return false;
	        					}
	        				);
	        		}
	        	}
	        }
	        ,
	        error: function (XMLHttpRequest, textStatus, errorThrown) {
	        	$("#message_div").html("ajax error");
	            console.log(textStatus, errorThrown);
	        }
		});
	}
	
});
	
	

