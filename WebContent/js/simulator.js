//var endpoint = "https://localhost:8443/huzontv/endpoint";
var endpoint = "https://www.huzon.tv/endpoint";

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
		
	//docCookies.setItem("twitter_access_token", "11m");
	//docCookies.setItem("twitter_handle", "huzontv");
	
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
    	//	mds = mds + "<div style=\"font-size:16;color:green\">You are administering " + administering_station + "</div>";
    		
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
    	        		//mds = mds + "<div style=\"font-size:16;color:green\">Designations successfully retrieved.</div>";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function1_begin_input\" size=13 value=\"20130619_230355\"> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function1_end_input\" value=\"20130619_230410\" size=13> ";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function2_begin_input\" size=13 value=\"20130619_230355\"> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function2_end_input\" value=\"20130619_230410\" size=13> ";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function3_begin_input\" size=13 value=\"20130619_230355\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function3_end_input\" value=\"20130619_230410\" size=13>";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function4_begin_input\" size=13 value=\"20130619_230355\"><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function4_end_input\" value=\"20130619_230410\" size=13><br>";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function5_begin_input\" size=13 value=\"20130619_230355\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function5_end_input\" value=\"20130619_230410\" size=13>";
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
	//fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	//fds = fds + "						Delta: <input type=\"text\" id=\"function4_delta_input\" value=\".1\" size=4> ";
	//fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function6_begin_input\" size=13 value=\"20130619_230355\"><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function6_end_input\" value=\"20130619_230410\" size=13><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Designation: <select id=\"function6_designation_select\">";
	fds = fds + "							<option value=\"none\">none</option>";
	for(var a = 0; a < reporters_ja.length; a++)
	{
		fds = fds + "							<option value=\"" + reporters_ja[a] + "\">" + reporters_ja[a] + "</option>";
	}	
	fds = fds + "	</select> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function6_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "	</tr>";
	
	fds = fds + "	<tr>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	/*fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=3>";
	fds = fds + "						<b>Function 7:</b> Get fired alerts for time range (inclusive)";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function7_begin_input\" size=13 value=\"20130409_050000\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function7_end_input\" value=\"20130409_060000\" size=13>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function7_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";*/
	fds = fds + "		</td>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
	fds = fds + "						<b>Function 8:</b> Delete an alert";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Designation: <select id=\"function8_designation_select\">";
	for(var a = 0; a < reporters_ja.length; a++)
	{
		fds = fds + "							<option value=\"" + reporters_ja[a] + "\">" + reporters_ja[a] + "</option>";
	}	
	fds = fds + "							</select>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Type: <input type=\"text\" id=\"function8_social_type_input\" size=13 value=\"twitter\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Item ID: <input type=\"text\" id=\"function8_id_input\" value=\"\" size=13>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "   					<input id=\"function8_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
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
				var singlemodifier = $('#function3_singlemodifier_input').val();
				var mamodifier = $('#function3_mamodifier_input').val()
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
				        			graphFrameScoresAndMovingAverages(data.frames_ja, designation, reporter_homogeneity, singlemodifier, mamodifier, maw_int);
				        		
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
				        				rds = rds + "<br>score_for_frame_that_passed_ma_thresh:" + data.alert_frames_ja[x].score_for_frame_that_passed_ma_thresh;
				        				rds = rds + "<br>ma_for_alert_frame:" + data.alert_frames_ja[x].ma_for_alert_frame;
				        				rds = rds + "<br>ma_for_frame_that_passed_ma_thresh:" + data.alert_frames_ja[x].ma_for_frame_that_passed_ma_thresh;
				        				rds = rds + "<br>image_name_for_frame_that_passed_ma_thresh:<br>" + data.alert_frames_ja[x].image_name_for_frame_that_passed_ma_thresh;
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
				            get_score_data: "false",
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
				var designation = $('#function6_designation_select').val();
				var dograph = false;
				var reporter_homogeneity = 0;
				if(designation !== "none")
				{
					dograph = true;
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
				}
				$.ajax({
					type: 'GET',
					url: endpoint,
					data: {
			            method: "resetTestAlertTimers",
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
				        		if(data.timestamps_ja.length > 250)
				        		{
				        			alert("too many frames. Try a smaller window.");
				        		}	
				        		else
				        		{
				        			timestamps_ja = data.timestamps_ja;
				        			timestamps_ja.sort();
				        			$("#results_div").append("<div>" + data.timestamps_ja.length + " frames in range</div>");
					        		
				        			
				        			var frames_ja = [];
				        			var length = data.timestamps_ja.length;
				        			var index = 0;
				        			var currentframe = null;
					        		var doFrame = function(){
					        			if(index < length)
					        			{
					        				if(designation)
					        					currentframe = simulateNewFrame(data.timestamps_ja[index], station, designation);
					        				else
					        					currentframe = simulateNewFrame(data.timestamps_ja[index], station, null);
					        				//alert(JSON.stringify(currentframe));
					        				frames_ja.push(currentframe);
					        				//alert(JSON.stringify(frames_ja));
					        				if(dograph)
					        					graphFrameScoresAndMovingAverages(frames_ja, designation, reporter_homogeneity, 1, .67, 5);
					        			}
					        			index++;
					        		};
					        		// 264 copies.
					        		setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();setTimeout(function(){doFrame();},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);},500);
				        		}
				        	}
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#message_div").html("ajax error");
				            console.log(textStatus, errorThrown);
				        }
					});
			});
	
	$("#function8_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "deleteAlert",
				            designation: $('#function8_designation_select').val(),
				            social_type: $('#function8_social_type_input').val(),             
				            id: $('#function8_id_input').val(),
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
				        		rds = JSON.stringify(data);
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
	
}

var simulateNewFrame = function(timestamp_in_ms, station, designation){
//function simulateNewFrame(timestamp_in_ms, station) {
	var rds = "";
	var frame_to_display = null;
	var jsonpostbody;
	
	if(typeof designation === undefined || designation === null)
	{	
		jsonpostbody = { 
				timestamp_in_ms: timestamp_in_ms,
				station: station,
	            simulation: "true"
			};
	}
	else
	{
		jsonpostbody = { 
				timestamp_in_ms: timestamp_in_ms,
				station: station,
	            simulation: "true",
	            designation: designation
			};
	}
	
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
        		rds = "<div style=\"border: 1px black solid;display:inline-block\">";
        		
        		if(data.alert_triggered && (data.twitter_fired || data.facebook_fired))
        		{
    				rds = rds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:3px\">";
    				rds = rds + "<tr>";
    				rds = rds + "		<td style=\"text-align:right;vertical-align:middle\">";
    				if(data.twitter_fired == true)
    					rds = rds + "			<img src=\"images/twitter_logo_30x26.jpg\" style=\"width:30px;height26px;\">";
    				if(data.facebook_fired == true)
    					rds = rds + "			<img src=\"images/facebook_logo_small1.jpg\" style=\"width:30px;height26px;\">";
    				rds = rds + "		</td>";
    				rds = rds + "		<td style=\"text-align:left;vertical-align:middle;font-size:20px;font-weight:bold\">Alert fired!</td>";
    				rds = rds + "	</tr>";
    				rds = rds + "</table>";
    			//	rds = rds + "<br><img src=\"" + data.url + "\" style=\"width:426px;height:240px\">";
    			//	rds = rds + "<br>image_name: " + data.image_name;
    			//	rds = rds + "<br>(passing) image_name: " + data.image_name_of_frame_in_window_that_passed_single_thresh;
    			//	rds = rds + "<br>designation: " + data.designation;
    				/*
    				var index_of_fired_alert = 0;
    				for(var x = 0; x < data.frames_ja.length; x++)
    				{
    					if(data.frames_ja[x].image_name === data.image_name)
    					{
    						index_of_fired_alert = x;
    						break;
    					}
    				}	
    				frame_to_display = data.frames_ja[index_of_fired_alert];*/
    				// 4
    				/*var first_index = 0;
    				var second_index = Math.floor(data.frames_ja.length / 3);
    				var third_index = Math.floor(data.frames_ja.length * 2 / 3);
    				var fourth_index = data.frames_ja.length-1;
    				rds = rds + "<br><table style=\"border-spacing:0px;border-collapse:collapse\">";
    				rds = rds + "	<tr>";
    				rds = rds + "		<td>";
    				rds = rds + "			<img src=\"" + data.frames_ja[first_index].url + "\" style=\"width:213px;height:120px\">";
    				rds = rds + "		</td>";
    				rds = rds + "		<td>";
    				rds = rds + "			<img src=\"" + data.frames_ja[second_index].url + "\" style=\"width:213px;height:120px\">";
    				rds = rds + "		</td>";
    				rds = rds + "	</tr>";
    				rds = rds + "	<tr>";
    				rds = rds + "		<td>";
    				rds = rds + "			<img src=\"" + data.frames_ja[third_index].url + "\" style=\"width:213px;height:120px\">";
    				rds = rds + "		</td>";
    				rds = rds + "		<td>";
    				rds = rds + "			<img src=\"" + data.frames_ja[fourth_index].url + "\" style=\"width:213px;height:120px\">";
    				rds = rds + "		</td>";
    				rds = rds + "	</tr>";
        			rds = rds + "</table>";
    				
    				// 9
    				
    				rds = rds + "<br><table style=\"border-spacing:0px;border-collapse:collapse\">";
        			for(var x = 0; x < data.frames_ja.length && x < 18; x = x + 2)
        			{	
        				if(((x + 6) % 6) == 0)
        					rds = rds + "	<tr>";
        				rds = rds + "		<td>";
        				rds = rds + "			<img src=\"" + data.frames_ja[x].url + "\" style=\"width:142px;height:80px\">";
        				rds = rds + "		</td>";
        				if(((x + 1) % 6) == 0)
        					rds = rds + "	</tr>";
        			}
        			if(((x + 1) % 6) != 0)
    					rds = rds + "	</tr>";
        			rds = rds + "</table>";
    				
    				
    				// 16
    				rds = rds + "<br><table style=\"border-spacing:0px;border-collapse:collapse\">";
        			for(var x = 0; x < data.frames_ja.length && x < 16; x++)
        			{	
        				if(((x + 4) % 4) == 0)
        					rds = rds + "	<tr>";
        				rds = rds + "		<td>";
        				rds = rds + "			<img src=\"" + data.frames_ja[x].url + "\" style=\"width:107px;height:60px\">";
        				rds = rds + "		</td>";
        				if(((x + 1) % 4) == 0)
        					rds = rds + "	</tr>";
        			}
        			if(((x + 1) % 4) != 0)
    					rds = rds + "	</tr>";
        			rds = rds + "</table>";
        			
        			rds = rds + "</div>";*/
        		}
        		
        		// alert or not, display frame
        		frame_to_display = data.frame_jo;
        		rds = rds + "<img src=\"" + frame_to_display.url + "\" style=\"width:250px;height:141px\">";
        		
				rds = rds + "<br>" + frame_to_display.image_name;
				if(frame_to_display.designation)
					rds = rds + "<br>des: " + frame_to_display.designation;
				if(frame_to_display.designation_homogeneity)
					rds = rds + "<br>des homogeneity: " + frame_to_display.designation_homogeneity;
				if(frame_to_display.designation_moving_average)
					rds = rds + "<br>des ma: " + frame_to_display.designation_moving_average;
				//rds = rds + "<br>avg4des:"+ data.frames_ja[x].reporters[designation].score_avg;
				//rds = rds + "<br>homogeneity:" + reporter_homogeneity;
				//rds = rds + "<br>threshold:" + (reporter_homogeneity * $('#function2_singlemodifier_input').val());
				//rds = rds + "<br>closest_desg:" + data.frames_ja[x].closest_designation;
				//rds = rds + "<br>closest_avg:" + data.frames_ja[x].closest_avg;
				//rds = rds + "<br>closest_delta:" + (data.frames_ja[x].score_average - data.frames_ja[x].closest_avg);
				data.frame_jo = "suppressed for readability";
				var stringified_data = JSON.stringify(data);
				var regex = new RegExp(',', 'g');
				stringified_data = stringified_data.replace(regex, ',<br>');
				rds = rds + "<br>frame processing response:" + stringified_data;
				rds = rds + "</div>";
        		$("#results_div").append(rds);
        		//alert('f2d=' + JSON.stringify(frame_to_display));
        		//return frame_to_display;
        	}
        }
        ,
        error: function (XMLHttpRequest, textStatus, errorThrown) {
        	$("#message_div").html("ajax error");
            console.log(textStatus, errorThrown);
        }
	});
	//alert("reached end of simulateNewFrame()... shouldn't happen");
	return frame_to_display;
}
	
function graphFrameScoresAndMovingAverages(frames_ja, designation, reporter_homogeneity, singlemodifier, mamodifier, maw_int)
{
	$("#chart1").html("");
	var scores = []; 
	var moving_avg = [];
	var ma = 0;
	var sum = 0; 
	var num = 0;
	var ts = 0;
	for(var x = 0; x < frames_ja.length; x++)
	{
		scores.push(frames_ja[x].reporters[designation].score_avg);
		
		sum = 0;
		num = 0;
		// loop through all the frames, looking for timestamps in the moving average window in the past
		ts = frames_ja[x].timestamp_in_ms;
		for(var y = 0; y < frames_ja.length; y++) 
		{
			// if the timestamp of this frame is within the moving average window x seconds in the past, then add this designation's score
			// to a running total.
			if(frames_ja[y].timestamp_in_ms > (ts - (maw_int * 1000)) && frames_ja[y].timestamp_in_ms <= ts)
			{
				sum = sum + frames_ja[y].reporters[designation].score_avg;
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
    			            y: (reporter_homogeneity * singlemodifier),
    			            lineWidth: 3,
    			            color: 'rgb(100, 55, 124)',
    			            shadow: true,
    			            lineCap: 'butt',
    			            xOffset: 0
    			          }},  
	          {dashedHorizontalLine: {
	            name: 'bam-bam',
	            y: (reporter_homogeneity * mamodifier),
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


