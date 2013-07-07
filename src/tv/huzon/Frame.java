package tv.huzon;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.TreeSet;

import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Frame implements Comparable<Frame> {

	long timestamp_in_ms;
	String image_name;
	String s3_location;
	String url;
	int frame_rate;
	String station;
	Station station_object;
	String[] reporter_designations;
	double[] reporter_avgs; // FIXME this should be called reporter_scores
	//JSONArray[] reporter_score_arrays;
	int[] reporter_nums;
	//double[] reporter_ma5s;
	double[] reporter_ma6s;
	
	boolean max_and_second_set;
	String max_ma_designation;
	String second_max_ma_designation;
	double max_ma;
	double second_max_ma;
	
	
	String dbName = System.getProperty("RDS_DB_NAME"); 
	String userName = System.getProperty("RDS_USERNAME"); 
	String password = System.getProperty("RDS_PASSWORD"); 
	String hostname = System.getProperty("RDS_HOSTNAME");
	String port = System.getProperty("RDS_PORT");
	
	//String connectionstring = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + pass word;
	String connectionstring = "jdbc:mysql://aa13frlbuva60me.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com:3306/ebdb?user=huzon&password=cTp88qLkS240y5x";
	
	
	public Frame(long inc_timestamp_in_ms, String inc_image_name, String inc_s3_location,
			String inc_url, int inc_frame_rate, String inc_station, String[] inc_reporter_designations, 
			double[] inc_reporter_avgs, JSONArray[] inc_reporter_score_arrays, int[] inc_reporter_nums, double[] inc_reporter_ma6s)
	{
		timestamp_in_ms = inc_timestamp_in_ms;
		image_name = inc_image_name;
		s3_location = inc_s3_location;
		url = inc_url;
		frame_rate = inc_frame_rate;
		station = inc_station;
		station_object = new Station(station);
		reporter_designations = inc_reporter_designations;
		reporter_avgs = inc_reporter_avgs;
		//reporter_score_arrays = inc_reporter_score_arrays;
		reporter_nums = inc_reporter_nums;
		//reporter_ma5s = inc_reporter_ma5s;
		reporter_ma6s = inc_reporter_ma6s;
		max_and_second_set = false;
	}
	
	
	public Frame(long inc_timestamp_in_ms, String inc_station)
	{
		timestamp_in_ms = inc_timestamp_in_ms;
		station = inc_station;
		station_object = new Station(station);
		max_and_second_set = false;
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection(connectionstring);
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE timestamp_in_ms=" + timestamp_in_ms); // get the frames in the time range
			
			// calculate the number of reporters by looping through all columns and looking for "_avg"
			ResultSetMetaData rsmd = rs.getMetaData();
			int columncount = rsmd.getColumnCount();
			int reportercount = 0;
			int x = 1; 
			while(x <= columncount)
			{
				if(rsmd.getColumnName(x).endsWith("_avg"))
				{
					reportercount++;
				}
				x++;
			}

			// now examine the row and extract all the information
			if(rs.next())
			{	
				image_name = rs.getString("image_name");
				s3_location = rs.getString("s3_location");
				url = rs.getString("url");
				frame_rate = rs.getInt("frame_rate");
				x = 1; 
				int reporter_index = 0;
				reporter_designations = new String[reportercount];
				reporter_avgs = new double[reportercount];
				//reporter_score_arrays = new JSONArray[reportercount];
				reporter_nums = new int[reportercount];
				while(x <= columncount)
				{
					if(rsmd.getColumnName(x).endsWith("_avg"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_avg"));
						reporter_avgs[reporter_index] = rs.getDouble(x);
					}
					else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
						reporter_index++;
					}
					x++;
				}
			}
			else
			{
				timestamp_in_ms = 0;
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
	}
	
	long getTimestampInMillis()
	{
		return timestamp_in_ms;
	}
	
	String getImageName()
	{
		return image_name;
	}

	String getS3Location()
	{
		return s3_location;
	}
	
	String getURLString()
	{
		return url;
	}
	
	URL getURL()
	{
		URL returnurl = null;
		try {
			returnurl = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return returnurl;
	}
	
	String getStation()
	{
		return station;
	}
	
	int getFrameRate()
	{
		return frame_rate;
	}
	
	int getNumReporters()
	{
		return reporter_designations.length;
	}
	
	String[] getReporterDesignations()
	{
		return reporter_designations;
	}
	
	double getScore(String reporter_designation)
	{
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(reporter_designation))
				return reporter_avgs[x];
			x++;
		}
		return 0;
	}
	
	// this function needs to be rewritten to account for the new moving averages colums
	
	double getMovingAverage6(String designation)
	{
		//System.out.print("Frame.getMovingAverage6(): frame=" + getTimestampInMillis() + " designation=" + designation + " ");
		if(reporter_ma6s == null)  // reporter_ma6s was never populated
		{
			return -1;
		}
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
			{
				//System.out.println("ma=" + reporter_ma6s[x]);
				return reporter_ma6s[x];
			}
			x++;
		}
	//	System.out.println("error");
		return -1;
	}
	
	// reporter_ma6s presumed to be set if this is called. Calling functions should be responsible for this.
	private void setMaxAndSecond()
	{
		max_ma = 0;
		second_max_ma = 0;
		max_ma_designation = null;
		second_max_ma_designation = null;
		
		int x = 0;
		
		while(x < reporter_ma6s.length)
		{
			if(reporter_ma6s[x] > max_ma)
			{
				second_max_ma = max_ma;
				max_ma = reporter_ma6s[x];
				second_max_ma_designation = max_ma_designation;
				max_ma_designation = reporter_designations[x];
			}
			x++;
		}
		max_and_second_set = true;
	}
	
	double getHighestMovingAverage()
	{
		if(reporter_ma6s == null)  // reporter_ma6s was never populated
			return -1;
		else if (max_and_second_set == false)
		{
			setMaxAndSecond();
			return max_ma;
		}
		else
			return max_ma;
	}
	
	double getSecondHighestMovingAverage()
	{
		if(reporter_ma6s == null)  // reporter_ma6s was never populated
			return -1;
		else if (max_and_second_set == false)
		{
			setMaxAndSecond();
			return second_max_ma;
		}
		else
			return second_max_ma;
	}
	
	String getHighestMovingAverageDesignation()
	{
		if(reporter_ma6s == null)  // reporter_ma6s was never populated
			return null;
		else if (max_and_second_set == false)
		{
			setMaxAndSecond();
			return max_ma_designation;
		}
		else
			return max_ma_designation;
	}
	
	String getSecondHighestMovingAverageDesignation()
	{
		if(reporter_ma6s == null)  // reporter_ma6s was never populated
			return null;
		else if (max_and_second_set == false)
		{
			setMaxAndSecond();
			return second_max_ma_designation;
		}
		else
			return second_max_ma_designation;
	}
	
	int getNumFramesInWindowAboveSingleThresh(String designation, double single_thresh)
	{
		Station station_object = new Station(station);
		TreeSet<Frame> frames = station_object.getFrames(timestamp_in_ms - (6 * 1000), timestamp_in_ms, designation, 1.0); // FIXME 1.0 as single modifier should not be hardcoded
		Iterator<Frame> it = frames.iterator();
		Frame currentframe = null;
		int returnval = 0;
		while(it.hasNext())
		{
			currentframe = it.next();
			if(currentframe.getDesignationScore(designation) > single_thresh)
				returnval++;
		}
		return returnval;
		
	}
	
	double getDesignationScore(String designation)
	{
		// reporter_avgs[] should always be populated. No need to check here.
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
				return reporter_avgs[x];
			x++;
		}
		return -1;
	}
	
	void updateRowWithMovingAverage6s(String[] reporter_designations, double[] inc_reporter_ma6s)
	{
		// after calculating moving average, update the row with the information
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;		
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM frames_" + station_object.getCallLetters() + " WHERE timestamp_in_ms=" + getTimestampInMillis()); // get the frames in the time range
			if(!rs.next())
			{
				System.out.println("Frame.updateRowWithMovingAverage6s(): ERROR could not find frame in table frames_" + station_object.getCallLetters() + " for timestamp_in_ms=" + getTimestampInMillis());
			}
			else
			{
				for(int y = 0; y < inc_reporter_ma6s.length; y++)
				{
					rs.updateDouble(reporter_designations[y] + "_ma6", inc_reporter_ma6s[y]);
				}
				rs.updateRow();
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
		reporter_ma6s = inc_reporter_ma6s;
	}
	
	// 
	URL[] get2x2CompositeURLs()
	{
		Station station_object = new Station(station);
		TreeSet<Frame> frames_ts = station_object.getFrames(getTimestampInMillis() - 6000, getTimestampInMillis() + 3500, null, -1);
		if(frames_ts.size() < 4) // this should absolutely NEVER happen as this function shouldn't be called for any reason if there are this few frames in the window.
		{						 // at ~9.5 seconds, there should be ~18 frames available
			System.out.println("Frame.get2x2CompositeURLs(): not enough frames in window. Returning null.");
			(new Platform()).addMessageToLog("get2x2: Not enough frames in window. Returning null");
			return null;
		}
		Frame[] frames_array = null;
		URL[] return_urls = new URL[4];
		int numchecks = 0;
		String debug = "";
		long lastframe_ts = 0L;
		
		// loop until the last frame is within 2.5 - 3.5 seconds from the current frame's timestamp (failing after 7 tries) 
		int looplimit = 10;
		while((numchecks == 0  || !((lastframe_ts >= (getTimestampInMillis() + 2500)) && (lastframe_ts <= (getTimestampInMillis() + 3500)))) && numchecks < looplimit) // do up to 10 loops at 500ms sec each (i.e. > 5 seconds) 
		{
			if(numchecks > 0) // on the first go, immediately look for the frames. After that, sleep for a brief period
			{
				try { Thread.sleep(500);} catch (InterruptedException e) { e.printStackTrace(); }
			}
			
			// get the frame objects in the window
			frames_ts = station_object.getFrames(getTimestampInMillis() - 6000, getTimestampInMillis() + 3500, null, -1);
			Frame lastframe = frames_ts.last();
			lastframe_ts = lastframe.getTimestampInMillis();
			//debug = "numchecks=" + numchecks + " numframes=" + frames_ts.size() + " and last=" + lastframe_ts + " target=" + (getTimestampInMillis() + 2500) + "-" + (getTimestampInMillis() + 3500 + " numchecks=" + numchecks);
		 	//(new Platform()).addMessageToLog("get2x2: Looping, looking to satisfy condition. " + debug);
			numchecks++;
		}
		
		if(numchecks == looplimit && !((lastframe_ts >= (getTimestampInMillis() + 2500)) && (lastframe_ts <= (getTimestampInMillis() + 3500)))) // if we hit the loop limit and STILL aren't within the target area, we failed. 
		{
			debug = "numframes=" + frames_ts.size() + " and last=" + lastframe_ts + " target=" + (getTimestampInMillis() + 2500) + "-" + (getTimestampInMillis() + 3500);
			(new Platform()).addMessageToLog("get2x2: Reached end of loop without satisfying future frame condition. Returning null. " + debug);
			return null;
		}
		else // loop broke without reaching limit meaning frames_ts good to go. Get frames_array and create urls array
		{
			// create the frames_array from the frames object treeset
			Iterator<Frame> frames_it = frames_ts.iterator();
			frames_array = new Frame[frames_ts.size()];
			Frame currentframe = null;
			int x = 0;
			while(frames_it.hasNext())
			{
				currentframe = frames_it.next();
				frames_array[x] = currentframe;
				x++;
			}
			return_urls[0] = frames_array[0].getURL();							// get first image
			System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[0] + " from index 0");
			return_urls[1] = frames_array[frames_array.length / 3].getURL();      // get second image approx 1/3 through the array
			System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[1] + " from index " + frames_array.length/3);
			return_urls[2] = frames_array[frames_array.length * 2 / 3].getURL();  // get third image approx 2/3 through the array
			System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[2] + " from index " + frames_array.length*2/3);
			return_urls[3] = frames_array[frames_array.length - 1].getURL();   // get last image
			System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[3] + " from index " + (frames_array.length-1));
		}
		return return_urls;
	}
	
	/* return object as:
	 * 
	 * {
	 * 		image_name : image_name,
	 * 		s3_location: s3_location,
	 * 		...
	 * 		reporters:[
	 * 			designation: {
	 * 				designation: designation,
	 * 				score_avg: score_avg,
	 * 				num: num,
	 * 				moving_average: moving_average,
	 * 				scores: [ score1, score2, .... score3]  // OPTIONAL
	 * 			},
	 * 			designation2: {
	 * 				designation: designation2,
	 * 				score_avg: score_avg,
	 * 				num: num,
	 * 				moving_average: moving_average,
	 * 				scores: [ score1, score2, .... score3] // OPTIONAL
	 * 			},
	 * 			...
	 * 		]
	 *	} 
	 *
	 */
	
	JSONObject getAsJSONObject(boolean get_score_data, String designation)
	{
		JSONObject jo = new JSONObject();
		try
		{
			jo.put("image_name", image_name);
			jo.put("s3_location", s3_location); 
			jo.put("url", url);
			jo.put("timestamp_in_ms", timestamp_in_ms);
			jo.put("frame_rate", frame_rate);
			jo.put("station", station);
			if(designation != null)
			{
				double homogeneity = new User(designation,"designation").getHomogeneity();
				double des_ma = getMovingAverage6(designation);
				//System.out.println("Frame.getAsJSONObject(): a designation=" + designation + " (window=6) was specified by the simulator. Returning specialized information. designation=" + designation + " ma=" + des_ma);
				jo.put("designation", designation);
				jo.put("designation_homogeneity", homogeneity);
				jo.put("designation_moving_average", des_ma);
			}
			//jo.put("highest_designation", getHighestDesig)
			if(get_score_data)
			{	
				int x = 0;
				JSONObject reporter_jo = new JSONObject();
				JSONObject jo2 = null;
				while(x < reporter_designations.length)
				{
					jo2 = new JSONObject();
					jo2.put("designation", reporter_designations[x]);
					jo2.put("score_avg", reporter_avgs[x]);
					jo2.put("num", reporter_nums[x]);
					jo2.put("ma6", reporter_ma6s[x]);
					reporter_jo.put(reporter_designations[x],jo2);
					x++;
				}
				jo.put("reporters", reporter_jo);
			}
		}
		catch(JSONException jsone)
		{
			jsone.printStackTrace();
		}
		return jo;
	}
	
	public int compareTo(Frame o) // this sorts by ts
	{
	    Long othertimestamp = ((Frame)o).getTimestampInMillis();
	    int x = othertimestamp.compareTo(timestamp_in_ms);
	    if(x >= 0) // this is to prevent equals
	    	return -1;
	    else
	    	return 1;
	}
	
	public static void main(String args[])
	{
		Station s = new Station("wkyt");
		TreeSet<Frame> frames = s.getFrames(args[0], args[1], null, -1); // begin, end, no designation, no single thresh
		Iterator<Frame> it = frames.iterator();
		Frame currentframe = null;
		while(it.hasNext())
		{
			currentframe = it.next();
			System.out.println("Looping " + currentframe.getImageName());
			//currentframe.setMovingAveragesForFrame(5);
			currentframe.setMovingAverage6sForFrame();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// these 2 functions below  should be essentially obsolete after working through the backlog. They were built to populate _ma5 and _ma6 columns for the wkyt table from existing raw score data
	// although I guess it could be tweaked for longer moving averages, too
		
		void populateMovingAverage6s()
		{
			reporter_ma6s = new double[reporter_designations.length];
			TreeSet<Frame> window_frames = station_object.getFrames(getTimestampInMillis()-(6 * 1000), getTimestampInMillis(), null, 0);
			int num_frames_in_window = window_frames.size();
			int x = 0;
			if(num_frames_in_window < 6) // not enough frames in window, set all reporter moving averages to -1 so they get put into the database as null
			{
				while(x < reporter_designations.length)
				{
					reporter_ma6s[x] = -1;
					x++;
				}
			}
			else
			{	
				double[] reporter_totals = new double[reporter_designations.length];
				//System.out.println("Frame.populateMovingAverages(): looping " + num_frames_in_window + " frames for totals");
				Iterator<Frame> it = window_frames.iterator();
				Frame currentframe = null;
				while(it.hasNext())
				{
					currentframe = it.next();
					x = 0;
					while(x < reporter_designations.length)
					{
						reporter_totals[x] = reporter_totals[x] + currentframe.getScore(reporter_designations[x]);
						x++;
					}
				}
				
				// then divide the sum designation_avgs by dividing by the number of frames in the window
				x = 0;
				//System.out.println("Frame.populateMovingAverages(): looping reporters to get ma6s from totals");
				while(x < reporter_totals.length)
				{
					reporter_ma6s[x] = reporter_totals[x] / num_frames_in_window;
					x++;
				}
			}
		}
		
		void setMovingAverage6sForFrame() // WILL OVERWRITE EXISTING DATA
		{
			//System.out.println("Frame.setMovingAveragesForFrame(): " + image_name);
			
			populateMovingAverage6s();
			
			ResultSet rs = null;
			Connection con = null;
			Statement stmt = null;		
			try
			{
				con = DriverManager.getConnection(connectionstring);
				stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE timestamp_in_ms=" + timestamp_in_ms); // get the frames in the time range
				if(!rs.next())
				{
					System.out.println("Frame.setMovingAveragesForFrame(): ERROR could not find frame in table frames_" + station + " for timestamp_in_ms=" + timestamp_in_ms);
				}
				else
				{
					for(int x = 0; x < reporter_ma6s.length; x++)
					{
						if(reporter_ma6s[x] == -1)
							rs.updateNull(reporter_designations[x] + "_ma6");
						else
							rs.updateDouble(reporter_designations[x] + "_ma6", reporter_ma6s[x]);
					}
					rs.updateRow();
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
		}
		
	
	
	
	
	
}
