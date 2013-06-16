var endpoint = "https://localhost:8443/huzontv/endpoint";
//var endpoint = "https://www.huzon.tv/endpoint";

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


var dateFormat = function () {
	var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
		timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
		timezoneClip = /[^-+\dA-Z]/g,
		pad = function (val, len) {
			val = String(val);
			len = len || 2;
			while (val.length < len) val = "0" + val;
			return val;
		};

	// Regexes and supporting functions are cached through closure
	return function (date, mask, utc) {
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(date)) throw SyntaxError("invalid date");

		mask = String(dF.masks[mask] || mask || dF.masks["default"]);

		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}

		var	_ = utc ? "getUTC" : "get",
			d = date[_ + "Date"](),
			D = date[_ + "Day"](),
			m = date[_ + "Month"](),
			y = date[_ + "FullYear"](),
			H = date[_ + "Hours"](),
			M = date[_ + "Minutes"](),
			s = date[_ + "Seconds"](),
			L = date[_ + "Milliseconds"](),
			o = utc ? 0 : date.getTimezoneOffset(),
			flags = {
				d:    d,
				dd:   pad(d),
				ddd:  dF.i18n.dayNames[D],
				dddd: dF.i18n.dayNames[D + 7],
				m:    m + 1,
				mm:   pad(m + 1),
				mmm:  dF.i18n.monthNames[m],
				mmmm: dF.i18n.monthNames[m + 12],
				yy:   String(y).slice(2),
				yyyy: y,
				h:    H % 12 || 12,
				hh:   pad(H % 12 || 12),
				H:    H,
				HH:   pad(H),
				M:    M,
				MM:   pad(M),
				s:    s,
				ss:   pad(s),
				l:    pad(L, 3),
				L:    pad(L > 99 ? Math.round(L / 10) : L),
				t:    H < 12 ? "a"  : "p",
				tt:   H < 12 ? "am" : "pm",
				T:    H < 12 ? "A"  : "P",
				TT:   H < 12 ? "AM" : "PM",
				Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
				o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
				S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
			};

		return mask.replace(token, function ($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();

// Some common format strings
dateFormat.masks = {
	"default":      "ddd mmm dd yyyy HH:MM:ss",
	shortDate:      "m/d/yy",
	mediumDate:     "mmm d, yyyy",
	longDate:       "mmmm d, yyyy",
	fullDate:       "dddd, mmmm d, yyyy",
	shortTime:      "h:MM TT",
	mediumTime:     "h:MM:ss TT",
	longTime:       "h:MM:ss TT Z",
	isoDate:        "yyyy-mm-dd",
	isoTime:        "HH:MM:ss",
	isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
	isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
	dayNames: [
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	],
	monthNames: [
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
	return dateFormat(this, mask, utc);
};

var reporters_ja;

document.addEventListener('DOMContentLoaded', function () {
		
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	if(location.protocol !== "https:")
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">This page must be accessed securely. Please visit <a href=\"https://www.huzon.tv/simulator.html\">https://www.huzon.tv/simulator.html</a> instead.</span>");
	}
	else if(twitter_handle === null ||  twitter_access_token === null)
	{
		$("#message_div").html("<span style=\"font-size:16;color:red\">You are not logged in. Please register or login at  <a href=\"https://www.huzon.tv/registration.html\">https://www.huzon.tv/registration.html.</a></span>");
	}
	else if(twitter_handle !== null && twitter_access_token !== null)
	{
		var administering_station = docCookies.getItem("administering_station");
		if(administering_station == null) // need to prompt the user for the station they want to administer.
		{	
			$.ajax({
				type: 'GET',
				url: endpoint,
				data: {
		            method: "getStations",
		            twitter_handle: twitter_handle,
		            twitter_access_token: twitter_access_token
				},
		        dataType: 'json',
		        async: false,
		        success: function (data, status) {
		        	if(data.response_status === 'success')
		        	{
		        		var mds = "";
		        		mds = mds + "<div style=\"font-size:16;color:green\">Select a station:</div>";
		        		for(var x = 0; x < data.stations_ja.length; x++)
		        		{
		        			mds = mds + "<div style=\"font-size:16\"><a href=\"#\" id=\"" + data.stations_ja[x].call_letters  + "_link\">" + data.stations_ja[x].call_letters  + "</a></div>";
		        		}	
		        		$("#message_div").html(mds);
		        		for(var x = 0; x < data.stations_ja.length; x++)
		        		{
		        			$("#" + data.stations_ja[x].call_letters  + "_link").click({value: data.stations_ja[x].call_letters},
		        					function (event) {
		        						docCookies.setItem("administering_station", event.data.value);
		        						window.location.reload();
		        					});
		        		}
		        	}
		        	else
		        	{
		        		$("#message_div").html("<span style=\"font-size:16;color:red\">Error: message=" + data.message  + "</span>");
		        	}
		        },
		        error: function (XMLHttpRequest, textStatus, errorThrown) {
		        	$("#message_div").html("<span style=\"font-size:16;color:red\">ajax error</span>");
		            console.log(textStatus, errorThrown);
		        }
			});
		}
		else
		{
			var mds = "";
    		mds = mds + "<div style=\"font-size:16;color:green\">You are administering " + administering_station + "</div>";
    		
    		$.ajax({
    			type: 'GET',
    			url: endpoint,
    			data: {
    	            method: "getActiveReporterDesignations",
    	            station: "wkyt",
    	            twitter_handle: twitter_handle,
		            twitter_access_token: twitter_access_token
    			},
    	        dataType: 'json',
    	        async: false,
    	        success: function (data, status) {
    	        	if (data.response_status == "error")
    	        		mds = mds + "<div style=\"font-size:16;color:red\">error getting station designations</div>";
    	        	else
    	        	{
    	        		mds = mds + "<div style=\"font-size:16;color:green\">Designations successfully retrieved. " + JSON.stringify(data.reporters_ja) + "</div>";
    	        		reporters_ja = data.reporters_ja;
    	        		displayAvailableFunctions();
    	        	}
    	        }
    	        ,
    	        error: function (XMLHttpRequest, textStatus, errorThrown) {
    	        	$("#results_div").html("ajax error");
    	            console.log(textStatus, errorThrown);
    	        }
    		});
    		
			$("#message_div").html(mds);
		}
	}
});



function displayAvailableFunctions() // user should have twitter_handle, twitter_access_token and administering_station cookies
{
	var twitter_handle = docCookies.getItem("twitter_handle");
	var twitter_access_token = docCookies.getItem("twitter_access_token");
	var station = docCookies.getItem("administering_station");
	var fds = "";
	fds = fds + "<div style=\"font-size:16px;font-weight:bold\">";
	fds = fds + "	Available functions:";
	fds = fds + "</div>"
	fds = fds + "<table style=\"width:100%;border-spacing:10px\">";
	fds = fds + "	<tr>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	fds = fds + "						<b>Function 1:</b> Get all still frames in a range (inclusive).";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function1_begin_input\" size=13 value=\"20130615_180100\"> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function1_end_input\" value=\"20130615_180105\" size=13> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "  						<input id=\"function1_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	fds = fds + "						<b>Function 2:</b> Get all frames for designation above auto-generated homogeneity threshold (inclusive)";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Designation: <select id=\"function2_designation_select\">";
	for(var a = 0; a < reporters_ja.length; a++)
	{
		fds = fds + "							<option value=\"" + reporters_ja[a] + "\">" + reporters_ja[a] + "</option>";
	}	
	fds = fds + "	</select> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Single modifier: <input type=\"text\" id=\"function2_singlemodifier_input\" value=\"1\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Delta: <input type=\"text\" id=\"function2_delta_input\" value=\".1\" size=4> ";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function2_begin_input\" size=13 value=\"20130615_180100\"> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function2_end_input\" value=\"20130615_180105\" size=13> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function2_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "	</tr>";
	fds = fds + "	<tr>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
	fds = fds + "						<b>Function 3:</b> Graph one designee over time (inclusive)";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Designation: <select id=\"function3_designation_select\">";
	for(var a = 0; a < reporters_ja.length; a++)
	{
		fds = fds + "						<option value=\"" + reporters_ja[a] + "\">" + reporters_ja[a] + "</option>";
	}	
	fds = fds + "							</select>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Single Thresh Modifier: <input type=\"text\" id=\"function3_singlemodifier_input\" value=\"1\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						MA Thresh Modifier: <input type=\"text\" id=\"function3_mamodifier_input\" value=\".67\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Moving Avg Window: <input type=\"text\" id=\"function3_mawindow_input\" value=\"5\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function3_begin_input\" size=13 value=\"20130615_180100\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function3_end_input\" value=\"20130615_180105\" size=13>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function3_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";

	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
	fds = fds + "						<b>Function 4:</b> Get alerts for a given timeframe (inclusive)";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Single Thresh Modifier: <input type=\"text\" id=\"function4_singlemodifier_input\" value=\"1.0\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						MA Thresh Modifier: <input type=\"text\" id=\"function4_mamodifier_input\" value=\".67\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Moving Avg Window: <input type=\"text\" id=\"function4_mawindow_input\" value=\"5\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	// alert waiting periods should be separated, but this function doesn't actually use one
	//fds = fds + "						Alert waiting period: <input type=\"text\" id=\"function4_awp_input\" value=\"7200\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Delta: <input type=\"text\" id=\"function4_delta_input\" value=\".1\" size=4> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function4_begin_input\" size=13 value=\"20130615_180100\"><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function4_end_input\" value=\"20130615_180105\" size=13><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function4_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "	</tr>";
	fds = fds + "	<tr>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	fds = fds + "						<b>Function 5:</b> Graph frame rates over time";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function5_begin_input\" size=13 value=\"20130615_180100\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function5_end_input\" value=\"20130615_180105\" size=13>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function5_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";

	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	fds = fds + "						<b>Function 6:</b> Simulate Alerts (inclusive)";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	/*fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Single Thresh Modifier: <input type=\"text\" id=\"function4_singlemodifier_input\" value=\"1.0\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						MA Thresh Modifier: <input type=\"text\" id=\"function4_mamodifier_input\" value=\".67\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Moving Avg Window: <input type=\"text\" id=\"function4_mawindow_input\" value=\"5\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	// alert waiting periods should be separated, but this function doesn't actually use one
	//fds = fds + "						Alert waiting period: <input type=\"text\" id=\"function4_awp_input\" value=\"7200\" size=4>";
	fds = fds + "					</td>";*/
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	//fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	//fds = fds + "						Delta: <input type=\"text\" id=\"function4_delta_input\" value=\".1\" size=4> ";
	//fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function6_begin_input\" size=13 value=\"20130615_180100\"><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function6_end_input\" value=\"20130615_180105\" size=13><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function6_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "	</tr>";
	fds = fds + "</table>";
	$("#functions_div").html(fds);
	
	$("#function1_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function1_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function1_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getFrames",
				            begin: begin,             
				            end: end,
				            station: station,
				            get_score_data: "false",
		    	            twitter_handle: twitter_handle,
				            twitter_access_token: twitter_access_token
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error message=" + data.message);
				        	else
				        	{
				        		if(data.frames_ja.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			for(var x = 0; x < data.frames_ja.length; x++)
				        			{
				        				rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
				        				rds = rds + "<img src=\"" + data.frames_ja[x].url + "\" style=\"width:250px;height:141px\">";
				        				rds = rds + "<br>" + data.frames_ja[x].image_name;
				        				rds = rds + "</div>";
				        			}
				        			$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function2_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function2_begin_input').val();
				
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				
				var begin = d.getTime()/1000;
				
				datestring = $('#function2_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				var reporter_homogeneity = 0;
				var designation = $('#function2_designation_select').val();
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "getUser",
			            designation: designation,
	    	            twitter_handle: twitter_handle,
			            twitter_access_token: twitter_access_token
					},
			        dataType: 'json',
			        async: true,
			        success: function (data, status) {
			        	if (data.response_status == "error")
			        		$("#results_div").html("error: " + data.message);
			        	else
			        	{
			        		reporter_homogeneity = data.user_jo.homogeneity;
			        	}
			        }
			        ,
			        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	$("#results_div").html("ajax error");
			            console.log(textStatus, errorThrown);
			        }
				});
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getFramesAboveDesignationHomogeneityThreshold",
				            begin: begin,             
				            end: end,
				            designation: $('#function2_designation_select').val(),
				            singlemodifier: $('#function2_singlemodifier_input').val(),
				            delta: $('#function2_delta_input').val(),
				            station: station,
		    	            twitter_handle: twitter_handle,
				            twitter_access_token: twitter_access_token
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error: " + data.message);
				        	else
				        	{
				        		if(data.frames_ja.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			if(data.frames_ja.length > 0)
				        			{	
				        				for(var x = 0; x < data.frames_ja.length; x++)
				        				{
				        					d = new Date(data.frames_ja[x].timestamp_in_ms);
				        					rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
				        					rds = rds + "<img src=\"" + data.frames_ja[x].url + "\" style=\"width:250px;height:141px\">";
				        					rds = rds + "<br>" + data.frames_ja[x].image_name;
				        					rds = rds + "<br>avg4des:"+ data.frames_ja[x].reporters[designation].score_avg;
				        					rds = rds + "<br>homogeneity:" + reporter_homogeneity;
				        					rds = rds + "<br>threshold:" + (reporter_homogeneity * $('#function2_singlemodifier_input').val());
				        					//rds = rds + "<br>closest_desg:" + data.frames_ja[x].closest_designation;
				        					//rds = rds + "<br>closest_avg:" + data.frames_ja[x].closest_avg;
				        					//rds = rds + "<br>closest_delta:" + (data.frames_ja[x].score_average - data.frames_ja[x].closest_avg);
				        					rds = rds + "</div>";
				        				}
				        			}
				        			else
				        				rds = "no matches";
				        			
				        		//	rds = "frames processed:" + data.frames_processed + "<br>delta suppressions:" + data.delta_suppressions + "<br>" + rds;
				        			$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function3_go_button").click(
			function () {
				$("#results_div").html("<span style=\"font-size:20px;font-weight:bold\">BLUE = frame scores<br>ORANGE = moving average<br>DASHED LINE = alert threshold (for moving average)</span>");
				$("#chart1").html("");
				var datestring = $('#function3_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function3_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				var designation = $('#function3_designation_select').val();
				var reporter_homogeneity = 0;
				var maw_int = $('#function3_mawindow_input').val();
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "getUser",
			            designation: designation,
	    	            twitter_handle: twitter_handle,
			            twitter_access_token: twitter_access_token
					},
			        dataType: 'json',
			        async: true,
			        success: function (data, status) {
			        	if (data.response_status == "error")
			        		$("#results_div").html("error: " + data.message);
			        	else
			        	{
			        		reporter_homogeneity = data.user_jo.homogeneity;
			        	}
			        }
			        ,
			        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	$("#results_div").html("ajax error");
			            console.log(textStatus, errorThrown);
			        }
				});
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getFrames",
				            begin: begin,             
				            end: end,
				            station: station,
				            get_score_data: "true",
		    	            twitter_handle: twitter_handle,
				            twitter_access_token: twitter_access_token
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error message=" + data.message);
				        	else if (data.response_status == "success")
				        	{
				        		//alert("success");
				        		if(data.frames_ja.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			var scores = []; 
				        			var moving_avg = [];
				        			var ma = 0;
				        			var sum = 0; 
				        			var num = 0;
				        			var ts = 0;
				        			for(var x = 0; x < data.frames_ja.length; x++)
				        			{
				        				scores.push(data.frames_ja[x].reporters[designation].score_avg);
				        				
				        				sum = 0;
				        				num = 0;
				        				// loop through all the frames, looking for timestamps in the moving average window in the past
				        				ts = data.frames_ja[x].timestamp_in_ms;
				        				for(var y = 0; y < data.frames_ja.length; y++) 
					        			{
				        					// if the timestamp of this frame is within the moving average window x seconds in the past, then add this designation's score
				        					// to a running total.
				        					if(data.frames_ja[y].timestamp_in_ms > (ts - (maw_int * 1000)) && data.frames_ja[y].timestamp_in_ms <= ts)
				        					{
				        						sum = sum + data.frames_ja[y].reporters[designation].score_avg;
				        						num++;
				        					}
					        			}
				        				ma = sum / num; // now derive the moving average for this designation
				        				moving_avg.push(ma);
				        			}
				        			var plot1 = $.jqplot ('chart1', [scores, moving_avg],{
				        				axes: {
				        					yaxis: {
				        			            min:0,max:1
				        			        }
				        				}
				        				,
				        				canvasOverlay: {
				        					show: true,
				        			        objects: [
				        			                  {horizontalLine: {
							        			            name: 'pebbles',
							        			            y: (reporter_homogeneity * $('#function3_singlemodifier_input').val()),
							        			            lineWidth: 3,
							        			            color: 'rgb(100, 55, 124)',
							        			            shadow: true,
							        			            lineCap: 'butt',
							        			            xOffset: 0
							        			          }},  
				        			          {dashedHorizontalLine: {
				        			            name: 'bam-bam',
				        			            y: (reporter_homogeneity * $('#function3_mamodifier_input').val()),
				        			            lineWidth: 4,
				        			            dashPattern: [8, 16],
				        			            lineCap: 'round',
				        			            xOffset: '25',
				        			            color: 'rgb(66, 98, 144)',
				        			            shadow: false
				        			          }}
				        			        ]
				        			      }
				        			    });
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});

	$("#function4_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function4_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function4_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getAlertFrames",
				            begin: begin,             
				            end: end,
				            mamodifier: $('#function4_mamodifier_input').val(),
				            singlemodifier: $('#function4_singlemodifier_input').val(),
				            mawindow: $('#function4_mawindow_input').val(),
				            delta:  $('#function4_delta_input').val(),
				            station: station,
		    	            twitter_handle: twitter_handle,
				            twitter_access_token: twitter_access_token
						},
				        dataType: 'json',
				        async: false,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error message=" + data.message);
				        	else
				        	{
				        		//alert("response_status=success");
				        		if(data.alert_frames_ja)
			        			{	
				        			data.alert_frames_ja.sort(function(a,b){
				        				a = a.timestamp_in_ms;
				        				b = b.timestamp_in_ms;
				        				return a - b;
				        			});
				        			
				        			
			        				for(var x = 0; x < data.alert_frames_ja.length; x++)
			        				{
			        					rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
				        				rds = rds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\"><tr><td style=\"text-align:right;vertical-align:middle\"><img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\"></td><td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td></tr></table>";
				        				rds = rds + "<br><img src=\"" + data.alert_frames_ja[x].url + "\" style=\"width:250px;height:141px\">";
				        				rds = rds + "<br>image_name:" + data.alert_frames_ja[x].image_name;
				        				rds = rds + "<br>designation:" + data.alert_frames_ja[x].designation;
				        				rds = rds + "<br>score_for_alert_frame:" + data.alert_frames_ja[x].score_for_alert_frame;
				        				rds = rds + "<br>score_for_frame_that_passed:" + data.alert_frames_ja[x].score_for_frame_that_passed;
				        				rds = rds + "<br>ma_for_alert_frame:" + data.alert_frames_ja[x].ma_for_alert_frame;
				        				rds = rds + "<br>ma_for_frame_that_passed:" + data.alert_frames_ja[x].ma_for_frame_that_passed;
				        				rds = rds + "<br>des homogeneity:" + data.alert_frames_ja[x].homogeneity;
				        				rds = rds + "<br>des single thresh:" + data.alert_frames_ja[x].single_threshold;
				        				rds = rds + "<br>des ma thres:" + data.alert_frames_ja[x].ma_threshold;
				        				rds = rds + "</div>";
			        				}
			        			}
				        
			        			$("#results_div").html(rds);
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#results_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function5_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function5_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function5_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getFrames",
				            begin: begin,             
				            end: end,
				            station: station,
		    	            twitter_handle: twitter_handle,
				            twitter_access_token: twitter_access_token
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error message=" + data.message);
				        	else
				        	{
				        		if(data.frames_ja.length > 100000)
				        		{
				        			$("#results_div").html("too many results. Try again.");
				        		}	
				        		else
				        		{
				        			var frame_rates = []; 
				        			var max_frame_rate = 0;
				        			for(var x = 0; x < data.frames_ja.length; x++)
				        			{
				        				frame_rates.push(data.frames_ja[x].frame_rate);
				        				if(data.frames_ja[x].frame_rate > max_frame_rate)
				        					max_frame_rate = data.frames_ja[x].frame_rate;
				        			}
				        			var plot1 = $.jqplot ('chart1', [frame_rates],{
				        				axes: {
				        					yaxis: {
				        			            min:0,max:max_frame_rate
				        			        }
				        				},
				        				canvasOverlay: {
				        					show: true,
				        			        objects: [
				        			              /*    {horizontalLine: {
							        			            name: 'pebbles',
							        			            y: 1000,
							        			            lineWidth: 3,
							        			            color: 'rgb(100, 55, 124)',
							        			            shadow: true,
							        			            lineCap: 'butt',
							        			            xOffset: 0
							        			          }},*/  
				        			          {dashedHorizontalLine: {
				        			            name: 'bam-bam',
				        			            y: 1000,
				        			            lineWidth: 4,
				        			            dashPattern: [8, 16],
				        			            lineCap: 'round',
				        			            xOffset: '25',
				        			            color: 'rgb(66, 98, 144)',
				        			            shadow: false
				        			          }}
				        			        ]
				        			      }
				        			    });
				        			
				        			/*for(var x = 0; x < data.frames_ja.length; x++)
				        			{
				        				rds = rds + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
				        				rds = rds + data.frames_ja[x].image_name + "-"  + data.frames_ja[x].frame_rate;
				        				rds = rds + "</div>";
				        			}
				        			$("#results_div").html(rds);*/
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				return;
			});
	
	$("#function6_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				var datestring = $('#function6_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function6_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;
				var timestamps_ja = null;
				
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "resetAllLastAlerts",
			            station: station,
			            twitter_handle: twitter_handle,
			            twitter_access_token: twitter_access_token
					},
			        dataType: 'json',
			        async: false,
			        success: function (data, status) {
			        	if (data.response_status == "error")
			        		$("#results_div").html("error: "  + data.message);
			        	else
			        	{
			        		//$("#results_div").html("success resetting all last alerts");
			        	}
			        }
			        ,
			        error: function (XMLHttpRequest, textStatus, errorThrown) {
			        	$("#results_div").html("ajax error");
			            console.log(textStatus, errorThrown);
			        }
				});	
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getFrameTimestamps",
				            begin: begin,             
				            end: end,
				            station: station,
		    	            twitter_handle: twitter_handle,
				            twitter_access_token: twitter_access_token
						},
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status == "error")
				        		$("#results_div").html("error message=" + data.message);
				        	else
				        	{
				        		if(data.timestamps_ja.length > 100)
				        		{
				        			alert("too many frames. Try a smaller window.");
				        		}	
				        		else
				        		{
				        			timestamps_ja = data.timestamps_ja;
					        		/*for(var x = 0; x < data.timestamps_ja.length; x++)
					        		{
					        			rds = rds + "<div>" + data.timestamps_ja[x] + "</div>";
					        		}
					        		$("#results_div").html(rds);*/
					        		
					        		timestamps_ja.sort();
					        		
					        		// now use the frames to simulate 
					        		//simulateNewFrame(timestamps_ja[0], station);
					        		
					        		for(var x = 0; x < data.timestamps_ja.length; x++)
					        		{
										//alert("simulating new frame with current_ts=" + current_ts + " end=" + end);
										rds = rds + simulateNewFrame(timestamps_ja[x],station);
					        		}	// end of for loop
					        		$("#results_div").html(rds);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
				
				
				
				
				return;
			});
	
}

function simulateNewFrame(timestamp_in_ms, station)
{
	var returnstring = "";
	var jsonpostbody = { 
			timestamp_in_ms: timestamp_in_ms,
			station: station,
            simulation: "true"
		};
	
	$.ajax({
		type: 'POST',
		url: endpoint,
		data: {
			method: "commitFrameDataAndAlert",
            jsonpostbody: JSON.stringify(jsonpostbody)
		},
        dataType: 'json',
        async: false,
        success: function (data, status) {
        	if (data.response_status == "error")
        		$("#results_div").html("error message=" + data.message);
        	else
        	{
        		if(data.alert_triggered === "yes" && data.alert_fired === "yes")
        		{
        			returnstring = "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
    				returnstring = returnstring + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\">";
    				returnstring = returnstring + "<tr>";
    				returnstring = returnstring + "		<td style=\"text-align:right;vertical-align:middle\">";
    				if(data.social_type == "twitter" || data.social_type == "both")
    					returnstring = returnstring + "			<img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\">";
    				if(data.social_type == "facebook" || data.social_type == "both")
    					returnstring = returnstring + "			<img src=\"images/facebook_logo_small1.jpg\" style=\"width:30px;height26px;\">";
    				returnstring = returnstring + "		</td>";
    				returnstring = returnstring + "		<td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td>";
    				returnstring = returnstring + "	</tr>";
    				returnstring = returnstring + "</table>";
    				returnstring = returnstring + "<br><img src=\"" + data.url + "\" style=\"width:250px;height:141px\">";
    				returnstring = returnstring + "<br>image_name: " + data.image_name;
    				returnstring = returnstring + "<br>designation: " + data.designation;
    				returnstring = returnstring + "</div>";
        		}
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#message_div").html("ajax error");
            console.log(textStatus, errorThrown);
        }
	});
	return returnstring;
}
	


