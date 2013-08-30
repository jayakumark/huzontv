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
		$("#message_div").html("<span style=\"font-size:16;color:red\">For security reasons, this registration page does not support Microsoft Internet Explorer. Please use Chrome, Firefox, Safari or Opera instead. Thanks!</span>");
	}
	else if(location.protocol !== "https:")
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.huzon.tv/registration.html\">https://www.huzon.tv/registration.html</a> instead.</span>");
	}
	else if(twitter_handle === null)
	{
		//alert("twitter_handle === null");
		var oauth_verifier = getParameterByName("oauth_verifier");
		if(typeof oauth_verifier !== undefined && oauth_verifier != null && oauth_verifier !== "") // no twitter_handle cookie, but oauth_verifier is present, signifying response from twitter after user has granted access
		{
			//alert("oauth_verifier !== null");
			// this is a response from twitter after user has granted access
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
		        	if (data.response_status === "error")
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:red\">gTATFAC Error: " + data.message + " </span>");
		        	}
		        	else
		        	{
		        		//$("#message_div").html("<span style=\"font-size:16;color:blue\">You have successfully linked your Twitter account (" +  data.twitter_handle + ") to huzon.tv!<br><br>Please wait. Reloading page...</span>");
		        		docCookies.setItem("twitter_handle", data.twitter_handle, 604800);
		        		docCookies.setItem("twitter_access_token", data.twitter_access_token, 604800);
		        		//alert('twitter_handle null, oauth verifier existed, returning from call to gTATFAC. Reloading.');
		        		window.location.href = "https://www.huzon.tv/registration.html";
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
			//alert("oauth_verifier is undefined, null or empty");
			$("#message_div").html("<span style=\"font-size:16;color:red\">Please log in with your Twitter account.</span>");
			$("#main_div").html("<a href=\"#\" id=\"sign_in_with_twitter_link\">Sign in with Twitter</a><br><br>Note: Even if you are exempted from huzon.tv Twitter postings (as determined by your station's management), you still need to log in with Twitter since it is our authentication method. This is true for station administrators as well.");
			
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
				        	if (data.response_status === "error")
				        	{
				        		$("#message_div").html("<span style=\"font-size:16;color:red\">sTA Error: " + data.message + "</span>");
				        	}
				        	else
				        	{
				        		$("#message_div").html("<span style=\"font-size:16;color:blue\">Twitter authentication started...</span>");
				        		//$("#main_div").html("");
				        		oauth_token = data.oauth_token;
				        		//alert('twitter_handle null, oauth did not exist, returning from call to startTwitterAuthentication. Reloading.');
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
		var facebook_code = getParameterByName("code");
		if(facebook_code !== "")
		{
			var facebook_state = getParameterByName("state");
			var state = docCookies.getItem("state");
			if(state === facebook_state)
			{
				// this is a redirect from facebook after user has granted access
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "getFacebookAccessTokenFromAuthorizationCode",
			            facebook_code: facebook_code,
			            twitter_handle: twitter_handle,
			            twitter_access_token: twitter_access_token
					},
			        dataType: 'json',
			        async: false,
			        success: function (data, status) {
			        	if (data.response_status === "error")
			        	{
			        		$("#message_div").html("<span style=\"font-size:14;color:red\">gFATFAC Error: " + data.message + " </span>");
			        		docCookies.removeItem("state");
			        	}
			        	else
			        	{
			        		//$("#message_div").html("<span style=\"font-size:16;color:blue\">You have successfully linked your Facebook account to huzon.tv! Thanks!</span>");
			        		docCookies.removeItem("state");
			        		//alert('twitter_handle not null, fb code exists, returning from call to getFBAccessTokenFromAuthCode. Reloading.');
			        		window.location.href = "https://www.huzon.tv/registration.html";
			        	}
			        }
			        ,
			        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
			        	docCookies.removeItem("state");
			            console.log(textStatus, errorThrown);
			        }
				});
			}	
			else
			{
				docCookies.removeItem("state");
				$("#message_div").html("<span style=\"font-size:14;color:red\">State variables did not match. Please start over by clicking <a href=\"https://www.huzon.tv/registration.html\">HERE</a>.</span>");
			}
		}	
		else
		{	
			//alert("twitter_handle and twitter_access_token cookies !== null");
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
		        		// no need to produce any error, just reload 
		        		//$("#message_div").html("<span style=\"font-size:16;color:red\">gS Error: " + data.message + "</span>");
		        		if(data.error_code && data.error_code === "07734") // the twitter cookie credentials were invalid. Delete them and reload page (start over).
		        		{
		        			docCookies.removeItem("twitter_access_token");
		        			docCookies.removeItem("twitter_handle");
		        			//alert('twitter_handle not null, fb code did not exist, returning from call to getSelf where error code was 07734. Wiping cookies and reloading.');
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
		        		mds = mds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:10px\">";
		        		mds = mds + "	<tr>";
		        		mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-weight:bold;font-size:20px\" colspan=2>";
		        		mds = mds + "			Information for <span style=\"color:green\">" + data.user_jo.display_name + "</span>";
		        		mds = mds + "		</td>";
		        		mds = mds + "	</tr>";
		        		mds = mds + "	<tr>";
		        		mds = mds + "		<td style=\"vertical-align:top;text-align:center;font-size:20px\" colspan=2>";
		        		/*if(data.user_jo.appears_on_air == false) 
		        		{
		        			mds = mds + "Twitter linked? <span style=\"color:blue\">YES</span> (" + data.user_jo.twitter_handle + ") <img src=\"images/check.png\" style=\"width:20px;height:20px\"><br>";
		        			mds = mds + "<br><br>You're finished! Thanks! (Only on-air personalities need to link their Facebook accounts)";
		        			$("#main_div").html(mds);
		        		}	
		        		else
		        		{*/	
		        		var get_self_user_jo = data.user_jo;
		        			mds = mds + "1. Twitter linked? <span style=\"color:blue\">YES</span> (" + data.user_jo.twitter_handle + ") <img src=\"images/check.png\" style=\"width:20px;height:20px\"><br>";
			        		mds = mds + "2. Facebook linked? ";
			        		var fb_toplevel_valid = false;
			        		var fb_subaccounts_ja = null;
			        		$.ajax({
			        			type: 'GET',
			        			url: endpoint,
			        			data: {
			        	            method: "verifyTopLevelFBCredentialsSelf",
			        	            designation: data.user_jo.designation,
			        	            twitter_handle: twitter_handle,
			        	            twitter_access_token: twitter_access_token
			        			},
			        	        dataType: 'json', 
			        	        async: false,
			        	        success: function (data, status) {
			        	        	if (data.response_status === "error")
			        	        		mds = mds + "<span style=\"color:red\">ERROR</span> Please reload the page or contact huzon.tv support. Sorry.<br>";
			        	        	else if(data.response_status === "success")
			        	        	{
			        	        		//alert(JSON.stringify(data));
			        	        		if(data.valid === true)
			        	        		{
			        	        			mds = mds + "<span style=\"color:blue\">YES</span> ";
			        	        			if(typeof get_self_user_jo.facebook_name !== undefined && get_self_user_jo.facebook_name !== null && get_self_user_jo.facebook_name !== "")
			        	        			{
			        	        				mds = mds + " (" + get_self_user_jo.facebook_name + ")";
			        	        			}
			        	        			mds = mds + " <img src=\"images/check.png\" style=\"width:20px;height:20px\">";
			        	        			mds = mds + "<br>";
			        	        			fb_toplevel_valid = true;
			        	        		}
			        	        		else
			        	        			mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">NO</a> <img src=\"images/leftarrow.png\" style=\"width:20px;height:20px\"> click here <br>";
			        	        	}
			        	        }
			        	        ,
			        	        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	        	$("#" + designation + "_fb_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
			        	            console.log(textStatus, errorThrown);
			        	        }
			        		});
			        		
			        		mds = mds + "3. Facebook <i>reporter page</i> linked? ";
			        		var fb_page_valid = false;
			        		var fb_page_id = data.user_jo.facebook_page_id;
			        		$.ajax({
			        			type: 'GET',
			        			url: endpoint,
			        			data: {
			        	            method: "verifyPageFBCredentialsSelf",
			        	            designation: data.user_jo.designation,
			        	            twitter_handle: twitter_handle,
			        	            twitter_access_token: twitter_access_token
			        			},
			        	        dataType: 'json',
			        	        async: false,
			        	        success: function (data, status) {
			        	        	if (data.response_status === "error")
			        	        		mds = mds + "<span style=\"color:red\">ERROR</span> Please reload the page or contact huzon.tv support. Sorry.<br>";
			        	        	else if(data.response_status === "success")
			        	        	{
			        	        		var valid_page_id = "";
			        	        		if(data.valid === true)
			        	        		{
			        	        			mds = mds + "<span style=\"color:blue\">YES</span> <img src=\"images/check.png\" style=\"width:20px;height:20px\"><br>";
			        	        			fb_page_valid = true;

			        	        			// get the (determined to be valid) page information from the user object already retrieved
			        	        			valid_page_id = fb_page_id;  			
			        	        		}
			        	        		else
			        	        		{
			        	        			if(fb_toplevel_valid === true)
			        	        				mds = mds + "<span style=\"color:red\">NO</span> <img src=\"images/leftarrow.png\" style=\"width:20px;height:20px\"> select a page below <br>";
			        	        			else
			        	        				mds = mds + "<a href=\"#\" id=\"facebook_link\" style=\"color:red\">NO</a><br>";
			        	        		}

			        	        		// in either case...
		        	        			// get the page options from facebook so we can show user which one is currently selected (if any) and which other options are available.
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
		        					        	if (data.response_status === "error")
		        					        	{
		        					        		// This is ok. Probably just means the user has not linked their top-level account yet. Fail silently
		        					        	}
		        					        	else
		        					        	{
		        					        		//$("#message_div").html("<span style=\"font-size:16;color:blue\">Brand pages successfully retrieved from Facebook. Select the correct one below.</span>");
		        					        		fb_subaccounts_ja = data.fb_subaccounts_ja;
		        					        		// alert(fb_subaccounts_ja);
		        					        		//if(twitter_handle === "huzontv")  // this is to test when no subaccounts are found.
		        					        		//	fb_subaccounts_ja = [];
		        					        		if(typeof fb_subaccounts_ja === undefined || fb_subaccounts_ja == null || fb_subaccounts_ja.length === 0)
		        					        		{
		        					        			mds = mds + " 			<table style=\"margin-right:auto;margin-left:auto;font-size:20px\">";
		        					        			mds = mds + " 			<tr><td style=\"text-align:center;padding-top:10px;color:red;valign:middle;font-style:italic;font-size:12px;\">";
		        					        			mds = mds + " 				No subaccounts found under this FB account. Do you have a brand page?";
		        					        			mds = mds + " 				<br><br>To try a different FB account,<br>(1) click <a href=\"#\" id=\"facebook_reset_link\">here</a> to start over,<br>(2) switch accounts on facebook.com and<br>(3) return to this page.";
		        					        			mds = mds + " 				<br><br>Please contact help@huzon.tv or call 646-926-3101 for immediate assistance.";
		        					        			mds = mds + " 			</td></tr>";
		        					        			mds = mds + "			</table>";
		        					        		}	
		        					        		else //if(typeof fb_subaccounts_ja !== undefined && fb_subaccounts_ja !== null)
		        					        		{	
			        					        		mds = mds + " 			<table style=\"margin-right:auto;margin-left:auto;font-size:20px\">";
			        					        		for(var x=0; x < fb_subaccounts_ja.length; x++)
			        					        		{
			        					        			//alert("fb_page_valid=" + fb_page_valid + " comparison to true=" + (fb_page_valid === true));
			        					        			//alert("fb_subaccounts_ja[x].id=" + fb_subaccounts_ja[x].id + " valid_page_id=" + valid_page_id + "comparison=" + ((fb_subaccounts_ja[x].id*1) === (valid_page_id*1)));
			        					        			if(fb_page_valid === true && ((fb_subaccounts_ja[x].id*1) === (valid_page_id*1))) // this is the checked option
			        					        			{
			        					        				//alert(fb_subaccounts_ja[x].name + "checked");
			        					        				mds = mds + " 			<tr><td style=\"valign:middle\"><input name='fbsubaccounts' type='radio' CHECKED id=\"" + fb_subaccounts_ja[x].id + "_radio\"></td><td>" + fb_subaccounts_ja[x].name + " <img src=\"images/check.png\" style=\"width:20px;height:20px\"></td></tr>";
			        					        			}
			        					        			else
			        					        			{
			        					        				//alert(fb_subaccounts_ja[x].name + "not checked");
			        					        				mds = mds + " 			<tr><td style=\"valign:middle\"><input name='fbsubaccounts' type=radio id=\"" + fb_subaccounts_ja[x].id + "_radio\"></td><td>" + fb_subaccounts_ja[x].name + "</td></tr>";
			        					        			}
			        					        		}
			        					        		mds = mds + "			</table>";
			        					        		if(!fb_page_valid)
			        					        		{
			        					        			mds = mds + " 			<div style=\"text-align:center;padding-top:10px;color:red;valign:middle;font-style:italic;font-size:12px;\"> Not listed? To try a different top-level FB account,<br>(1) click <a href=\"#\" id=\"facebook_reset_link\">here</a> to start over,<br>(2) switch accounts on facebook.com and<br>(3) return to this page.</div>";
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
			        	        	}
			        	        }
			        	        ,
			        	        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	        	$("#" + designation + "_fb_valid_td").html("<span style=\"color:red\">AJAX ERROR</span>");
			        	            console.log(textStatus, errorThrown);
			        	        }
			        		});
			        		
			        		if(fb_toplevel_valid && fb_page_valid)
			        		{
			        			mds = mds + "<br><br>You're finished! Thanks!";
			        		}	
			        		else if(!fb_toplevel_valid && fb_page_valid)
			        		{
			        			mds = mds + "<br><br>You're finished! Thanks!<br>(Your main Facebook account has become unlinked, but that's ok. All huzon.tv needs is a connection to your FB reporter page.)";
			        		}
			        		
			        		$("#main_div").html(mds);
			        		
			        		$("#facebook_reset_link").click(
		        					function() {
		        						$.ajax({
	    	        						type: 'GET',
	    	        						url: endpoint,
	    	        						data: {
	    	        				            method: "resetFacebookInfo",
	    	        				            twitter_handle: twitter_handle,
	    	    					            twitter_access_token: twitter_access_token,
	    	        						},
	    	        				        dataType: 'json',
	    	        				        async: false,
	    	        				        success: function (data, status) {
	    	        				        	if (data.response_status === "error")
	    	        				        		$("#message_div").html("<span style=\"font-size:16;color:red\">error setting designated account. message= " + data.message + "</span>");
	    	        				        	else
	    	        				        	{
	    	        				        		window.location.href = "https://www.huzon.tv/registration.html";
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
			        		
			        		$("#facebook_link").click({value1: data.user_jo.designation},
		        					function (event) {
			        					var randomnumber=Math.floor(Math.random()*1000000);
			        					var state = randomnumber+"";
			        					docCookies.setItem("state", state, 300);
			        					docCookies.setItem("designation", event.data.value1, 604800);
			        					alert('Sending you to Facebook. Grant ALL permissions you are prompted for. Otherwise, huzon.tv will not work properly.');
			        					window.location.href = "https://www.facebook.com/dialog/oauth?client_id=176524552501035&redirect_uri=https://www.huzon.tv/registration.html&scope=publish_stream,manage_pages&state=" + randomnumber;
			        					return false;
		        					}
		        				);
			        		
			        		if(fb_subaccounts_ja != null)
			        		{	
			        			for(var x=0; x < fb_subaccounts_ja.length; x++)
				        		{
			        				$("#" + fb_subaccounts_ja[x].id + "_radio").click({fb_subaccount_id: fb_subaccounts_ja[x].id},
			            					function (event) {
			        							//alert('setting facebook sub account info with ' + event.data.fb_subaccount_id);
			        							$.ajax({
			    	        						type: 'GET',
			    	        						url: endpoint,
			    	        						data: {
			    	        				            method: "setFacebookSubAccountInfo",
			    	        				            twitter_handle: twitter_handle,
			    	    					            twitter_access_token: twitter_access_token,
			    	    					            fb_subaccount_id: event.data.fb_subaccount_id
			    	        						},
			    	        				        dataType: 'json',
			    	        				        async: false,
			    	        				        success: function (data, status) {
			    	        				        	if (data.response_status === "error")
			    	        				        		$("#message_div").html("<span style=\"font-size:16;color:red\">error setting designated account. message= " + data.message + "</span>");
			    	        				        	else
			    	        				        	{
			    	        				        		//alert('twitter_handle not null, fb code did not exist, getSelf was successful. Get get and setFBSubAccountInfo was successful. Reloading.');
			    	        				        		window.location.href = "https://www.huzon.tv/registration.html";
			    	        				        	}
			    	        				        }
			    	        				        ,
			    	        				        error: function (XMLHttpRequest, textStatus, errorThrown) {
			    	        				        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
			    	        				            console.log(textStatus, errorThrown);
			    	        				        }
			    	        					});
			        						});
				        		}
		    				}
		        		//}
		        	}
		        }
		        ,
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
		            console.log(textStatus, errorThrown);
		        }
			});
		}
	}
	else
	{
		// incorrect registration page state. Delete all cookies and start again.
		// logic above says
		// if secure
		// else if twitter_handle == null
		// else if twitter_handle !== null && twitter_access_token !== null
		// then do this section. So to reach here, twitter_access_token would have to be null while twitter_handle != null, logic above should be improved, I guess
		docCookies.removeItem("twitter_access_token");
		docCookies.removeItem("twitter_handle");
		docCookies.removeItem("state");
		docCookies.removeItem("designation");
		window.location.href = "https://www.huzon.tv/registration.html";
	}	
	
	
});
	

