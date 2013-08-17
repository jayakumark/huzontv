package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Station implements java.lang.Comparable<Station> {

	/**
	 * @param args
	 */
	
	private String call_letters;
	private String city;
	private String state;
	private int dma_2013;
	private TreeSet<String> administrators;
	private TreeSet<String> reporters;
	//private boolean active;
	private boolean valid;
	private int frame_rate; // milliseconds per frame
	private String s3_bucket_public_hostname;
	private String livestream_url;
	private String livestream_url_alias;
	private int nrpst;
	private int maw;
	private double mamodifier;
	private double delta;
	private String homepage_url;
	private String clips_url;
	private String recent_newscasts_url;
	private String iphone_app_url;
	private String android_app_url;
	private String logo_filename;
	private String short_display_name; // like "Local 12", "WKYT" or "LEX 18", whatever they are called
	private DataSource datasource;
	private String java_timezone_string;
	private String master_designation;
	private boolean twitter_active_individual;
	private boolean twitter_active_master;
	private boolean facebook_active_individual;
	private boolean facebook_active_master;
	private String private_ip;
	private String public_ip;
	private int ssh_port;
	private int web_port; 
	
	public Station(String inc_call_letters)
	{
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM stations WHERE call_letters='" + inc_call_letters + "'");
			if(rs.next())
			{
				call_letters = rs.getString("call_letters");
				city = rs.getString("city");
				state = rs.getString("state");
				dma_2013 = rs.getInt("dma_2013");
				frame_rate = rs.getInt("frame_rate");
				livestream_url = rs.getString("livestream_url");
				livestream_url_alias = rs.getString("livestream_url_alias");
				
				logo_filename = rs.getString("logo_filename");
				homepage_url = rs.getString("homepage_url");
				clips_url = rs.getString("clips_url");
				recent_newscasts_url = rs.getString("recent_newscasts_url");
				iphone_app_url = rs.getString("iphone_app_url");
				android_app_url = rs.getString("android_app_url");
				short_display_name = rs.getString("short_display_name");
				java_timezone_string = rs.getString("java_timezone_string");
				master_designation = rs.getString("master_designation");
				twitter_active_individual = rs.getBoolean("twitter_active_individual");
				twitter_active_master = rs.getBoolean("twitter_active_master");
				facebook_active_individual = rs.getBoolean("facebook_active_individual");
				facebook_active_master = rs.getBoolean("facebook_active_master");
				
				nrpst = rs.getInt("nrpst");
				maw = rs.getInt("maw");
				mamodifier = rs.getDouble("mamodifier");
				delta = rs.getDouble("delta");
				
				private_ip = rs.getString("private_ip");
				public_ip = rs.getString("public_ip");
				ssh_port = rs.getInt("ssh_port");
				web_port = rs.getInt("web_port");
				
				reporters = new TreeSet<String>();
				StringTokenizer st = new StringTokenizer(rs.getString("reporters")," ");
				while(st.hasMoreTokens())
				{
					reporters.add(st.nextToken()); // = rs.getString("station");
				}
				administrators = new TreeSet<String>();
				st = new StringTokenizer(rs.getString("administrators")," ");
				while(st.hasMoreTokens())
				{
					administrators.add(st.nextToken()); // = rs.getString("station");
				}
				//active = rs.getBoolean("active");
				s3_bucket_public_hostname = rs.getString("s3_bucket_public_hostname");
				valid = true;
			}
			else
				valid = false;
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station constructor: valid set to false message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				(new Platform()).addMessageToLog("SQLException in Station constructor: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		
	}
	
	// these 5 will never be null or empty.
	public String getHomepageURL()
	{
		return homepage_url;
	}
	public String getLogoFilename()
	{
		return logo_filename;
	}
	public String getShortDisplayName()
	{
		return short_display_name;
	}
	public String getJavaTimezoneString()
	{
		return java_timezone_string;
	}
	public String getMasterDesignation()
	{
		return master_designation;
	}
	// end 5 functions note
	
	// these 4 functions will never return null, but value may be empty. Calling function should check.
	public String getRecentNewscastsURL()
	{
		return recent_newscasts_url;
	}
	public String getClipsURL()
	{
		return clips_url;
	}
	public String getiPhoneAppURL()
	{
		return iphone_app_url;
	}
	public String getAndroidAppURL()
	{
		return android_app_url;
	}
	// end 4 functions note
	
	public boolean isTwitterActiveIndividual()
	{
		return twitter_active_individual;
	}

	public boolean isTwitterActiveMaster()
	{
		return twitter_active_master;
	}	
	
	public boolean isFacebookActiveIndividual()
	{
		return facebook_active_individual;
	}

	public boolean isFacebookActiveMaster()
	{
		return facebook_active_master;
	}
	
	public String getPublicIP()
	{
		return public_ip;
	}
	public String getPrivateIP()
	{
		return private_ip;
	}
	public int getSSHPort()
	{
		return ssh_port;
	}
	public int getWebPort()
	{
		return web_port;
	}

	public double getMAModifier()
	{
		return mamodifier;
	}
		
	public int getNRPST() // number required past single threshold
	{
		return nrpst;
	}
	
	public int getMAWindow() // number required past single threshold
	{
		return maw;
	}
	
	public double getDelta() // number required past single threshold
	{
		return delta;
	}
	
	public String getLiveStreamURL()
	{
		return livestream_url;
	}
	
	public String getLiveStreamURLAlias()
	{
		return livestream_url_alias;
	}
	
	public boolean isValid()
	{
		return valid;
	}
	
	public String getCallLetters()
	{
		return call_letters;
	}
	
	public TreeSet<String> getAdministrators()
	{
		return administrators;
	}
	
	public TreeSet<String> getReporterDesignations()
	{
		return reporters;
	}
	
	public JSONArray getReportersAsJSONArray(boolean return_tokens, boolean return_tw_profile, boolean return_fb_profile, boolean return_fb_page, int alert_history_in_hours) 
	{
		JSONArray return_ja = new JSONArray();
		TreeSet<String> localset = reporters;
		Iterator<String> reporter_it = localset.iterator();
		User currentreporter = null;
		while(reporter_it.hasNext())
		{
			currentreporter = new User(reporter_it.next(), "designation");
			return_ja.put(currentreporter.getAsJSONObject(return_tokens, return_tw_profile, return_fb_profile, return_fb_page, alert_history_in_hours));
		}
		return return_ja;
	}

	/*public boolean isActive()
	{
		return active;
	}*/
	
	public String getCity()
	{
		return city;
	}
	
	public String getState()
	{
		return state;
	}
	
	public int getDMA()
	{
		return dma_2013;
	}
	
	public int getFrameRate()
	{
		return frame_rate;
	}
	
	public String getS3BucketPublicHostname()
	{
		return s3_bucket_public_hostname;
	}
	
	// datestring convenience method
	TreeSet<Alert> getFiredAlerts(String beginstring, String endstring, boolean self_posted_only)
	{
		System.out.println("Station.getFiredAlerts(" + beginstring + "," + endstring + ", " + self_posted_only);
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFiredAlerts(beginstring, endstring): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFiredAlerts(beginstring, endstring): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		return getFiredAlerts(begin_in_ms, end_in_ms, self_posted_only);
	}
	
	TreeSet<Alert> getFiredAlerts(long begin_long, long end_long, boolean self_posted_only)
	{
		System.out.println("Station.getFiredAlerts(" + begin_long + "(long)," + end_long + "(long), " + self_posted_only);
		TreeSet<Alert> returnset = new TreeSet<Alert>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			if(self_posted_only)
			{	
				System.out.println("Station.getFiredAlerts(long,long) self_posted_only=true: SELECT * FROM alerts WHERE (`station`='" + getCallLetters() + "' AND `social_item_id`!='' AND `created_by`=`designation` AND " +
					" timestamp_in_ms >= " + begin_long +" AND timestamp_in_ms <= " + end_long + ")");
				rs = stmt.executeQuery("SELECT * FROM alerts WHERE (`station`='" + getCallLetters() + "' AND `social_item_id`!='' AND `created_by`=`designation` AND " +
					" timestamp_in_ms >= " + begin_long + " AND timestamp_in_ms <= " + end_long + ")");
			}
			else
			{
				System.out.println("Station.getFiredAlerts(long,long) self_posted_only=false: SELECT * FROM alerts WHERE (`station`='" + getCallLetters() + "' AND `social_item_id`!='' AND " + " timestamp_in_ms >= " + begin_long + " AND timestamp_in_ms <= " + end_long + ")");
				rs = stmt.executeQuery("SELECT * FROM alerts WHERE (`station`='" + getCallLetters() + "' AND `social_item_id`!='' AND " + " timestamp_in_ms >= " + begin_long + " AND timestamp_in_ms <= " + end_long + ")");
			}
			while(rs.next())
			{
				returnset.add(new Alert(rs.getLong("id")));
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("Station.getFiredAlerts(long,long): SQLException: message=" +sqle.getMessage());
		} 
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Station.getFiredAlerts(long,long): Problem closing resultset, statement and/or connection to the database."); 
				(new Platform()).addMessageToLog("Station.getFiredAlerts(long,long): sqlexception Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return returnset;
	}
	
	
	JSONArray getFiredAlertsAsJSONArray(String beginstring, String endstring, boolean self_posted_only)
	{
		System.out.println("Station.getFiredAlertsAsJSONArray(" + beginstring + "," + endstring + ", " + self_posted_only);
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFiredAlerts(beginstring, endstring): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFiredAlerts(beginstring, endstring): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		return getFiredAlertsAsJSONArray(begin_in_ms, end_in_ms, self_posted_only);
	}
	
	JSONArray getFiredAlertsAsJSONArray(long begin_long, long end_long, boolean self_posted_only)
	{
		System.out.println("Station.getFiredAlertsAsJSONArray(" + begin_long + "(long)," + end_long + "(long), " + self_posted_only);
		TreeSet<Alert> alertset = getFiredAlerts(begin_long, end_long, self_posted_only);
		JSONArray return_ja = new JSONArray();
		Alert currentalert = null;
		Iterator<Alert> alert_it = alertset.iterator();
		while(alert_it.hasNext())
		{
			currentalert = alert_it.next();
			return_ja.put(currentalert.getAsJSONObject());
		}
		return return_ja;
	}
	
	// datestring convenience method
	JSONArray getFiredAlertStatistics(String beginstring, String endstring, long interval_in_ms, boolean include_unabridged_redirect_count, boolean include_sansbot_redirect_count, boolean self_posted_only)
	{
		System.out.println("Station.getFiredAlertStatistics(" + beginstring + "," + endstring + ", " + interval_in_ms + "(long), " + include_unabridged_redirect_count + ", " + include_sansbot_redirect_count +  ", " + self_posted_only + ")");
		
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFiredAlertStatistics(beginstring,endstring,interval, incl_redir_count, incl_sansbot_count): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFiredAlertStatistics(beginstring,endstring,interval, incl_redir_count, incl_sansbot_count): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		return getFiredAlertStatistics(begin_in_ms, end_in_ms, interval_in_ms, include_unabridged_redirect_count, include_sansbot_redirect_count, self_posted_only);
	}
	
	JSONArray getFiredAlertStatistics(long begin_long, long end_long, long interval_in_ms, boolean include_unabridged_redirect_count, boolean include_sansbot_redirect_count, boolean self_posted_only)
	{
		System.out.println("Station.getFiredAlertStatistics(" + begin_long + "(long)," + end_long + "(long), " + interval_in_ms + "(long), " + include_unabridged_redirect_count + ", " + include_sansbot_redirect_count +  ", " + self_posted_only + ")");
		JSONArray return_ja = new JSONArray();
		long x = begin_long;
		while(x < end_long)
		{
			return_ja.put(getFiredAlertStatisticsForInterval(x, x+interval_in_ms, false, false, self_posted_only));
			x = x + interval_in_ms;
		}
		return return_ja;
	}
	
	// this gets the results for one specific period in time, an hour, a day, week, month, etc and should be called multiple times for long time periods with many intervals
	// begin_long and end_long are set to this one, specific interval
	JSONObject getFiredAlertStatisticsForInterval(long begin_long, long end_long, boolean include_unabridged_redirect_count, boolean include_sansbot_redirect_count, boolean self_posted_only)
	{
		System.out.print("Station.getFiredAlertStatisticsForInterval(" + begin_long + "(long)," + end_long + "(long), " + include_unabridged_redirect_count + ", " + include_sansbot_redirect_count +  ", " + self_posted_only + ")");
		TreeSet<Alert> fired_alert_set = getFiredAlerts(begin_long, end_long, self_posted_only); // get the fired alerts for just this interval
		long unabridged_redirect_count = 0; long sansbot_redirect_count = 0;
		Iterator<Alert> alert_it = fired_alert_set.iterator();
		Alert currentalert = null;
		while(alert_it.hasNext())
		{
			currentalert = alert_it.next();
			unabridged_redirect_count = unabridged_redirect_count + currentalert.getRedirectCount(false); // include everything (unabridged)
			sansbot_redirect_count = sansbot_redirect_count + currentalert.getRedirectCount(true); // filter out bots (sansbot)
		}
		
		JSONObject return_jo = new JSONObject();
		try {
			String intervalstring = toDateString(end_long);
			return_jo.put("day", intervalstring);
			return_jo.put("fired_alert_count", fired_alert_set.size());
			return_jo.put("unabridged_redirect_count", unabridged_redirect_count);
			return_jo.put("sansbot_redirect_count", sansbot_redirect_count);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Station.getFiredAlertStatisticsForInterval(): return_jo=" + return_jo);
		return return_jo;
	}
	
	private String toDateString(long timestamp_in_ms)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp_in_ms); // we know that the most recent image has a timestamp of right now. It can't "survive" there for more than a few seconds
		// make the filename human-readable
		String year = new Integer(cal.get(Calendar.YEAR)).toString();
		String month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
		if(month.length() == 1) { month = "0" + month; }
		String day = new Integer(cal.get(Calendar.DAY_OF_MONTH)).toString();
		if(day.length() == 1) { day = "0" + day;} 
		String hour24 = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
		/*if(hour24.length() == 1) { hour24 = "0" + hour24;} 
		String minute = new Integer(cal.get(Calendar.MINUTE)).toString();
		if(minute.length() == 1) { minute = "0" + minute;} 
		String second = new Integer(cal.get(Calendar.SECOND)).toString();
		if(second.length() == 1) { second = "0" + second;} 
		String ms = new Long(timestamp_in_ms%1000).toString();
		if(ms.length() == 1) { ms = "00" + ms;} 
		if(ms.length() == 2) { ms = "0" + ms;}*/
		String datestring = "";
		datestring = year  + month + day;
		return datestring;
	}
	
	
	public static void main(String[] args) {
		Station s = new Station("wkyt");
		JSONObject fired_alert_count_jo = null;
		//fired_alert_count_jo = s.getFiredAlertCount(1374454057000L, 1374768476258L, false, false);
		//s.unlock("testval3", "twitter");
		//s.getAlertFrames(long begin_long, long end_long, int maw_int, double ma_modifier_double, double single_modifier_double, int awp_int, int nrpst)
		//JSONArray ja = s.getAlertFrames("20130705_230000", "20130705_231000", .8, 1, 3600, 2);
	}
	// datestring convenience method
	public JSONArray getFrameTimestamps(String beginstring, String endstring)
	{
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		return getFrameTimestamps(begin_in_ms, end_in_ms);
	}
	
	public JSONArray getFrameTimestamps(long begin_in_ms, long end_in_ms)
	{
		JSONArray timestamps_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			System.out.println("Station.getFrameTimestamps(): SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")");
			rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); 
			while(rs.next()) // at least one row exists
			{
				timestamps_ja.put(rs.getLong("timestamp_in_ms"));
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.getFrameTimestamps: message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}  
		return timestamps_ja; 
	}
	
	public long convertDateTimeStringToLong(String dtstring)
	{
		if(dtstring.length() < 8)
		{
			System.out.println("Station.convertDateTimeStringToLong(): string must be at least 8 char long");
			return -1L;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		cal.set(Calendar.YEAR, Integer.parseInt(dtstring.substring(0,4)));
		cal.set(Calendar.MONTH, Integer.parseInt(dtstring.substring(4,6)) - 1);
		cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dtstring.substring(6,8)));
		if(dtstring.length() >= 11)
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(dtstring.substring(9,11)));
		else
			cal.set(Calendar.HOUR_OF_DAY, 0);
		if(dtstring.length() >= 13)
			cal.set(Calendar.MINUTE, Integer.parseInt(dtstring.substring(11,13)));
		else
			cal.set(Calendar.MINUTE, 0);
		if(dtstring.length() >= 15)
			cal.set(Calendar.SECOND, Integer.parseInt(dtstring.substring(13,15)));
		else
			cal.set(Calendar.SECOND, 0);
		if(dtstring.length() ==19)
			cal.set(Calendar.MILLISECOND, Integer.parseInt(dtstring.substring(16,19)));
		else
			cal.set(Calendar.MILLISECOND, 0);
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
		System.out.println(dateFormat.format(cal.getTime()));
		
		return cal.getTimeInMillis();
	}
	
	public TreeSet<Frame> getFrames(String beginstring, String endstring, String designation) // convenience method for taking datestring in the form YYYYMMDD_HHMMSS_sss
	{
		System.out.println("Station.getFrames(): datestring method begin");
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		
		return getFrames(begin_in_ms, end_in_ms, designation);
	}
	
	// designation can be null. If designation supplied, then get all frames above the single threshold
	public TreeSet<Frame> getFrames(long begin_in_ms, long end_in_ms, String designation) // INCLUSIVE
	{
		//System.out.println("Station.getFrames(): long method begin");
		TreeSet<Frame> returnset = new TreeSet<Frame>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			if(designation == null) // get all frames
			{	
				System.out.println("Station.getFrames(no designation): SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")");
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); 
			}
			else if(designation != null) // get all frames where designation is above single thresh
			{	
				User reporter = new User(designation, "designation");
				double homogeneity_double = reporter.getHomogeneity();
				double threshold = homogeneity_double;
				System.out.println("Station.getFrames("+ designation + "): SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_score > " + threshold + ")"); 
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_score > " + threshold + ")"); 
			}
		
			// this loop gets each frame the database individually
			/*while(rs.next())
			{
				System.out.println("Station.getFrames(): " + rs.getLong("timestamp_in_ms"));
				returnset.add(new Frame(rs.getLong("timestamp_in_ms"), getCallLetters()));
			}*/

			// this if statement gets the whole resultset from the database as a whole, then parses it to create Frame objects manually. I don't know which method is faster.
			//System.out.println("Does the resultset have any rows?");
			if(rs.next()) // at least one row exists
			{
				//System.out.println("Getting frames from resultset");
				returnset = getFramesFromResultSet(rs);
			}
			
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.getFrames: message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}  
		return returnset; 
	}

	// datestring convenience method
	public JSONArray getFramesAsJSONArray(String beginstring, String endstring, boolean get_score_data)
	{
		//System.out.println("Station.getFramesAsJSONArray(): datestring method begin");
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, null);
		Iterator<Frame> it = frameset.iterator();
		Frame currentframe = null;
		while(it.hasNext())
		{
			currentframe = it.next();
			//System.out.println("putting frame " + currentframe.getTimestampInMillis() + " into jsonarray");
			frames_ja.put(currentframe.getAsJSONObject(get_score_data, null)); // no designation specified
		}
		return frames_ja;
	}
	
	public JSONArray getFramesAsJSONArray(long begin_in_ms, long end_in_ms, boolean get_score_data)
	{
		//System.out.println("Station.getFramesAsJSONArray(): long method begin");
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, null);
		Iterator<Frame> it = frameset.iterator();
		Frame currentframe = null;
		while(it.hasNext())
		{
			currentframe = it.next();
			//System.out.println("putting frame " + currentframe.getTimestampInMillis() + " into jsonarray");
			frames_ja.put(currentframe.getAsJSONObject(get_score_data, null)); // no designation specified
		}
		return frames_ja;
	}
	
	public JSONArray getFramesAboveDesignationHomogeneityThresholdAsJSONArray(long begin_in_ms, long end_in_ms, String designation, boolean get_score_data)
	{
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, designation);
		Iterator<Frame> it = frameset.iterator();
		Frame currentframe = null;
		while(it.hasNext())
		{
			currentframe = it.next();
			//System.out.println("putting frame " + currentframe.getTimestampInMillis() + " into jsonarray");
			frames_ja.put(currentframe.getAsJSONObject(get_score_data, null)); // no designation specified
		}
		return frames_ja;
	}
	
	// datestring convenience method.
	JSONArray getAlertFrames(String beginstring, String endstring, double ma_modifier_double, int awp_int, int nrpst, double delta_double, int maw)
	{
		if(beginstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): beginstring must be at least 8 char long");
			return null;
		}
		if(endstring.length() < 8)
		{
			System.out.println("Station.getFrames(beginstring,endstring,designation,singlemodifier): endstring must be at least 8 char long");
			return null;
		}
		long begin_in_ms = convertDateTimeStringToLong(beginstring);
		long end_in_ms = convertDateTimeStringToLong(endstring);
		return getAlertFrames(begin_in_ms, end_in_ms, ma_modifier_double, awp_int, nrpst, delta_double, maw);
	}
	
	JSONArray getAlertFrames(long begin_in_ms, long end_in_ms, double ma_modifier_double, int awp_int, int nrpst_int, double delta_double, int maw_int)
	{
		System.out.println("Station.getAlertFrames(): begin");
		JSONArray return_ja = new JSONArray();
		TreeSet<String> reporters = getReporterDesignations();
		Iterator<String> it = reporters.iterator();
		User currentreporter = null;
		
		String querystring = "SELECT * FROM frames_" + getCallLetters() + " WHERE ((`timestamp_in_ms` <= " + end_in_ms + " AND `timestamp_in_ms` >= " + begin_in_ms + ") AND ( ";
		while(it.hasNext()) // looping through reporters
		{
			currentreporter = new User(it.next(), "designation");
			querystring = querystring + " (`" + currentreporter.getDesignation() + "_ma" + maw_int + "` >= " + (currentreporter.getHomogeneity()*ma_modifier_double) + " AND ";
			querystring = querystring + " `" + currentreporter.getDesignation() + "_score` >= " + currentreporter.getHomogeneity() + ") OR ";
		}
		querystring = querystring.substring(0,querystring.length() - 4) + "))";
		//System.out.println("Station.getAlertFrames(): querystring=" + querystring);
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery(querystring);
			TreeSet<Frame> frames = getFramesFromResultSet(rs);
			System.out.println("Station.getAlertFrames(): # of frames=" + frames.size());
			Iterator<Frame> frames_it = frames.iterator();
			Frame currentframe = null;

			JSONObject current_jo = null;
			while(frames_it.hasNext())
			{
				currentframe = frames_it.next();
				current_jo = currentframe.process(ma_modifier_double, nrpst_int, delta_double, "test", "silent", awp_int, awp_int, maw_int); // which_timers, alert_mode, tw_wp_override, fb_wp_override
				if(current_jo != null && current_jo.has("alert_triggered") && current_jo.getBoolean("alert_triggered"))
				{
					System.out.println("Station.getAlertFrames(): Alert triggered. putting current_jo onto return_ja");
					return_ja.put(current_jo);
				}
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("Station.getAlertFrames(): SQLException message=" +sqle.getMessage());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}  
		return return_ja; 
	}

	// DANGEROUS!!!! This will reset all alerts for every active reporter at this station
	boolean resetProductionAlertTimers()
	{
		System.out.println("resetting all last alerts");
		Iterator<String> it = reporters.iterator();
		String query = "SELECT * FROM people WHERE ("; 
		while(it.hasNext())
		{
			query = query + "`designation`='" + it.next() + "' OR ";
		}
		query = query.substring(0,query.length() - 4) + ")";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			// get frames where this designation crosses the single frame threshold
			rs = stmt.executeQuery(query);
			while(rs.next())
			{
				rs.updateLong("facebook_last_alert", 0);
				rs.updateLong("twitter_last_alert", 0);
				rs.updateString("facebook_last_alert_hr", "");
				rs.updateString("twitter_last_alert_hr", "");
				rs.updateRow();
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.resetAllLastAlerts(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.resetAllLastAlerts(): Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}   		
		return true;
	}
	
	boolean resetTestAlertTimers()
	{
		System.out.println("resetting all last alerts");
		Iterator<String> it = reporters.iterator();
		String query = "SELECT * FROM people WHERE ("; 
		while(it.hasNext())
		{
			query = query + "`designation`='" + it.next() + "' OR ";
		}
		query = query.substring(0,query.length() - 4) + ")";
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			// get frames where this designation crosses the single frame threshold
			rs = stmt.executeQuery(query);
			while(rs.next())
			{
				rs.updateLong("facebook_last_alert_test", 0);
				rs.updateLong("twitter_last_alert_test", 0);
				rs.updateString("facebook_last_alert_test_hr", "");
				rs.updateString("twitter_last_alert_test_hr", "");
				rs.updateRow();
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.resetAllLastAlerts(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.resetAllLastAlerts(): Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}   		
		return true;
	}
	
	public boolean isLiveStreaming()
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " ORDER BY timestamp_in_ms DESC LIMIT 1"); // get the frames in the time range
			if(rs.next())
			{	
				if((new Date().getTime() - rs.getLong("timestamp_in_ms")) < 30000) // last frame less than 30 seconds
					returnval = true;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.isLiveStreaming: Error getting table row. message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}   	
		return returnval;
	}

	// value of alert_mode has already been validated by EndPoint. Maybe should do it here?
	public boolean setAlertMode(String which, String on_or_off)
	{
		boolean returnval = false;
		Connection con = null;
		PreparedStatement pstmt = null;
		String updateString = null;
		boolean onoffbool = false;
		if(on_or_off.equals("on"))
			onoffbool = true;
		try
		{
			con = datasource.getConnection();
			
			updateString = "UPDATE stations SET `" + which + "`=" + onoffbool + " WHERE call_letters='" + getCallLetters() + "' ";
			pstmt = con.prepareStatement(updateString);
			pstmt.executeUpdate();
			if(pstmt.getUpdateCount() == 1) // the update actually occurred
				returnval = true; 
			else
			{
				(new Platform()).addMessageToLog("Station.setAlertMode(): Tried to set station which to " + onoffbool + " but failed.");
			}
			pstmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.setAlertMode(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (pstmt != null) { pstmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.setAlertMode(): Error occurred when closing con. message=" +sqle.getMessage());
			}
		}   		
		return returnval;
	}
	
	boolean lock(String uuid, String social_type, String which_lock)
	{
		boolean returnval = false;
		if(!(which_lock.equals("master") || which_lock.equals("individual")))
		{
			(new Platform()).addMessageToLog("Station.lock(): Tried to lock with incorrect which_lock value=" + which_lock + ". Aborted.");
			return returnval;
		}
		Connection con = null;
		PreparedStatement pstmt = null;
		String updateString = null;
		try
		{
			con = datasource.getConnection();
			if(social_type.equals("twitter"))
				updateString = "UPDATE stations SET `twitter_lock_" + which_lock + "`='" + uuid + "' WHERE (call_letters='" + getCallLetters() + "' AND `twitter_lock_" + which_lock + "`='') ";
			else if (social_type.equals("facebook"))
				updateString = "UPDATE stations SET `facebook_lock_" + which_lock + "`='" + uuid + "' WHERE (call_letters='" + getCallLetters() + "' AND `facebook_lock_" + which_lock + "`='')";
			System.out.println("Executing statement: " + updateString);
			pstmt = con.prepareStatement(updateString);
			pstmt.executeUpdate();
			System.out.println(pstmt.getUpdateCount());
			if(pstmt.getUpdateCount() == 1) // the update actually occurred
				returnval = true; 
			else
			{
				(new Platform()).addMessageToLog("Station.lock(): Tried to set " + social_type + "-" + which_lock + " lock but the existing value was not empty!");
			}
			pstmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.lock(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (pstmt != null) { pstmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.lock(): Error occurred when closing con. message=" +sqle.getMessage());
			}
		}   		
		return returnval;
	}
	

	boolean unlock(String uuid, String social_type, String which_lock)
	{
		boolean returnval = false;
		if(!(which_lock.equals("master") || which_lock.equals("individual")))
		{
			(new Platform()).addMessageToLog("Station.lock(): Tried to unlock with incorrect which_lock value=" + which_lock + ". Aborted.");
			return returnval;
		}
		Connection con = null;
		PreparedStatement pstmt = null;
		String updateString = null;
		try
		{
			con = datasource.getConnection();
			if(social_type.equals("twitter"))
				updateString = "UPDATE stations SET `twitter_lock_" + which_lock + "`='' WHERE (call_letters='" + getCallLetters() + "' AND `twitter_lock_" + which_lock + "`='" + uuid + "') ";
			else if (social_type.equals("facebook"))
				updateString = "UPDATE stations SET `facebook_lock_" + which_lock + "`='' WHERE (call_letters='" + getCallLetters() + "' AND `facebook_lock_" + which_lock + "`='" + uuid + "')";
			System.out.println("Executing statement: " + updateString);
			pstmt = con.prepareStatement(updateString);
			pstmt.executeUpdate();
			System.out.println(pstmt.getUpdateCount());
			if(pstmt.getUpdateCount() == 1) // the update actually occurred
				returnval = true; 
			else
			{
				(new Platform()).addMessageToLog("Station.unlock(): Tried to unlock " + social_type + " but UUID mismatch (or UUID was empty). Attempting an unlock when the lock was unsuccesful is ok. Overkill, but no big deal.");
			}
			pstmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.unlock(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (pstmt != null) { pstmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.unlock(): Error occurred when closing con. message=" +sqle.getMessage());
			}
		}   		
		return returnval;
	}
	
	String getMessage(String social_type, long timestamp_in_ms, long redirect_id, User reporter)
	{
		String returnval = "";
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		cal.setTimeInMillis(timestamp_in_ms);
		int hour = cal.get(Calendar.HOUR);
		if(hour == 0)
			hour = 12;
		int minute = cal.get(Calendar.MINUTE);
		String minutestring = (new Integer(minute)).toString();
		if(minutestring.length() < 2)
			minutestring = "0" + minutestring;
		String am_or_pm_string = "";
		if(cal.get(Calendar.AM_PM) == 0)
			am_or_pm_string = " AM";
		else
			am_or_pm_string = " PM";
		String ts_string = hour + ":" + minutestring + am_or_pm_string;
		
		
		if(social_type.equals("facebook"))
		{
			returnval = reporter.getDisplayName() + " is on the air right now (" + ts_string + "). Tune in or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #" + getCallLetters();
		}
		else if(social_type.equals("twitter"))
		{
			returnval = ".@" + reporter.getTwitterHandle() + " is on the air right now (" + ts_string + "). Tune in or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #" + getCallLetters();
		}
		return returnval;
	}
	
	
	TreeSet<Frame> getFramesFromResultSet(ResultSet rs)
	{
		if(rs == null)
		{
			System.out.println("Station.getFramesFromResultSet() entering with rs == null, returning");
			return null;
		}
		else
		{
			//System.out.println("Station.getFramesFromResultSet() entering with rs != null");
		}
		
		TreeSet<Frame> returnframes = new TreeSet<Frame>();
		try
		{
			ResultSetMetaData rsmd = rs.getMetaData();
			int columncount = rsmd.getColumnCount();
			int reportercount = 0;
			int x = 1; 
			//System.out.println("Station.getFramesFromResultSet() Starting loop through " + columncount + " columns to find how many reporters are there...");
			String station = "";
			while(x <= columncount)
			{
				if(x == 1)
					station = rsmd.getTableName(1).substring(rsmd.getTableName(1).indexOf("_") + 1);
				if(rsmd.getColumnName(x).endsWith("_score"))
				{
					reportercount++;
				}
				x++;
			}
			//System.out.println("Found " + reportercount + " columns. Initalizing arrays.");
			//String reporter_designations[] = new String[reportercount];
			//double reporter_scores[] = new double[reportercount];
			//JSONArray reporter_score_arrays[] = new JSONArray[reportercount];
			//int reporter_nums[] = new int[reportercount];
			String reporter_designations[] = null;
			double reporter_scores[] = null;
			JSONArray reporter_score_arrays[] = null;
			int reporter_nums[] = null;
			double reporter_ma6s[] = null;
			double reporter_ma5s[] = null;
			double reporter_ma4s[] = null;
			double reporter_ma3s[] = null;
			rs.beforeFirst();
			//System.out.println("Starting loop through resultset of frames...");
			while(rs.next())
			{
				reporter_designations = new String[reportercount];
				reporter_scores = new double[reportercount];
				reporter_score_arrays = new JSONArray[reportercount];
				reporter_nums = new int[reportercount];
				reporter_ma6s = new double[reportercount];
				reporter_ma5s = new double[reportercount];
				reporter_ma4s = new double[reportercount];
				reporter_ma3s = new double[reportercount];
				int reporter_index = 0;
				x=1; 
				//System.out.println("On frame " + rs.getString("image_name") + ", starting loop through " + columncount + " columns to fill reporter arrays...");
				boolean db_has_ma6_data = true; // assume true until proven false
				boolean db_has_ma5_data = true;
				boolean db_has_ma4_data = true;
				boolean db_has_ma3_data = true;
				while(x <= columncount)
				{
					//System.out.println("Reading columname: " + rsmd.getColumnName(x));
					if(rsmd.getColumnName(x).endsWith("_score"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_score"));
						reporter_scores[reporter_index] = rs.getDouble(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_ma3"))
					{
						if(db_has_ma3_data == true) // it could either be true or assumed to be true at this point
						{	
							reporter_ma3s[reporter_index] = rs.getDouble(x); // try to start saving reporter_ma6 data. 
							if (rs.wasNull()) // if that didn't work
							{
								reporter_ma3s = null; // then set the reporter_ma6s array to null to signify that this row doesn't have them. Maybe this row is new and they'll be set later.
								db_has_ma3_data = false;
							}
						}
						// else skip. We already know there is no ma3 data in this row
					}
					else if(rsmd.getColumnName(x).endsWith("_ma4"))
					{
						if(db_has_ma4_data == true) // it could either be true or assumed to be true at this point
						{	
							reporter_ma4s[reporter_index] = rs.getDouble(x); // try to start saving reporter_ma6 data. 
							if (rs.wasNull()) // if that didn't work
							{
								reporter_ma4s = null; // then set the reporter_ma6s array to null to signify that this row doesn't have them. Maybe this row is new and they'll be set later.
								db_has_ma4_data = false;
							}
						}
						// else skip. We already know there is no ma4 data in this row
					}
					else if(rsmd.getColumnName(x).endsWith("_ma5"))
					{
						if(db_has_ma5_data == true) // it could either be true or assumed to be true at this point
						{	
							reporter_ma5s[reporter_index] = rs.getDouble(x); // try to start saving reporter_ma6 data. 
							if (rs.wasNull()) // if that didn't work
							{
								reporter_ma5s = null; // then set the reporter_ma6s array to null to signify that this row doesn't have them. Maybe this row is new and they'll be set later.
								db_has_ma5_data = false;
							}
						}
						// else skip. We already know there is no ma5 data in this row
					}
					else if(rsmd.getColumnName(x).endsWith("_ma6"))
					{
						//reporter_ma6s[reporter_index] = rs.getDouble(x);
						//reporter_index++;
						if(db_has_ma6_data == true) // it could either be true or assumed to be true at this point
						{	
							reporter_ma6s[reporter_index] = rs.getDouble(x); // try to start saving reporter_ma6 data. 
							if (rs.wasNull()) // if that didn't work
							{
								reporter_ma6s = null; // then set the reporter_ma6s array to null to signify that this row doesn't have them. 
								db_has_ma6_data = false;
							}
						}
						else
						{
							// skip. We already know there is no ma6 data in this row
						}
						reporter_index++;
					}
					else
					{
						//System.out.println("Skipping a non-score-related row.");
					}
					x++;
				}
				//System.out.println("Adding Frame object to treeset and going to next...");
				returnframes.add(new Frame(rs.getLong("timestamp_in_ms"), rs.getString("image_name"), 
						rs.getString("url"), rs.getInt("frame_rate"), station, reporter_designations, 
						reporter_scores, reporter_score_arrays, reporter_nums, reporter_ma3s, reporter_ma4s, reporter_ma5s, reporter_ma6s));
				//System.out.println("... frame added");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		} 
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); }
			}
			catch(SQLException sqle)
			{ 
				sqle.printStackTrace();
			}
		}  
		return returnframes;
	}
	
	
	public JSONObject getAsJSONObject()
	{
		JSONObject return_jo = new JSONObject();
		try
		{
			return_jo.put("call_letters", getCallLetters());
			return_jo.put("city", getCity());
			return_jo.put("state", getState());
			return_jo.put("frame_rate", getFrameRate());
			return_jo.put("dma2013", getDMA());
			//return_jo.put("active", isActive());
			return_jo.put("reporters", new JSONArray(getReporterDesignations()).toString());
			return_jo.put("administrators", new JSONArray(getAdministrators()).toString());
			return_jo.put("maw", maw);
			return_jo.put("mamodifier", mamodifier);
			return_jo.put("delta", delta);
			return_jo.put("twitter_active_individual", twitter_active_individual);
			return_jo.put("facebook_active_individual", facebook_active_individual);
			return_jo.put("twitter_active_master", twitter_active_master);
			return_jo.put("facebook_active_master", facebook_active_master);
			return_jo.put("nrpst", nrpst);
		}	
		catch (JSONException e) {
			return_jo = null;
			e.printStackTrace();
		}
		return return_jo;
	}

	public int compareTo(Station o) // this sorts by call_letters alphabetically
	{
	    String othercall = ((Station)o).getCallLetters();
	    int x = othercall.compareTo(call_letters);
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
	
}
