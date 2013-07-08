package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.MessagingException;

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
	private boolean active;
	private boolean valid;
	private int frame_rate; // milliseconds per frame
	private String s3_bucket_public_hostname;
	private String livestream_url;
	private String livestream_url_alias;
	
	String dbName = System.getProperty("RDS_DB_NAME"); 
	String userName = System.getProperty("RDS_USERNAME"); 
	String password = System.getProperty("RDS_PASSWORD"); 
	String hostname = System.getProperty("RDS_HOSTNAME");
	String port = System.getProperty("RDS_PORT");
	
	public Station(String inc_call_letters)
	{
		//System.err.println("Station init()");
		try {
		        Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
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
				active = rs.getBoolean("active");
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
	
	public TreeSet<String> getReporters()
	{
		return reporters;
	}

	public boolean isActive()
	{
		return active;
	}
	
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
	
	JSONArray getMostRecentAlerts(int num_to_get)
	{
		JSONArray alerts_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		Platform p = new Platform();
		try
		{
			con = DriverManager.getConnection(p.getJDBCConnectionString());
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE (`station`='" + getCallLetters() + "' AND `deletion_timestamp` IS NULL) ORDER BY creation_timestamp DESC LIMIT 0," + num_to_get);
			int x = 0;
			JSONObject jo;
			while(rs.next() && x < num_to_get)
			{
				jo = new JSONObject();
				jo.put("image_url", rs.getString("image_url"));
				jo.put("designation", rs.getString("designation"));
				java.util.Date date = rs.getTimestamp("creation_timestamp");
				jo.put("creation_timestamp", ((java.util.Date)rs.getTimestamp("creation_timestamp")).toLocaleString());
				if(rs.getString("created_by").isEmpty())
					jo.put("created_by", rs.getString("designation"));
				else
					jo.put("created_by", rs.getString("created_by"));
				jo.put("social_type", rs.getString("social_type"));
				jo.put("station", rs.getString("station"));
				jo.put("id", rs.getLong("id"));
				alerts_ja.put(jo);
				x++;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			p.addMessageToLog("SQLException in Platform.getMostRecentAlerts: message=" +sqle.getMessage());
		} catch (JSONException e) {
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
				p.addMessageToLog("SQLException in Platform.getMostRecentAlerts: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return alerts_ja;
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
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
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
		//System.out.println("Station.getFrames(): getting frames from " + begin_in_ms + " to " + end_in_ms);
		TreeSet<Frame> returnset = new TreeSet<Frame>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		
		try
		{
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
			stmt = con.createStatement();
			if(designation == null) // get all frames
			{	
				//System.out.println("Station.getFrames(no designation): SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")");
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); 
			}
			else if(designation != null) // get all frames where designation is above single thresh
			{	
				User reporter = new User(designation, "designation");
				double homogeneity_double = reporter.getHomogeneity();
				double threshold = homogeneity_double;
				//System.out.println("Station.getFrames("+ designation + "): SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
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
	JSONArray getAlertFrames(String beginstring, String endstring, double ma_modifier_double, int awp_int, int nrpst, double delta_double)
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
		return getAlertFrames(begin_in_ms, end_in_ms, ma_modifier_double, awp_int, nrpst, delta_double);
	}
	
	JSONArray getAlertFrames(long begin_in_ms, long end_in_ms, double ma_modifier_double, int awp_int, int nrpst_int, double delta_double)
	{
		System.out.println("Station.getAlertFrames(): begin");
		JSONArray return_ja = new JSONArray();
		TreeSet<String> reporters = getReporters();
		Iterator<String> it = reporters.iterator();
		User currentreporter = null;
		
		String querystring = "SELECT * FROM frames_" + getCallLetters() + " WHERE ((`timestamp_in_ms` <= " + end_in_ms + " AND `timestamp_in_ms` >= " + begin_in_ms + ") AND ( ";
		while(it.hasNext()) // looping through reporters
		{
			currentreporter = new User(it.next(), "designation");
			querystring = querystring + " (`" + currentreporter.getDesignation() + "_ma6` >= " + (currentreporter.getHomogeneity()*ma_modifier_double) + " AND ";
			querystring = querystring + " `" + currentreporter.getDesignation() + "_avg` >= " + currentreporter.getHomogeneity() + ") OR ";
		}
		querystring = querystring.substring(0,querystring.length() - 4) + "))";
		System.out.println("Station.getAlertFrames(): querystring=" + querystring);
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		Platform p = new Platform();
		try
		{
			con = DriverManager.getConnection(p.getJDBCConnectionString());
			stmt = con.createStatement();
			rs = stmt.executeQuery(querystring);
			TreeSet<Frame> frames = getFramesFromResultSet(rs);
			System.out.println("# of frames=" + frames.size());
			Iterator<Frame> frames_it = frames.iterator();
			Frame currentframe = null;

			JSONObject current_jo = null;
			while(frames_it.hasNext())
			{
				currentframe = frames_it.next();
				current_jo = currentframe.process(ma_modifier_double, nrpst_int, delta_double, "test", "silent", awp_int, awp_int); // which_timers, alert_mode, tw_wp_override, fb_wp_override
				if(current_jo != null && current_jo.has("alert_triggered") && current_jo.getBoolean("alert_triggered"))
					return_ja.put(current_jo);
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
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
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
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
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
	
	boolean isLocked(String social_type)
	{
		boolean returnval = true;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
				stmt = con.createStatement();
				// get frames where this designation crosses the single frame threshold
				String query = "SELECT * FROM stations WHERE call_letters='" + getCallLetters() + "'";
				rs = stmt.executeQuery(query);
				if(rs.next())
				{
					String lock = null;
					if(social_type.equals("twitter"))
						lock = rs.getString("twitter_lock");
					if(social_type.equals("facebook"))
						lock = rs.getString("facebook_lock");
					if(lock.isEmpty())
						returnval = false;
				}
				else
				{
					(new Platform()).addMessageToLog("Eror in Station.isLocked(): couldn't find row for station=" + getCallLetters());
				}
				rs.close();
				stmt.close();
				con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			(new Platform()).addMessageToLog("SQLException in Station.isLocked(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.isLocked(): Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}   		
		return returnval;
	}
	
	boolean lock(String uuid, String social_type)
	{
		boolean returnval = false;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
				stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				// get frames where this designation crosses the single frame threshold
				String query = "SELECT * FROM stations WHERE call_letters='" + getCallLetters() + "' ";
				rs = stmt.executeQuery(query);
				if(rs.next())
				{
					if(social_type.equals("twitter"))
					{
						if(!rs.getString("twitter_lock").isEmpty())
						{	
							(new Platform()).addMessageToLog("Station.lock(): Tried to set twitter lock but the existing value was not empty!");
							returnval = false;
						}
						else
						{
							rs.updateString("twitter_lock", uuid);
							returnval = true;
							rs.updateRow();
						}
					}
					else if(social_type.equals("facebook"))
					{
						if(!rs.getString("facebook_lock").isEmpty())
						{	
							(new Platform()).addMessageToLog("Station.lock(): Tried to set facebook lock but the existing value was not empty!");
							returnval = false;
						}
						else
						{
							rs.updateString("facebook_lock", uuid);
							returnval = true;
							rs.updateRow();
						}
					}
				}
				else
				{
					(new Platform()).addMessageToLog("Eror in Station.lock(): couldn't find row for station=" + getCallLetters());
				}
				rs.close();
				stmt.close();
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
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.lock(): Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}   		
		return returnval;
	}
		
	boolean unlock(String uuid, String social_type)
	{
		boolean returnval = false;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			Platform p = new Platform();
			con = DriverManager.getConnection(p.getJDBCConnectionString());
				stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				// get frames where this designation crosses the single frame threshold
				String query = "SELECT * FROM stations WHERE call_letters='" + getCallLetters() + "' ";
				rs = stmt.executeQuery(query);
				if(rs.next())
				{
					if(social_type.equals("twitter"))
					{
						if(!rs.getString("twitter_lock").equals(uuid))
						{	
							(new Platform()).addMessageToLog("ERROR in Station.unlock(): Tried to unlock twitter but the existing value did not match the specified UUID. Another process set this lock! BAD!");
							returnval = false;
						}
						else
						{
							rs.updateString("twitter_lock", "");
							returnval = true;
							rs.updateRow();
						}
					}
					else if(social_type.equals("facebook"))
					{
						if(!rs.getString("facebook_lock").equals(uuid))
						{	
							(new Platform()).addMessageToLog("ERROR in Station.unlock(): Tried to unlock facebook but the existing value did not match the specified UUID. Another process set this lock! BAD!");
							returnval = false;
						}
						else
						{
							rs.updateString("facebook_lock", "");
							returnval = true;
							rs.updateRow();
						}
					}
				}
				else
				{
					(new Platform()).addMessageToLog("Eror in Station.unlock(): couldn't find row for station=" + getCallLetters());
				}
				rs.close();
				stmt.close();
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
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				(new Platform()).addMessageToLog("SQLException in Station.unlock(): Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
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
			returnval = "@" + reporter.getTwitterHandle() + " is on the air right now (" + ts_string + "). Tune in or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #" + getCallLetters();
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
				if(rsmd.getColumnName(x).endsWith("_avg"))
				{
					reportercount++;
				}
				x++;
			}
			//System.out.println("Found " + reportercount + " columns. Initalizing arrays.");
			//String reporter_designations[] = new String[reportercount];
			//double reporter_avgs[] = new double[reportercount];
			//JSONArray reporter_score_arrays[] = new JSONArray[reportercount];
			//int reporter_nums[] = new int[reportercount];
			String reporter_designations[] = null;
			double reporter_avgs[] = null;
			JSONArray reporter_score_arrays[] = null;
			int reporter_nums[] = null;
			double reporter_ma6s[] = null;
			rs.beforeFirst();
			//System.out.println("Starting loop through resultset of frames...");
			while(rs.next())
			{
				reporter_designations = new String[reportercount];
				reporter_avgs = new double[reportercount];
				reporter_score_arrays = new JSONArray[reportercount];
				reporter_nums = new int[reportercount];
				reporter_ma6s = new double[reportercount];
				int reporter_index = 0;
				x=1; 
				System.out.println("On frame " + rs.getString("image_name") + ", starting loop through " + columncount + " columns to fill reporter arrays...");
				boolean db_has_ma6_data = true; // assume true until proven false
				while(x <= columncount)
				{
					//System.out.println("Reading columname: " + rsmd.getColumnName(x));
					if(rsmd.getColumnName(x).endsWith("_avg"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_avg"));
						reporter_avgs[reporter_index] = rs.getDouble(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
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
				returnframes.add(new Frame(rs.getLong("timestamp_in_ms"), rs.getString("image_name"), rs.getString("s3_location"),
						rs.getString("url"), rs.getInt("frame_rate"), station, reporter_designations, 
						reporter_avgs, reporter_score_arrays, reporter_nums, reporter_ma6s));
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
			return_jo.put("active", isActive());
			return_jo.put("reporters", new JSONArray(getReporters()).toString());
			return_jo.put("administrators", new JSONArray(getAdministrators()).toString());
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
	
	public static void main(String[] args) {
		Station s = new Station("wkyt");
		s.resetProductionAlertTimers();
		//s.getAlertFrames(long begin_long, long end_long, int maw_int, double ma_modifier_double, double single_modifier_double, int awp_int, int nrpst)
		//JSONArray ja = s.getAlertFrames("20130705_230000", "20130705_231000", .8, 1, 3600, 2);
	}

}
