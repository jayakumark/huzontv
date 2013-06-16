package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Iterator;
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
	
	public Station(String inc_call_letters)
	{
		valid = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM stations WHERE call_letters='" + inc_call_letters + "'");
			if(rs.next())
			{
				call_letters = rs.getString("call_letters");
				city = rs.getString("city");
				state = rs.getString("state");
				dma_2013 = rs.getInt("dma_2013");
				frame_rate = rs.getInt("frame_rate");
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
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Station constructor", "valid set to false. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				e.printStackTrace();
			}
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
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Station constructor", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		}  	
		
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
	
	public TreeSet<Frame> getFrames(long begin_in_ms, long end_in_ms, String designation, double single_modifier_double) // INCLUSIVE
	{
		TreeSet<Frame> returnset = new TreeSet<Frame>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			if(designation == null) // get all frames
			{	
				System.out.println("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")");
				rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + ")"); 
			}
			else if(designation != null) // get all frames where designation is above single thresh
			{	
				User reporter = new User(designation, "designation");
				double homogeneity_double = reporter.getHomogeneity();
				double threshold = homogeneity_double * single_modifier_double;
				System.out.println("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms <= " + end_in_ms + " AND timestamp_in_ms >= " + begin_in_ms + " AND " + designation + "_avg > " + threshold + ")"); 
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
			frames_ja.put(currentframe.getAsJSONObject(get_score_data));
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
			frames_ja.put(currentframe.getAsJSONObject(get_score_data));
		}
		return frames_ja;
	}
	
	JSONArray getAlertFrames(long begin_long, long end_long, int maw_int, double ma_modifier_double, double single_modifier_double, double delta_double)
	{
		System.out.println("Endpoint.getAlertFrames() begin");
		JSONArray alert_frames_ja = new JSONArray();
		JSONObject current_frame_jo = new JSONObject();
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
					con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
					stmt = con.createStatement();
					// get frames where this designation crosses the single frame threshold
					rs = stmt.executeQuery("SELECT * FROM frames_" + getCallLetters() + " WHERE (timestamp_in_ms >= " + (1000*begin_long) + " AND timestamp_in_ms <= " + (1000*end_long) + " AND " + current_designation + "_avg > " + (current_homogeneity * single_modifier_double) + ") ORDER BY timestamp_in_ms ASC");
					rs.last();
					System.out.println("Endpoint.getAlertFrames() found " + rs.getRow()  + " frames over threshold (" + current_homogeneity + " * " + single_modifier_double + "=" +  (current_homogeneity * single_modifier_double) + ") for " + currentreporter.getDesignation());
					rs.beforeFirst();
					TreeSet<Frame> frames_past_single_thresh = getFramesFromResultSet(rs);
					Iterator<Frame> frames_past_single_thresh_it = frames_past_single_thresh.iterator();
					Frame currentframe = null;
					Frame subsequentframe = null;
					Frame frame_that_passed = null;
					double moving_average = 0.0;
					while(frames_past_single_thresh_it.hasNext())
					{
						currentframe = frames_past_single_thresh_it.next();
						System.out.println("Looking up moving average of " + current_designation + " with maw_int=" + maw_int);
						moving_average = currentframe.getMovingAverage(maw_int, current_designation);
						if(moving_average > (current_homogeneity * ma_modifier_double) && 
								moving_average == currentframe.getHighestMovingAverage(maw_int))
						{
							frame_that_passed = currentframe;
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
									frame_that_passed = subsequentframe;
									break;
								}
								else
								{
									System.out.println("Endpoint.gAF(): ma of subsequent DID NOT pass req thresh. ma=" + moving_average + " thresh=" + (current_homogeneity * ma_modifier_double));
								}
							}
							rs2.close();
							stmt2.close();
							if(frame_that_passed != null) 
								break; // get out of the loop
						}
					}
					if(frame_that_passed != null) 
					{
						JSONObject jo2add = currentframe.getAsJSONObject(true);
						jo2add.put("designation", current_designation);
						jo2add.put("ma_for_alert_frame", currentframe.getMovingAverage(maw_int, current_designation));
						jo2add.put("ma_for_frame_that_passed", frame_that_passed.getMovingAverage(maw_int, current_designation));
						jo2add.put("score_for_alert_frame", currentframe.getScore(current_designation));
						jo2add.put("score_for_frame_that_passed", frame_that_passed.getScore(current_designation));
						jo2add.put("homogeneity", current_homogeneity);
						jo2add.put("ma_threshold",  (current_homogeneity * ma_modifier_double));
						jo2add.put("single_threshold", (current_homogeneity * single_modifier_double));
						alert_frames_ja.put(jo2add);
					}
				}
				rs.close();
				stmt.close();
				con.close();
			}
			catch(SQLException sqle)
			{
				sqle.printStackTrace();
				SimpleEmailer se = new SimpleEmailer();
				try {
					se.sendMail("SQLException in Endpoint getAlertFrames", "message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
			finally
			{
				try
				{
					if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
				}
				catch(SQLException sqle)
				{ 
					SimpleEmailer se = new SimpleEmailer();
					try {
						se.sendMail("SQLException in Endpoint getAlertFrames", "Error occurred when closing rs, stmt and con. message=" +sqle.getMessage(), "cyrus7580@gmail.com", "info@huzon.tv");
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
			}   
		}
		catch(JSONException jsone)
		{
			System.out.println("endpoint: JSONException thrown in large try block. " + jsone.getMessage());
		}	
		return alert_frames_ja;
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

}
