package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;

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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
	
	TreeSet<Frame> getFramesFromResultSet(ResultSet rs)
	{
		TreeSet<Frame> returnframes = new TreeSet<Frame>();
		try
		{
			ResultSetMetaData rsmd = rs.getMetaData();
			int columncount = rsmd.getColumnCount();
			int reportercount = 0;
			int x = 1; 
			//System.out.println("Starting loop through " + columncount + " columns to find how many reporters are there...");
			while(x <= columncount)
			{
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
			rs.beforeFirst();
			//System.out.println("Starting loop through resultset of frames...");
			while(rs.next())
			{
				reporter_designations = new String[reportercount];
				reporter_avgs = new double[reportercount];
				reporter_score_arrays = new JSONArray[reportercount];
				reporter_nums = new int[reportercount];
				int reporter_index = 0;
				x=1; 
				//System.out.println("Starting loop through " + columncount + " columns to fill reporter arrays...");
				while(x <= columncount)
				{
					//System.out.println("Reading columname: " + rsmd.getColumnName(x));
					if(rsmd.getColumnName(x).endsWith("_avg"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_avg"));
						reporter_avgs[reporter_index] = rs.getDouble(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_scores"))
					{
						if(rs.getString(x) == null || rs.getString(x).isEmpty())
							reporter_score_arrays[reporter_index] = new JSONArray();
						else
							reporter_score_arrays[reporter_index] = new JSONArray(rs.getString(x));
					}
					else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
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
						rs.getString("url"), rs.getInt("frame_rate"), getCallLetters(), reporter_designations, 
						reporter_avgs, reporter_score_arrays, reporter_nums));
				//System.out.println("... frame added");
			}
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	public JSONArray getFrameTimestamps(long begin_in_ms, long end_in_ms)
	{
		JSONArray timestamps_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
	
	// designation can be null. If designation supplied, then get all frames above the single threshold
	public TreeSet<Frame> getFrames(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double) // INCLUSIVE
	{
		TreeSet<Frame> returnset = new TreeSet<Frame>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
				double threshold = homogeneity_double * single_modifier_double;
				System.out.println("Station.getFrames("+ designation + "): SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
			}
		
			//System.out.println("Does the resultset have any rows?");
			if(rs.next()) // at least one row exists
			{
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

	public JSONArray getFramesAsJSONArray(long begin_in_ms, long end_in_ms, boolean get_score_data)
	{
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, null, 0);
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
	
	public JSONArray getFramesAboveDesignationHomogeneityThresholdAsJSONArray(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double, boolean get_score_data)
	{
		JSONArray frames_ja = new JSONArray();
		TreeSet<Frame> frameset = getFrames(begin_in_ms, end_in_ms, designation, single_modifier_double);
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
	
	JSONArray getAlertFrames(long begin_long, long end_long, int maw_int, double ma_modifier_double, double single_modifier_double, int awp_int)
	{
		System.out.println("Endpoint.getAlertFrames() begin");
		JSONArray alert_frames_ja = new JSONArray();
		try
		{
			TreeSet<String> reporters = getReporters();
			Connection con = null;
			Statement stmt = null;
			ResultSet rs = null;
			Statement stmt2 = null;
			ResultSet rs2 = null;
			long ts = 0L;
			double current_homogeneity;
			String current_designation;
			try
			{
				Iterator<String> it = reporters.iterator();
				User currentreporter = null;
				while(it.hasNext()) // looping through reporters
				{
					currentreporter = new User(it.next(), "designation");
					System.out.println("Endpoint.gAF(): looping reporters. " + currentreporter.getDesignation());
					current_homogeneity = currentreporter.getHomogeneity();
					current_designation = currentreporter.getDesignation();
					
					con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
					stmt = con.createStatement();
					// get frames where this designation crosses the single frame threshold
					rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms >= " + (1000*begin_long) + " AND timestamp_in_ms <= " + (1000*end_long) + " AND " + current_designation + "_avg > " + (current_homogeneity * single_modifier_double) + ") ORDER BY timestamp_in_ms ASC");
					rs.last();
					System.out.println("Station.gAF() found " + rs.getRow()  + " frames over threshold (" + current_homogeneity + " * " + single_modifier_double + "=" +  (current_homogeneity * single_modifier_double) + ") for " + currentreporter.getDesignation());
					rs.beforeFirst();
					TreeSet<Frame> frames_past_single_thresh = getFramesFromResultSet(rs);
					Iterator<Frame> frames_past_single_thresh_it = frames_past_single_thresh.iterator();
					Frame currentframe = null;
					Frame subsequentframe = null;
					Frame frame_that_passed_ma_thresh = null;
					double moving_average = 0.0;
					long last_ts_of_frame_added = 0L;
					while(frames_past_single_thresh_it.hasNext())
					{
						currentframe = frames_past_single_thresh_it.next();
						if((currentframe.getTimestampInMillis() - last_ts_of_frame_added) > (awp_int * 1000)) // if this frame is outside the awp and eligible to be returned
						{	
							System.out.println("Looking up moving average of " + current_designation + " with maw_int=" + maw_int);
							moving_average = currentframe.getMovingAverage(maw_int, current_designation);
							frame_that_passed_ma_thresh = null;
							if(moving_average == -1)
							{
								System.out.println("Station.getAlertFrames(): There were not enough frames in this window. Skip this frame.");
							}
							else
							{
								if(moving_average > (current_homogeneity * ma_modifier_double) && 
										moving_average == currentframe.getHighestMovingAverage(maw_int))
								{
									frame_that_passed_ma_thresh = currentframe;
								}
								else // initial frame didn't pass, look for subsequent frames that pass the ma threshold.
								{
									System.out.println("Station.gAF(): ma of current DID NOT pass req thresh. ma=" + moving_average + " thresh=" + (current_homogeneity * ma_modifier_double));
									stmt2 = con.createStatement();
									// get frames after the current ts within the maw_int window
									ts = currentframe.getTimestampInMillis();
									System.out.println("executing SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + ts + " AND timestamp_in_ms <= " + (ts + 1000*maw_int) + ") ORDER BY timestamp_in_ms ASC");
									rs2 = stmt2.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms > " + ts + " AND timestamp_in_ms <= " + (ts + 1000*maw_int) + ") ORDER BY timestamp_in_ms ASC");
									rs2.last();
									System.out.println("Got " + rs2.getRow() + " subsequent frames.");
									rs2.beforeFirst();
									TreeSet<Frame> subsequent_frames = getFramesFromResultSet(rs2);
									Iterator<Frame> subsequent_frames_it = subsequent_frames.iterator();
									while(subsequent_frames_it.hasNext())
									{
										subsequentframe = subsequent_frames_it.next();
										moving_average = subsequentframe.getMovingAverage(maw_int, current_designation);
										if(moving_average > (current_homogeneity * ma_modifier_double) && 
												moving_average == subsequentframe.getHighestMovingAverage(maw_int))
										{
											frame_that_passed_ma_thresh = subsequentframe;
											break;
										}
										else
										{
											System.out.println("Endpoint.gAF(): ma of subsequent DID NOT pass req thresh. ma=" + moving_average + " thresh=" + (current_homogeneity * ma_modifier_double));
										}
									}
									rs2.close();
									stmt2.close();
									if(frame_that_passed_ma_thresh != null) 
									{
										JSONObject jo2add = frame_that_passed_ma_thresh.getAsJSONObject(true, null); // no designation specified
										jo2add.put("designation", current_designation);
										jo2add.put("ma_for_alert_frame", currentframe.getMovingAverage(maw_int, current_designation));
										jo2add.put("ma_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getMovingAverage(maw_int, current_designation));
										jo2add.put("score_for_alert_frame", currentframe.getScore(current_designation));
										jo2add.put("score_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getScore(current_designation));
										jo2add.put("image_name_for_frame_that_passed_ma_thresh", frame_that_passed_ma_thresh.getImageName());
										jo2add.put("homogeneity", current_homogeneity);
										jo2add.put("ma_threshold",  (current_homogeneity * ma_modifier_double));
										jo2add.put("single_threshold", (current_homogeneity * single_modifier_double));
										alert_frames_ja.put(jo2add); 
										last_ts_of_frame_added = frame_that_passed_ma_thresh.getTimestampInMillis();
										// do not break the frames loop as before. Get all alerts for this window, pursuant to the awp_int between each.
									}
								}
							}
						}
					}
				}
				rs.close();
				stmt.close();
				con.close();
			}
			catch(SQLException sqle)
			{
				sqle.printStackTrace();
				(new Platform()).addMessageToLog("SQLException in Station.getAlertFrames: message=" +sqle.getMessage());
			}
			finally
			{
				try
				{
					if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
				}
				catch(SQLException sqle)
				{ 
					(new Platform()).addMessageToLog("SQLException in Station.getAlertFrames: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
				}
			}   
		}
		catch(JSONException jsone)
		{
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return alert_frames_ja;
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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
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
	
	
	private String[] morning_greetings = {"Good morning", "Morning"};
	private String[] afternoon_greetings = {"Good afternoon", "Afternoon"};
	private String[] evening_greetings = {"Good evening", "Evening"};
	private String[] generic_greetings = {"Hello", "Greetings"};
	private String[] objects = {"Lexington", "Bluegrass", "Central Kentucky", "everyone", "folks", "viewers"};
	
	
	String getMessage(String social_type, long timestamp_in_ms, long redirect_id, User reporter)
	{
		String returnval = "";
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
		cal.setTimeInMillis(timestamp_in_ms);
	/*	ArrayList<String> greeting_choices = new ArrayList<String>();
		if(cal.get(Calendar.HOUR_OF_DAY) < 12)
		{	
			for(int x = 0; x < morning_greetings.length; x++)
				greeting_choices.add(morning_greetings[x]);
			for(int x = 0; x < generic_greetings.length; x++)
				greeting_choices.add(generic_greetings[x]);
		}
		else if(cal.get(Calendar.HOUR_OF_DAY) < 18)
		{	
			for(int x = 0; x < afternoon_greetings.length; x++)
				greeting_choices.add(afternoon_greetings[x]);
			for(int x = 0; x < generic_greetings.length; x++)
				greeting_choices.add(generic_greetings[x]);
		}
		else if(cal.get(Calendar.HOUR_OF_DAY) < 24)
		{	
			for(int x = 0; x < evening_greetings.length; x++)
				greeting_choices.add(evening_greetings[x]);
			for(int x = 0; x < generic_greetings.length; x++)
				greeting_choices.add(generic_greetings[x]);
		}
			
		ArrayList<String> object_choices = new ArrayList<String>();
		for(int x = 0; x < objects.length; x++)
			object_choices.add(objects[x]);
		
		Random random = new Random();
		int greetings_index = random.nextInt(greeting_choices.size());
		int objects_index = random.nextInt(object_choices.size());*/
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
			// using display name for now while in test mode. Using @twitterhandle draws too much attention
			//returnval = reporter.getDisplayName() + " is on the air right now (" + ts_string + "). Tune in or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #" + getCallLetters();
			returnval = "@" + reporter.getTwitterHandle() + " is on the air right now (" + ts_string + "). Tune in or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #" + getCallLetters();
		}
		/*int selector = random.nextInt(4);
		
		if(social_type.equals("facebook"))
		{
			if(selector == 0) 
				returnval = getCallLetters().toUpperCase() + " is on. Tune in or watch the live stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " -- " + ts_string;  
			else if (selector == 1) 
				returnval = "The time is " + ts_string + " and we're on RIGHT NOW. Fire up the TV or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id;
			else if (selector == 2) // greeting, "I", "right now", "watch", timestamp last 
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". I am on-air right now. Tune in or watch the live stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " -- " + ts_string;
			else if (selector == 3) // greeting, "I", no "right now", "view", timestamp after greeting 
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". It is " + ts_string + " and I am on-air. Tune in or watch the live stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id;
			else if(selector == 0) // no greeting,  "I", no "right now", "watch", timestamp last
				returnval = "We're streaming live. Tune in! " + getLiveStreamURLAlias() + "?id=" + redirect_id + " -- " + ts_string;  
			else if (selector == 1) // no greeting, "we", "live", "catch" timestamp first
				returnval = "The news is on RIGHT NOW. Catch us here: " + getLiveStreamURLAlias() + "?id=" + redirect_id;
			else if (selector == 2) // greeting, "I", "right now", "watch", timestamp last 
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". I am on-air right now. Tune in or watch the live stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " -- " + ts_string;
			else if (selector == 3) // greeting, "I", no "right now", "view", timestamp after greeting 
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". It is " + ts_string + " and I am on-air. Tune in or watch the live stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id; 
		}
		else if(social_type.equals("twitter"))
		{
			if(selector == 0 || selector == 1)
				returnval = "I'm on the air right now (" + ts_string + "). Tune in or watch the live stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #wkyt";  
			else if(selector == 2 || selector == 3)
				returnval = greeting_choices.get(greetings_index) + ", " + object_choices.get(objects_index) + ". I'm on the air (" + ts_string + "). Tune in or stream here: " + getLiveStreamURLAlias() + "?id=" + redirect_id + " #wkyt";  
		}*/
		return returnval;
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
	}

}
