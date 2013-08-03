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
	
	docCookies.setItem("twitter_handle", "huzontv", 31536e3);
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
		        			//$("#message_div").html("Multiple stations as admin. Using administering_station cookie.");
		        			station = docCookies.getItem("administering_station");
		        		}	
	        			else
	        				station = data.user_jo.stations_as_admin_ja[0];
	        			
	        			
	        			getStationInformation(twitter_handle, twitter_access_token, station);
    	        		
	        			var d = new Date();
	        			var end = d.getTime();
	        			var endstring = end + "";
	        			var begin = end - (86400000*14); // 14 days
	        			var beginstring = begin + "";
	        			graphFiredAlertStatistics(beginstring, endstring, twitter_handle, twitter_access_token, station);
	        			
	        			
    	        		getActiveReporterDesignations(twitter_handle, twitter_access_token, station);
	        			
    	        		var d = new Date();
	        			var end = d.getTime();
	        			var endstring = end + "";
	        			var begin = end - (86400000*3); // 3 days
	        			var beginstring = begin + "";
    	        		getFiredAlerts(beginstring, endstring, twitter_handle, twitter_access_token, station);
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
	    		general_string = general_string + "<b>station:</b> " + data.station_jo.call_letters;
	    		general_string = general_string + "<br><b>city:</b> " + data.station_jo.city;
	    		general_string = general_string + "<br><b>state:</b> " + data.station_jo.state;
	    		general_string = general_string + "<br><b>2013 dma:</b> " + data.station_jo.dma2013;
	    		general_string = general_string + "<br><b>ma window:</b> " + data.station_jo.maw;
	    		general_string = general_string + "<br><b>ma modifier:</b> " + data.station_jo.mamodifier;
	    		general_string = general_string + "<br><b>delta:</b> " + data.station_jo.delta;
	    		general_string = general_string + "<br><b>frame rate:</b> " + data.station_jo.frame_rate;
	    		general_string = general_string + "<br><b>alert mode:</b>";
	    		general_string = general_string + "<select id=\"alert_mode_select\">";
	    		general_string = general_string + "	<option SELECTED id=\"alert_mode_live_option\" value=\"live\">live</option>";
	    		general_string = general_string + "	<option id=\"alert_mode_test_option\" value=\"test\">test</option>";
	    		general_string = general_string + "	<option id=\"alert_mode_silent_option\" value=\"silent\">silent</option>";
	    		general_string = general_string + "</select> <span id=\"alert_mode_change_span\"></span>";
	    		$("#general_div").html(general_string);
	    		if(data.station_jo.alert_mode === "live")
	    			$("#alert_mode_live_option").prop('selected', true);
	    		else if(data.station_jo.alert_mode === "test")
	    			$("#alert_mode_test_option").prop('selected', true);
	    		else if(data.station_jo.alert_mode === "silent")
	    			$("#alert_mode_silent_option").prop('selected', true);
	    		$("#alert_mode_select").change(function () {
					$.ajax({
						type: 'GET',
						url: endpoint,
						data: {
				            method: "setAlertMode",
				            alert_mode: $("#alert_mode_select").val(), 
				            station: station,
					        twitter_handle: twitter_handle,
					        twitter_access_token: twitter_access_token
				        },
				        dataType: 'json',
				        async: true,
				        success: function (data, status) {
				        	if (data.response_status === "error")
				        	{
				        		$("#alert_mode_change_span").css("color", "red");
				        		$("#alert_mode_change_span").html("error");
				        	}
				        	else
				        	{
				        		$("#alert_mode_change_span").css("color", "green");
				        		$("#alert_mode_change_span").html("updated");
				        	}
				        	var int=self.setInterval(function(){$("#alert_mode_change_span").html("");},5000); // clear it out after 5 seconds
				        }
				        ,
				        error: function (XMLHttpRequest, textStatus, errorThrown) {
				        	$("#alert_mode_change_span").html("ajax error");
				        	$("#alert_mode_change_span").css("color", "red");
				            console.log(textStatus, errorThrown);
				        }
					});
            	});	
	    	}
	    }
	    ,
	    error: function (XMLHttpRequest, textStatus, errorThrown) {
	    	$("#results_div").html("ajax error");
	        console.log(textStatus, errorThrown);
	    }
	});
}

function graphFiredAlertStatistics(beginstring, endstring, twitter_handle, twitter_access_token, station)
{
	var alert_stats_string = "";
	$("#graph_div").html("Loading fired alert statistics <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
	        method: "getFiredAlertStatistics",
	        station: station,
	        begin: beginstring,
	        end: endstring,
	        self_posted_only: true,
	        twitter_handle: twitter_handle,
	        twitter_access_token: twitter_access_token
		},
	    dataType: 'json',
	    async: true,
	    success: function (data, status) {
	    	if (data.response_status == "error")
	    	{
	    		alert_stats_string = "<div style=\"font-size:16;color:red\">getFiredAlertStatistics error: " + data.message + "</div>";
	    		$("#graph_div").html(alert_stats_string);
	    	}
	    	else
	    	{
	    			var fired_alert_counts = []; 
	    			//var unabridged_redirect_counts = [];
	    			var sansbot_redirect_counts = [];
	    			// looping through all frames gathered to graph
	    			var max_redirect_count = 0;
	    			var xticks = [];
	    			var days = [];
	    			for(var x = 0; x < data.fired_alerts_ja.length; x++)
	    			{
	    				days.push((x+1)+"");
	    				xticks.push(data.fired_alerts_ja[x].day+"");
	    				fired_alert_counts.push(data.fired_alerts_ja[x].fired_alert_count);
	    				sansbot_redirect_counts.push(data.fired_alerts_ja[x].sansbot_redirect_count);
	    				//fired_alert_counts.push([data.fired_alerts_ja[x].day, data.fired_alerts_ja[x].fired_alert_count]);
	    				//sansbot_redirect_counts.push([data.fired_alerts_ja[x].day, data.fired_alerts_ja[x].sansbot_redirect_count]);
	    				//unabridged_redirect_counts.push([data.fired_alerts_ja[x].day, data.fired_alerts_ja[x].unabridged_redirect_count]);
	    				if(data.fired_alerts_ja[x].sansbot_redirect_count > max_redirect_count)
	    					max_redirect_count = data.fired_alerts_ja[x].sansbot_redirect_count;
	    			}
	    			
	    			var plot1 = $.jqplot ('chart1', [fired_alert_counts, sansbot_redirect_counts],{
	    				title: 'Fired Alerts ' + xticks[0] + " - " + xticks[xticks.length-1],
	    				//series:[{showMarker:false}],
	    				axes: {
	    					xaxis:{
	    						ticks: days,
	  	    		            label:'Days',
	  	    		            labelRenderer: $.jqplot.CanvasAxisLabelRenderer
	  	    		        },
	    					yaxis: {
	    						label:'# alerts and clicks',
	    						labelRenderer: $.jqplot.CanvasAxisLabelRenderer,
	    			            min:0,max: (max_redirect_count + 20) // spacer
	    			        }
	    				}
	    				,
	    				canvasOverlay: {
	    					show: true
	    			        /*objects: [
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
	    			        ]*/
	    			      }
	    			    });
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
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"font-weight:bold\">";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td colspan=8 style=\"background-color:#CCFFFF;text-align:center;font-weight:bold\">";
				reporters_string = reporters_string + "			Twitter settings";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td colspan=6 style=\"background-color:#A9CAEB;text-align:center;font-weight:bold\">";
				reporters_string = reporters_string + "			Facebook settings";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "	</tr>";
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
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			handle";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			link";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			on";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			CD";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			followers";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			30d";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			clx 30d";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#CCFFFF;font-weight:bold\">";
				reporters_string = reporters_string + "			clx/alrt";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			link";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			pg link";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			on";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			CD";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			likes";
				reporters_string = reporters_string + "		</td>";
				reporters_string = reporters_string + "		<td style=\"background-color:#A9CAEB;font-weight:bold\">";
				reporters_string = reporters_string + "			30d";
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
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_linked_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_active_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_cooldown_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_followers_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_alert_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_click_count_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
					reporters_string = reporters_string + "		<td id=\"" + reporters_ja[x] + "_twitter_click_rate_td\"><img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\"></td>";
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
            alert_history_in_hours: 720, // one month (-1 for none)
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
        		
        		var sansbot_clicks = 0;
        		for(var a = 0; a < data.user_jo.twitter_alert_history_ja.length; a++)
        		{	
        			sansbot_clicks = sansbot_clicks + data.user_jo.twitter_alert_history_ja[a].sansbot_redirect_count;
        		}
        		$("#" + designation + "_twitter_click_count_td").html(sansbot_clicks);
        		var click_rate = 0;
        		if(data.user_jo.twitter_alert_history_ja.length != 0 )
        		{
        			click_rate = sansbot_clicks / data.user_jo.twitter_alert_history_ja.length;
        			click_rate = click_rate * 10;
        			click_rate = Math.floor(click_rate);
        			click_rate = click_rate/10;
        		}
        		$("#" + designation + "_twitter_click_rate_td").html(click_rate);
        		
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
        		if(typeof data.user_jo.facebook_page_jo === undefined || data.user_jo.facebook_page_jo == null || data.user_jo.facebook_page_jo.error != null)
        			facebook_page_linked = "<span style=\"color:red\">no</span>";
				else
				{
					//alert('ispub=' + data.user_jo.facebook_page_jo.is_published);
					//alert(JSON.stringify(data.user_jo.facebook_page_jo)); 
					if(typeof data.user_jo.facebook_page_jo.is_published === undefined || data.user_jo.facebook_page_jo.is_published == null || data.user_jo.facebook_page_jo.is_published === "false")
						facebook_page_linked = "<span style=\"color:green\">yes <a href=\"#\" onclick=\"alert('Page is not published!');return false;\" style=\"color:red;font-weight:bold\">NP!</a></span>";
					else
						facebook_page_linked = "<span style=\"color:green\">yes</span>";
				}
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

function getFiredAlerts(beginstring, endstring, twitter_handle, twitter_access_token, station)
{
	$("#alerts_div").html("Loading recent alerts... <img src=\"images/progress_16x16.gif\" style=\"width:16px;height:16px\">");
	$.ajax({
		type: 'GET',
		url: endpoint,
		data: {
            method: "getFiredAlerts",
            begin: beginstring,
            end: endstring,
            station: station,
            self_posted_only: false,
            twitter_handle: twitter_handle,
            twitter_access_token: twitter_access_token
		},
        dataType: 'json',
        async: true,
        success: function (data, status) {
        	if (data.response_status == "error")
        	{
        		$("#message_div").html("<span style=\"font-size:16;color:red\">gS Error: " + data.message + "</span>");
        	}
        	else // getMostRecentAlerts was successful, meaning twitter_handle and twitter_access_token were OK
        	{
        		var alerts_ja = data.fired_alerts_ja;
        		var mds = "";
        		mds = mds + "<table style=\"margin-left:auto;margin-right:auto;border-spacing:10px\">";
        		for(var x = 0; x < alerts_ja.length; x++)
        		{	
        			if(x%2 == 0)
        				mds = mds + "	<tr>";
        			mds = mds + "		<td style=\"vertical-align:middle;text-align:center;font-size:10px\">";
        			//mds = mds + JSON.stringify(alerts_ja[x]);
	        		if(alerts_ja[x].social_type === "twitter")
	        		{
	        			if(typeof alerts_ja[x].tweet_jo === "undefined")
	        				mds = mds + "This tweet doesn't exist or has been deleted.";
	        			else if(alerts_ja[x].tweet_jo == null)
	        				mds = mds + "tweet_jo == null";
	        			else if(typeof alerts_ja[x].tweet_jo.entities === "undefined")
	        				mds = mds + "tweet_jo.entities is undefined";
	        			else if(alerts_ja[x].tweet_jo.entities == null)
	        				mds = mds + "tweet_jo.entities == null";
	        			else if(typeof alerts_ja[x].tweet_jo.entities.media[0] === "undefined")
	        				mds = mds + "tweet_jo.entities.media[0] is undefined";
	        			else if(alerts_ja[x].tweet_jo.entities.media[0] == null)
	        				mds = mds + "tweet_jo.entities.media[0] == null";
	        			else if(typeof alerts_ja[x].tweet_jo.entities.media[0].media_url === "undefined")
	        				mds = mds + "tweet_jo.entities.media[0].media_url is undefined";
	        			else if(alerts_ja[x].tweet_jo.entities.media[0].media_url == null)
	        				mds = mds + "tweet_jo.entities.media[0].media_url == null";
	        			else
	        				mds = mds + "			<img src=\"" + alerts_ja[x].tweet_jo.entities.media[0].media_url + "\" style=\"width:300px;height:169px\">";
	        		}
	        		else if(alerts_ja[x].social_type === "facebook")
	        		{
	        			if(typeof alerts_ja[x].fbpost_jo === "undefined")
	        				mds = mds + "This fbpost doesn't exist or has been deleted.";
	        			else if(alerts_ja[x].fbpost_jo == null)
	        				mds = mds + "fbpost_jo == null";
	        			else if(typeof alerts_ja[x].fbpost_jo.source === "undefined")
	        				mds = mds + "This fbpost doesn't exist or has been deleted.";
	        			else if(alerts_ja[x].fbpost_jo.source == null)
	        				mds = mds + "fbpost_jo.source == null";
	        			else
	        			{
	        				mds = mds + "<img src=\"" + alerts_ja[x].fbpost_jo.source + "\" style=\"width:300px;height:169px\">";
	        			}
	        		}
	        		else
	        		{
	        			mds = mds + "			<img src=\"" + alerts_ja[x].image_url + "\" style=\"width:300px;height:169px\">";
	        		}
	        		mds = mds + "		</td>";
	        		mds = mds + "		<td style=\"vertical-align:middle;text-align:center;font-size:14px\">";
	        		mds = mds + "			" + alerts_ja[x].local_timestamp_hr;
	        		mds = mds + "			<br><b>id:</b> " +  alerts_ja[x].designation;
	        		mds = mds + "			<br><b>social type:</b> " + alerts_ja[x].social_type;
	        		mds = mds + "			<br><b>posted acct:</b> " + alerts_ja[x].created_by;
	        		mds = mds + "			<br><b>human clicks:</b> " + alerts_ja[x].sansbot_redirect_count;
	        		mds = mds + "			<br><b>all clicks (+bots):</b> " +  alerts_ja[x].unabridged_redirect_count;
	        		mds = mds + "			<br><a href=\"#\" id=\"delete_link_" + x + "\">DELETE</a>";
	        		mds = mds + "		</td>";
	        		if(x%2 == 1)
        				mds = mds + "	</tr>";
        		}
        		mds = mds + "</table>";
        		$("#alerts_div").html(mds);
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
	        				        			getFiredAlerts(beginstring, endstring, twitter_handle, twitter_access_token, station);
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


