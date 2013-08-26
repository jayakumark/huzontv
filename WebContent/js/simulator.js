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
		
	//docCookies.setItem("twitter_access_token", "", 3000000);
	//docCookies.setItem("twitter_handle", "huzontv", 3000000);
	//docCookies.setItem("pass", "", 3000000);
	var administering_station = docCookies.getItem("administering_station");
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
		/*var administering_station = docCookies.getItem("administering_station");
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
		        						docCookies.setItem("administering_station", event.data.value, 3000000);
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
		{*/
		
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
	        		/*var mds = "";
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
	        						docCookies.setItem("administering_station", event.data.value, 3000000);
	        						window.location.reload();
	        					});
	        		}*/
	        		
	        		var station_select_str = "";
	        		station_select_str = station_select_str + "<select id=\"station_select\">";
	        		for(var x = 0; x < data.stations_ja.length; x++)
	        		{	
	        			station_select_str = station_select_str + "	<option id=\"station_select_" + data.stations_ja[x].call_letters + "\" value=\"" + data.stations_ja[x].call_letters + "\">" + data.stations_ja[x].call_letters + "</option>";
	        		}
	        		station_select_str = station_select_str + "</select> <span id=\"station_select_change_span\"></span>";
	        		$("#station_select_td").html(station_select_str);
	        		
	        		if(administering_station == null)
	        		{
	        			administering_station = "wkyt";
	        			docCookies.setItem("administering_station", "wkyt");
	        		}
	        		
	        		$("#station_select_" + administering_station).prop('selected', true);
		    		$("#station_select").change(function () {
		    			docCookies.setItem("administering_station", $("#station_select").val(), 30000000);
		    			$("#station_select_change_span").html("reloading page");
		    			window.location.reload();
	            	});	
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
		
			var mds = "";
    	//	mds = mds + "<div style=\"font-size:16;color:green\">You are administering " + administering_station + "</div>";
    		
    		$.ajax({
    			type: 'GET',
    			url: endpoint,
    			data: {
    	            method: "getActiveReporterDesignations",
    	            station: administering_station,
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
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=4>";
	fds = fds + "						<b>Function 1:</b> Get all still frames in a range (inclusive).";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function1_begin_input\" size=13 value=\"20130707_040000\"> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function1_end_input\" value=\"20130707_235900\" size=13> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Show every <input type=\"text\" id=\"function1_increment_input\" value=\"5\" size=1 maxlength=2> frames";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function2_begin_input\" size=13 value=\"20130707_040000\"> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function2_end_input\" value=\"20130707_235900\" size=13> ";
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
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						MA Thresh Modifier: <input type=\"text\" id=\"function3_mamodifier_input\" value=\".8\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Moving Avg Window: <input type=\"text\" id=\"function3_mawindow_input\" value=\"5\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function3_begin_input\" size=13 value=\"20130707_040000\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function3_end_input\" value=\"20130707_235900\" size=13>";
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
	fds = fds + "						MA Thresh Modifier: <input type=\"text\" id=\"function4_mamodifier_input\" value=\".8\" size=4>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						MAW: <input type=\"text\" id=\"function4_maw_input\" value=\"6\" size=3><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						NRPST: <input type=\"text\" id=\"function4_nrpst_input\" value=\"2\" size=1>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Delta: <input type=\"text\" id=\"function4_delta_input\" value=\"0\" size=4> ";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Begin: <input type=\"text\" id=\"function4_begin_input\" size=13 value=\"20130709_040000\"><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function4_end_input\" value=\"20130709_235900\" size=13><br>";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						CD: <input type=\"text\" id=\"function4_awp_input\" value=\"3600\" size=4> ";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						Interval: <input type=\"text\" id=\"function4_grouping_input\" value=\"3600\" size=5> ";
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
	fds = fds + "						Begin: <input type=\"text\" id=\"function5_begin_input\" size=13 value=\"20130707_040000\">";
	fds = fds + "					</td>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left\">";
	fds = fds + "						End: <input type=\"text\" id=\"function5_end_input\" value=\"20130707_235900\" size=13>";
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
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=1>";
	fds = fds + "						<b>Function 6:</b> Test reporter tokens";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:center\">";
	fds = fds + "   					<input id=\"function6_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "	</tr>";
	fds = fds + "	<tr>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=1>";
	fds = fds + "						<b>Function 7:</b> Reset PRODUCTION alert timers";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:center\">";
	fds = fds + "   					<input id=\"function7_go_button\" type=button value=\"GO\">";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "			</table>";
	fds = fds + "		</td>";
	fds = fds + "		<td style=\"vertical-align:top;text-align:left\">";
	fds = fds + "			<table style=\"border-spacing:3px\">";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:left;font-size:15px\" colspan=1>";
	fds = fds + "						<b>Function 8:</b> Reset TEST alert timers";
	fds = fds + "					</td>";
	fds = fds + "				</tr>";
	fds = fds + "				<tr>";
	fds = fds + "					<td style=\"vertical-align:middle;text-align:center\">";
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
				$("#results_div").html("Loading frames...");
				$("#chart1").html("");
				var rds = "";
				/*var datestring = $('#function1_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin = d.getTime()/1000;
				datestring = $('#function1_end_input').val();
			    d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end = d.getTime()/1000;*/
				
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "getFrames",
				            begin: $('#function1_begin_input').val(),             
				            end: $('#function1_end_input').val(),
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
				        			var increment = $("#function1_increment_input").val() * 1;
				        			rds = rds + "<div>Showing " + data.frames_ja.length + " results</div>";
				        			for(var x = 0; x < data.frames_ja.length; x = x + increment)
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
				        					rds = rds + "<br>avg4des:"+ data.frames_ja[x].reporters[designation].score;
				        					rds = rds + "<br>homogeneity:" + reporter_homogeneity;
				        					rds = rds + "<br>threshold:" + (reporter_homogeneity * $('#function2_singlemodifier_input').val());
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
				var designation = $('#function3_designation_select').val();
				var reporter_homogeneity = 0;
				var maw_int = $('#function3_mawindow_input').val();
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
				            begin: $('#function3_begin_input').val(),             
				            end: $('#function3_end_input').val(),
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
				        			graphFrameScoresAndMovingAverages(data.frames_ja, designation, reporter_homogeneity, mamodifier, maw_int);
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
				$("#results_div").html(rds);
				$("#results_div").html("");
				$("#chart1").html("");
				
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
				
				var datestring = $('#function4_begin_input').val();
				var d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var begin_in_ms = d.getTime();
				datestring = $('#function4_end_input').val();
				d = new Date(datestring.substring(0,4), (datestring.substring(4,6) - 1), datestring.substring(6,8), datestring.substring(9,11), datestring.substring(11,13), datestring.substring(13,15), 0);
				var end_in_ms = d.getTime();
				var grouping_interval = $('#function4_grouping_input').val() * 1000;
				var num_rows = Math.ceil((end_in_ms - begin_in_ms) / grouping_interval);			

				var rds = "";
				rds = rds + "<div id=\"gAF_results_count\"></div>";
				rds = rds + "<table style=\"width:100%\">";
				for(var x = 0; x < num_rows; x++)
				{
					
					rds = rds + "	<tr>";
					rds = rds + "		<td style=\"width:150px;background-color:#dddddd;font-size:16px;font-weight:bold;vertical-align:middle\">Hour " + x + "</td>";
					rds = rds + "		</td>";
					rds = rds + "		<td id=\"gAF_results_td_" + x + "\" style=\"background-color:#dddddd\"></td>";
					rds = rds + "		</td>";
					rds = rds + "	</tr>";
				}	
				rds = rds + "</table>";
				$("#results_div").html(rds);
				for(var x = 0; x < num_rows; x++)
				{
					getAlertFramesClosure((begin_in_ms + (x * grouping_interval)),  //begin
							(begin_in_ms + (x * grouping_interval) + grouping_interval), // end
							$('#function4_mamodifier_input').val(), 
							$('#function4_nrpst_input').val(), 
							$('#function4_awp_input').val(),
							$('#function4_delta_input').val(),
							$('#function4_maw_input').val(),
							station, twitter_handle, twitter_access_token, 
							"gAF_results_td_" + x // row_id -- tells the ajax function which ID to paint when returning
					);
					
				}	
				
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
				rds = rds + "<table style=\"width:100%\">";
				rds = rds + "	<tr>";
				rds = rds + "		<td>";
				rds = rds + "			Reporter";
				rds = rds + "		</td>";
				rds = rds + "		<td>";
				rds = rds + "			Twitter token";
				rds = rds + "		</td>";
				rds = rds + "		<td>";
				rds = rds + "			TL FB token";
				rds = rds + "		</td>";
				rds = rds + "		<td>";
				rds = rds + "			Page FB token";
				rds = rds + "		</td>";
				rds = rds + "	</tr>";
				
				for(var x=0; x < reporters_ja.length; x++)
				{
					rds = rds + "	<tr>";
					rds = rds + "		<td>";
					rds = rds + "			" + reporters_ja[x];
					rds = rds + "		</td>";
					rds = rds + "		<td id=\"" + reporters_ja[x] + "_twitter_valid_td\">";
					rds = rds + "		</td>";
					rds = rds + "		<td id=\"" + reporters_ja[x] + "_fb_valid_td\">";
					rds = rds + "			";
					rds = rds + "		</td>";
					rds = rds + "		<td id=\"" + reporters_ja[x] + "_fbpage_valid_td\">";
					rds = rds + "			";
					rds = rds + "		</td>";
					rds = rds + "	</tr>";
				}
				rds = rds + "</table>";
				$("#results_div").html(rds);
				for(var x=0; x < reporters_ja.length; x++)
				{
					verifyTwitterCredentials(reporters_ja[x]);
					verifyTopLevelFBCredentials(reporters_ja[x]);
					verifyPageFBCredentials(reporters_ja[x]);
				}
				return;
			});
	
	$("#function7_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
				$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "resetProductionAlertTimers",
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
	
	$("#function8_go_button").click(
			function () {
				$("#results_div").html("");
				$("#chart1").html("");
				var rds = "";
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
        	if (data.response_status == "error")
        		$("#" + designation + "_twitter_valid_td").html(data.message);
        	else if(data.response_status == "success")
        	{
        		if(data.valid == true)
        			$("#" + designation + "_twitter_valid_td").html("<span style=\"color:green\">VALID</span>");
        		else
        			$("#" + designation + "_twitter_valid_td").html("<span style=\"color:red\">NOT VALID</span>");
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
}


	
function graphFrameScoresAndMovingAverages(frames_ja, designation, reporter_homogeneity, mamodifier, maw_int)
{
	$("#chart1").html("");
	var scores = []; 
	var ma6s = [];
	var ma = 0;
	var sum = 0; 
	var num = 0;
	var ts = 0;
	// looping through all frames gathered to graph
	for(var x = 0; x < frames_ja.length; x++)
	{
		scores.push(frames_ja[x].reporters[designation].score);
		ma6s.push(frames_ja[x].reporters[designation].ma6);
	}
	var plot1 = $.jqplot ('chart1', [scores, ma6s],{
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
    			            y: (reporter_homogeneity),
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

function getAlertFramesClosure(begin_in_ms, end_in_ms, mamodifier, nrpst, awp, delta, maw, station, twitter_handle, twitter_access_token, row_id)
{
	//alert("calling getAlertFrames with begin=" + begin_in_ms + " end=" + end_in_ms);
	$.ajax({
		type: 'GET',
		url: endpoint,
		timeout: 600000,
		data: {
            method: "getAlertFrames",
            begin: begin_in_ms,             
            end: end_in_ms,
            mamodifier: mamodifier,
            nrpst: nrpst, // number required past single threshold
            awp:  awp,
            delta: delta,
            maw: maw,
            station: station,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: false,
        success: function (data, status) {
        	//alert("returning to paint " + row_id);
        	if (data.response_status == "error")
        	{
        		$("#results_div").html("error message=" + data.message);
        	}
        	else
        	{
        		var rowstring = "";
        		if(data.alert_frames_ja && data.alert_frames_ja.length > 0)
    			{	
        			data.alert_frames_ja.sort(function(a,b){
        				a = a.timestamp_in_ms;
        				b = b.timestamp_in_ms;
        				return a - b;
        			});
        			
    				for(var x = 0; x < data.alert_frames_ja.length; x++)
    				{
    					/*
    					 * 			
    					 * 			return_jo.put("image_name", getImageName());
    					 * 			return_jo.put("designation", highest_ma_designation);
									return_jo.put("single_threshold", homogeneity);
									return_jo.put("ma_threshold", homogeneity * ma_modifier_double);
									return_jo.put("trigger_timestamp_in_ms", trigger_timestamp_in_ms);
									return_jo.put("trigger_score", trigger_score);
									return_jo.put("trigger_maw_int", trigger_maw_int);
									return_jo.put("trigger_ma5", trigger_ma5);
									return_jo.put("trigger_ma6", trigger_ma6);
									return_jo.put("trigger_numframes", trigger_numframes);
									return_jo.put("trigger_delta", trigger_delta);
									return_jo.put("trigger_npst", trigger_npst);
									return_jo.put("second_highest_designation", second_highest_ma_designation);
									return_jo.put("second_highest_ma", second_highest_ma);
									return_jo.put("second_highest_score", second_highest_score); // FIXME this isn't necessarily the score of the second_highest_ma_designation
    					 */
    					rowstring = rowstring + "<div style=\"border: 1px black solid;width:250px;display:inline-block;\">";
        				rowstring = rowstring + "<img src=\"" + data.alert_frames_ja[x].url + "\" style=\"width:250px;height:141px\">";
        				rowstring = rowstring + "<br>image_name:" + data.alert_frames_ja[x].image_name;
        				rowstring = rowstring + "<br>designation:" + data.alert_frames_ja[x].designation;
        				rowstring = rowstring + "<br>single_threshold:" + data.alert_frames_ja[x].single_threshold;
        				rowstring = rowstring + "<br>ma_threshold:" + data.alert_frames_ja[x].ma_threshold;
        				rowstring = rowstring + "<br>trigger_timestamp_in_ms:" + data.alert_frames_ja[x].trigger_timestamp_in_ms;
        				rowstring = rowstring + "<br>trigger_score:" + data.alert_frames_ja[x].trigger_score;
        				rowstring = rowstring + "<br>trigger_maw_int:" + data.alert_frames_ja[x].trigger_maw_int;
        				rowstring = rowstring + "<br>trigger_ma5:" + data.alert_frames_ja[x].trigger_ma5;
        				rowstring = rowstring + "<br>trigger_ma6:" + data.alert_frames_ja[x].trigger_ma6;
        				rowstring = rowstring + "<br>trigger_numframes:" + data.alert_frames_ja[x].trigger_numframes;
        				rowstring = rowstring + "<br>trigger_delta:" + data.alert_frames_ja[x].trigger_delta;
        				rowstring = rowstring + "<br>trigger_npst:" + data.alert_frames_ja[x].trigger_npst;
        				rowstring = rowstring + "<br>2nd highest des:" + data.alert_frames_ja[x].second_highest_designation;
        				rowstring = rowstring + "<br>2nd highest ma:" + data.alert_frames_ja[x].second_highest_ma;
        				rowstring = rowstring + "<br>2nd highest score:" + data.alert_frames_ja[x].second_highest_score;
        				rowstring = rowstring + "</div>";
    				}
    				
    				$("#" + row_id).html(rowstring);
    			
    			/*	for(var x = 0; x < data.alert_frames_ja.length; x++)
    				{
    					$("#" + data.alert_frames_ja[x].timestamp_in_ms + "_graphthis_link").click({value: x},
    							function (event) {
    								$("#function3_designation_select").val(data.alert_frames_ja[event.data.value].designation);
    								$("#function3_mamodifier_input").val($('#function4_mamodifier_input').val());
    								$("#function3_begin_input").val(data.alert_frames_ja[event.data.value].timestamp_in_ms - 120000);
    								$("#function3_end_input").val(data.alert_frames_ja[event.data.value].timestamp_in_ms + 120000);
    								$("#function3_go_button").trigger("click");
    							});
    				}*/
    			}
        		else
        		{
        			$("#" + row_id).html("No alerts for this interval.");
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

