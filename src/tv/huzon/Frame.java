package tv.huzon;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.mail.MessagingException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Frame implements Comparable<Frame> {

	long timestamp_in_ms;
	String image_name;
	String url;
	int frame_rate;
	String station;
	Station station_object;
	String[] reporter_designations;
	double[] reporter_scores;
	//JSONArray[] reporter_score_arrays;
	//int[] reporter_nums;
	//double[] reporter_ma5s;
	double[] reporter_ma6s;
	double[] reporter_ma5s;
	//double[] reporter_ma4s;
	//double[] reporter_ma3s;
	
	//boolean highest_and_second_highest_values_set;
	boolean has_moving_averages = false;
	String highest_ma6_designation;
	double highest_ma6;
	String highest_ma5_designation;
	double highest_ma5;
	/*String highest_ma4_designation;
	double highest_ma4;
	String highest_ma3_designation;
	double highest_ma3;*/
	String second_highest_ma6_designation;
	double second_highest_ma6;
	String second_highest_ma5_designation;
	double second_highest_ma5;
/*	String second_highest_ma4_designation;
	double second_highest_ma4;
	String second_highest_ma3_designation;
	double second_highest_ma3;*/
	String highest_score_designation;
	double highest_score;
	String second_highest_score_designation;
	double second_highest_score;
	
	TreeSet<Frame> frames_in_window = null; // null = not yet intialized, empty = processed but no frames.
	DataSource datasource;
	
	// to be used when another process has gotten a bunch of rows from the frames table 
	// and we want to build a bunch of frame objects without calling the database a zillion times.
	public Frame(long inc_timestamp_in_ms, String inc_image_name, 
			String inc_url, int inc_frame_rate, String inc_station, String[] inc_reporter_designations, 
			double[] inc_reporter_scores, JSONArray[] inc_reporter_score_arrays, double inc_reporter_ma5s[], double inc_reporter_ma6s[])
	{
		
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		
		timestamp_in_ms = inc_timestamp_in_ms;
		image_name = inc_image_name;
		url = inc_url;
		frame_rate = inc_frame_rate;
		station = inc_station;
		station_object = new Station(station);
		reporter_designations = inc_reporter_designations;
		reporter_scores = inc_reporter_scores;
		//reporter_nums = inc_reporter_nums;
	//	reporter_ma3s = inc_reporter_ma3s;
		//reporter_ma4s = inc_reporter_ma4s;
		reporter_ma5s = inc_reporter_ma5s;
		reporter_ma6s = inc_reporter_ma6s;
		if(reporter_ma6s != null && reporter_ma6s.length > 0 && reporter_ma5s != null && reporter_ma5s.length > 0)
			//&& reporter_ma4s != null && reporter_ma4s.length > 0 && reporter_ma3s != null && reporter_ma3s.length > 0)
		{
			has_moving_averages = true;
			setHighestAndSecondHighestValues();
		}
		
		// set highest and second highest SCORES (does not require moving averages)
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_scores[x] > highest_score)
			{
				second_highest_score = highest_score;
				highest_score = reporter_scores[x];
				second_highest_score_designation = highest_score_designation;
				highest_score_designation = reporter_designations[x];
			}
			else if(reporter_scores[x] > second_highest_score)
			{
				second_highest_score = reporter_scores[x];
				second_highest_score_designation = reporter_designations[x];
			}
			x++;
		}
	}
	
	
	public Frame(long inc_timestamp_in_ms, String inc_station)
	{
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		
		timestamp_in_ms = inc_timestamp_in_ms;
		station = inc_station;
		station_object = new Station(station);
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE timestamp_in_ms=" + timestamp_in_ms); // get the specified (unique) frame from the station's frame table
			
			// calculate the number of reporters by looping through all columns and looking for ""
			ResultSetMetaData rsmd = rs.getMetaData();
			int columncount = rsmd.getColumnCount();
			int reportercount = 0;
			int x = 1; 
			while(x <= columncount)
			{
				if(rsmd.getColumnName(x).endsWith("_score"))
				{
					reportercount++;
				}
				x++;
			}

			// now examine the row and extract all the information
			if(rs.next())
			{	
				image_name = rs.getString("image_name");
				url = rs.getString("url");
				frame_rate = rs.getInt("frame_rate");
				x = 1; 
				int reporter_index = 0;
				reporter_designations = new String[reportercount];
				reporter_scores = new double[reportercount];
				//reporter_ma3s = new double[reportercount];
				//reporter_ma4s = new double[reportercount];
				reporter_ma5s = new double[reportercount];
				reporter_ma6s = new double[reportercount];
				//reporter_score_arrays = new JSONArray[reportercount];
				//reporter_nums = new int[reportercount];
				boolean db_has_ma6_data = true; // assume true until proven false
				boolean db_has_ma5_data = true;
				boolean db_has_ma4_data = true;
				boolean db_has_ma3_data = true;
				while(x <= columncount)
				{
					if(rsmd.getColumnName(x).endsWith("_score"))
					{
						reporter_designations[reporter_index] = rsmd.getColumnName(x).substring(0,rsmd.getColumnName(x).indexOf("_score"));
						reporter_scores[reporter_index] = rs.getDouble(x);
					}
					/*else if(rsmd.getColumnName(x).endsWith("_num"))
					{
						reporter_nums[reporter_index] = rs.getInt(x);
					}*/
					/*else if(rsmd.getColumnName(x).endsWith("_ma3"))
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
					}*/
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
						if(db_has_ma6_data == true) // it could either be true or assumed to be true at this point
						{	
							reporter_ma6s[reporter_index] = rs.getDouble(x); // try to start saving reporter_ma6 data. 
							if (rs.wasNull()) // if that didn't work
							{
								reporter_ma6s = null; // then set the reporter_ma6s array to null to signify that this row doesn't have them. Maybe this row is new and they'll be set later.
								db_has_ma6_data = false;
							}
						}
						// else skip. We already know there is no ma6 data in this row
						reporter_index++; // this is the last column for each reporter in the database, move to next reporter
					}
					x++;
				}
				
				// if there was moving average data, set the flag and set highest and second highest data
				if(reporter_ma6s != null && reporter_ma6s.length > 0 && reporter_ma5s != null && reporter_ma5s.length > 0)
					//&& reporter_ma4s != null && reporter_ma4s.length > 0 && reporter_ma3s != null && reporter_ma3s.length > 0)
				{
					has_moving_averages = true;
					setHighestAndSecondHighestValues();
				}
				
				// set highest and second highest SCORES (does not require moving averages)
				x = 0;
				while(x < reporter_designations.length)
				{
					if(reporter_scores[x] > highest_score)
					{
						second_highest_score = highest_score;
						highest_score = reporter_scores[x];
						second_highest_score_designation = highest_score_designation;
						highest_score_designation = reporter_designations[x];
					}
					else if(reporter_scores[x] > second_highest_score)
					{
						second_highest_score = reporter_scores[x];
						second_highest_score_designation = reporter_designations[x];
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
	
	private void setHighestAndSecondHighestValues() // IS ALWAYS called by Frame constructor and by populateMovingAverages(). That way, we know these are always set properly for subsequent calls. 
	// This function is not very processor or DB intensive
	{
		highest_ma6_designation = null;
		highest_ma6 = -1;
		second_highest_ma6_designation = null;
		second_highest_ma6 = -1;
		
		highest_ma5_designation = null;
		highest_ma5 = -1;
		second_highest_ma5_designation = null;
		second_highest_ma5 = -1;
		
		/*highest_ma4_designation = null;
		highest_ma4 = -1;
		second_highest_ma4_designation = null;
		second_highest_ma4 = -1;
		
		highest_ma3_designation = null;
		highest_ma3 = -1;
		second_highest_ma3_designation = null;
		second_highest_ma3 = -1;*/
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_ma6s[x] > highest_ma6) // is this the highest? if so, bump highest and second highest.
			{
				second_highest_ma6 = highest_ma6;
				highest_ma6 = reporter_ma6s[x];
				second_highest_ma6_designation = highest_ma6_designation;
				highest_ma6_designation = reporter_designations[x];
			}
			else if(reporter_ma6s[x] > second_highest_ma6) // it wasn't the highest, but might be second. If, so, bump the second highest
			{
				second_highest_ma6 = reporter_ma6s[x];
				second_highest_ma6_designation = reporter_designations[x];
			}
			
			if(reporter_ma5s[x] > highest_ma5) // is this the highest? if so, bump highest and second highest.
			{
				second_highest_ma5 = highest_ma5;
				highest_ma5 = reporter_ma5s[x];
				second_highest_ma5_designation = highest_ma5_designation;
				highest_ma5_designation = reporter_designations[x];
			}
			else if(reporter_ma5s[x] > second_highest_ma5) // it wasn't the highest, but might be second. If, so, bump the second highest
			{
				second_highest_ma5 = reporter_ma5s[x];
				second_highest_ma5_designation = reporter_designations[x];
			}
			
			/*if(reporter_ma4s[x] > highest_ma4) // is this the highest? if so, bump highest and second highest.
			{
				second_highest_ma4 = highest_ma4;
				highest_ma4 = reporter_ma4s[x];
				second_highest_ma4_designation = highest_ma4_designation;
				highest_ma4_designation = reporter_designations[x];
			}
			else if(reporter_ma4s[x] > second_highest_ma4) // it wasn't the highest, but might be second. If, so, bump the second highest
			{
				second_highest_ma4 = reporter_ma4s[x];
				second_highest_ma4_designation = reporter_designations[x];
			}
			
			if(reporter_ma3s[x] > highest_ma3) // is this the highest? if so, bump highest and second highest.
			{
				second_highest_ma3 = highest_ma3;
				highest_ma3 = reporter_ma3s[x];
				second_highest_ma3_designation = highest_ma3_designation;
				highest_ma3_designation = reporter_designations[x];
			}
			else if(reporter_ma3s[x] > second_highest_ma3) // it wasn't the highest, but might be second. If, so, bump the second highest
			{
				second_highest_ma3 = reporter_ma3s[x];
				second_highest_ma3_designation = reporter_designations[x];
			}*/
			x++;
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
				return reporter_scores[x];
			x++;
		}
		return 0;
	}
	
/*	double getMovingAverage3(String designation)
	{
		if(reporter_ma3s == null)  // reporter_ma3s was never populated or was invalid
		{
			return -1;
		}
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
			{
				return reporter_ma3s[x];
			}
			x++;
		}
		return -1;
	}
	
	double getMovingAverage4(String designation)
	{
		if(reporter_ma4s == null)  // reporter_ma4s was never populated or was invalid
		{
			return -1;
		}
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
			{
				return reporter_ma4s[x];
			}
			x++;
		}
		return -1;
	}*/
	
	double getMovingAverage5(String designation)
	{
		if(reporter_ma5s == null)  // reporter_ma5s was never populated or was invalid
		{
			return -1;
		}
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
			{
				return reporter_ma5s[x];
			}
			x++;
		}
		return -1;
	}
	
	double getMovingAverage6(String designation)
	{
		if(reporter_ma6s == null)  // reporter_ma6s was never populated or was invalid
		{
			return -1;
		}
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
			{
				return reporter_ma6s[x];
			}
			x++;
		}
		return -1;
	}
	
	
	
	void populateFramesInWindow()
	{
		frames_in_window = station_object.getFrames(timestamp_in_ms - (6 * 1000), timestamp_in_ms, null); 
	}
	
	int getNumFramesInWindowAboveSingleThresh(String designation, double single_thresh)
	{
		if(frames_in_window == null)
			populateFramesInWindow();
		Iterator<Frame> it = frames_in_window.iterator();
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
		// reporter_scores[] should always be populated. No need to check here.
		
		int x = 0;
		while(x < reporter_designations.length)
		{
			if(reporter_designations[x].equals(designation))
				return reporter_scores[x];
			x++;
		}
		return -1;
	}
	// 
	URL[] get2x2CompositeURLs(boolean faces_only, String designation)
	{
		Station station_object = new Station(station);
		TreeSet<Frame> frames_ts = station_object.getFrames(getTimestampInMillis() - 6000, getTimestampInMillis() + 3500, null);
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
			frames_ts = station_object.getFrames(getTimestampInMillis() - 6000, getTimestampInMillis() + 3500, null);
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
			if(faces_only)
			{
				User reporter = new User(designation, "designation");
				 // get first image that contains the reporter's face
				int first_image_index = 0;
				int last_image_index = 0;
				x=0;
				while(x < frames_array.length)
				{
					if(frames_array[x].getHighestScoreDesignation().equals(designation) && frames_array[x].getHighestScore() > (.9 * reporter.getHomogeneity()))
					{
						return_urls[0] = frames_array[x].getURL();
						first_image_index = x;
						break;
					}
					x++;
				}
				
				// get the last image that contains the reporter's face
				x=frames_array.length - 1;
				while(x >= 0)
				{
					if(frames_array[x].getHighestScoreDesignation().equals(designation) && frames_array[x].getHighestScore() > (.9 * reporter.getHomogeneity()))
					{
						return_urls[3] = frames_array[x].getURL();
						last_image_index = x;
						break;
					}
					x--;
				}
				
				int num_frames_between_first_and_last = last_image_index - first_image_index;
				int one_third = num_frames_between_first_and_last / 3;
				
				// now make an assumption that all images inbetween are also faces of the reporter and space the images approximately evenly.
				return_urls[1] = frames_array[first_image_index + one_third].getURL();                                                       // get second image approx 1/3 through the array
				System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[1] + " from index " + (first_image_index + one_third));
				return_urls[2] = frames_array[first_image_index + one_third*2].getURL();                                                   // get third image approx 2/3 through the array
				System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[2] + " from index " + first_image_index + one_third*2);
			}
			else
			{
				return_urls[0] = frames_array[0].getURL();							                                                   // get first image
				System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[0] + " from index 0");
				return_urls[1] = frames_array[frames_array.length / 3].getURL();                                                       // get second image approx 1/3 through the array
				System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[1] + " from index " + frames_array.length/3);
				return_urls[2] = frames_array[frames_array.length * 2 / 3].getURL();                                                   // get third image approx 2/3 through the array
				System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[2] + " from index " + frames_array.length*2/3);
				return_urls[3] = frames_array[frames_array.length - 1].getURL();                                                       // get last image
				System.out.println("Frame.get2x2CompositeURLs(): adding" + return_urls[3] + " from index " + (frames_array.length-1));
			}
		}
		return return_urls;
	}
		
	/* return object as:
	 * 
	 * {
	 * 		image_name : image_name,
	 * 		...
	 * 		reporters:[
	 * 			designation: {
	 * 				designation: designation,
	 * 				score: score,
	 * 				num: num,
	 * 				moving_average: moving_average,
	 * 				scores: [ score1, score2, .... score3]  // OPTIONAL
	 * 			},
	 * 			designation2: {
	 * 				designation: designation2,
	 * 				score: score,
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
					//System.out.print("reporter_scores[" + x + "]=" + reporter_scores[x] + "reporter_ma5s[" + x + "]=" + reporter_ma5s[x] + "reporter_ma6s[" + x + "]=" + reporter_ma6s[x]);
					//System.out.print(" reporter_ma5s[" + x + "]=" + reporter_ma5s[x]);
					//System.out.println(" reporter_ma6s[" + x + "]=" + reporter_ma6s[x]);
					jo2 = new JSONObject();
					jo2.put("designation", reporter_designations[x]);
					jo2.put("score", reporter_scores[x]);
					if(reporter_ma5s == null) 
						jo2.put("ma5", 0);
					else
						jo2.put("ma5", reporter_ma5s[x]);
					if(reporter_ma6s == null) 
						jo2.put("ma6", 0);
					else
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
		TreeSet<Frame> frames = s.getFrames(args[0], args[1], null); // begin, end, no designation, no single thresh
		Iterator<Frame> it = frames.iterator();
		Frame currentframe = null;
		while(it.hasNext())
		{
			currentframe = it.next();
			System.out.println("Looping " + currentframe.getImageName());
			//currentframe.setMovingAveragesForFrame(5);
			//currentframe.setMovingAverage6sForFrame();
		}
	}
	
	
	// this function assumes that all attempts to fill reporter_ma6s have been made. It checks to see if reporters_ma6s is null. If so (which means not enough frames or no data), 
	// it just skips.
	public JSONObject process(
			double ma_modifier_double, 
			int nrpst_int, 
			double delta_double, 
			String which_timers, // test or production
			String alert_mode, // live, test (to master account) or silent 
			int tw_wp_override_in_sec, // use these to override the waiting periods for each reporter as they exist in the database.
			int fb_wp_override_in_sec,  // if -1, then use the database values
			int maw_int
			)
	{
		JSONObject return_jo = new JSONObject();
		boolean alert_triggered = false;
		String alert_triggered_failure_message = "";
		boolean twitter_triggered = false;
		boolean facebook_triggered = false;
		boolean twitter_individual_successful = false;
		boolean facebook_individual_successful = false;
		String twitter_individual_failure_message = "";
		String facebook_individual_failure_message = "";
		boolean twitter_master_successful = false;
		boolean facebook_master_successful = false;
		String twitter_master_failure_message = "";
		String facebook_master_failure_message = "";
		
		 double[] reporter_mas = null;
		 double highest_ma = 0;
		 String highest_ma_designation = "";
		 double second_highest_ma = 0;
		 String second_highest_ma_designation = "";
		 switch (maw_int) {
		 /*	case 3:  
		 		reporter_mas = reporter_ma3s;
		 		break;
			case 4: 
				reporter_mas = reporter_ma4s;
	 			break;*/
			case 5:
				reporter_mas = reporter_ma5s;
	 			break;
			case 6:  
				reporter_mas = reporter_ma6s;
	 			break;
			default: 
				System.out.println("Frame.process():error: maw_int has to be 5 or 6. It is " + maw_int);
				reporter_mas = reporter_ma6s;
	 			break;
		 }
		
		try
		{
			// FIRST, check that this row has analyzable data
			if(reporter_mas == null)
			{
				System.out.println("Frame.process(): (1) skipping timestamp " + getTimestampInMillis() + " because reporter_mas was null (prob not enough frames in window). May be the beginning of a recording or after a stutter. Ignoring.");
				alert_triggered_failure_message = "Reporter_mas was null. No valid data to analyze. (Might just be the beginning or a recording or a temp stutter.)";
			}
			else
			{	
				 switch (maw_int) {
				 /*	case 3:  
				 		highest_ma = highest_ma3; highest_ma_designation = highest_ma3_designation; second_highest_ma = second_highest_ma3; second_highest_ma_designation = second_highest_ma3_designation;
				 		break;
					case 4: 
						highest_ma = highest_ma4; highest_ma_designation = highest_ma4_designation; second_highest_ma = second_highest_ma4; second_highest_ma_designation = second_highest_ma4_designation;
			 			break;*/
					case 5:
						highest_ma = highest_ma5; highest_ma_designation = highest_ma5_designation; second_highest_ma = second_highest_ma5; second_highest_ma_designation = second_highest_ma5_designation;
			 			break;
					case 6:  
						highest_ma = highest_ma6; highest_ma_designation = highest_ma6_designation; second_highest_ma = second_highest_ma6; second_highest_ma_designation = second_highest_ma6_designation;
			 			break;
			 		default:
			 			System.out.println("Frame.process(): error: maw_int has to be 3,4,5 or 6. It is " + maw_int);
			 			break;
				 }
				
				System.out.println("Frame.process(): Analyzing frame for " + highest_ma_designation + " which had highest_ma==" + highest_ma);
				// SECOND, check to see that highest_ma passes the smell test threshold of .4
				if(highest_ma < .4)
				{	
					System.out.println("Frame.process(): (2) skipping timestamp " + getTimestampInMillis() + " because the highest ma was less than .4 (highest_ma=" + highest_ma + ")");
					alert_triggered_failure_message = "highest_ma was less than smell test threshold of .4";
				}
				else
				{	
					// THIRD, check that the highest MA designation was also the highest score designation
					if(!highest_ma_designation.equals(highest_score_designation))
					{	
						System.out.println("Frame.process(): (3) skipping timestamp " + getTimestampInMillis() + " because highest_ma_designation=" + highest_ma_designation + " was not the same as highest_score_designation=" + highest_score_designation);
						alert_triggered_failure_message = "highest_score and highest_ma were different";
					}
					else
					{	
						// FOURTH, delta check
						
						if(((highest_score - second_highest_score) < delta_double) || ((highest_ma - second_highest_ma) < delta_double))
						{
							System.out.println("Frame.process(): (4) skipping timestamp " + getTimestampInMillis() + ", delta failure. hs=" + highest_score + " shs=" + second_highest_score + " hma=" + highest_ma + " shma=" + second_highest_ma + " delta=" + delta_double);
							alert_triggered_failure_message = "Highest score - second highest score < delta";
						}
						else
						{	
							User reporter = new User(highest_ma_designation, "designation");
							double homogeneity = reporter.getHomogeneity();
							double ma_threshold = homogeneity * ma_modifier_double;
							// FIFTH does the highest reporter pass the ma_threshold?
							// (The reason for doing this check #5 in addition to #2 is that by delaying the ma_threshold calculation, we don't have to create a reporter object (and hit the db) for steps 2-4 (which the majority of frames never pass))
							if(highest_ma < ma_threshold)
							{
								System.out.println("Frame.process(): (5) skipping timestamp " + getTimestampInMillis() + " for " + highest_ma_designation + " because it does not pass the full ma threshold");
								alert_triggered_failure_message = "highest_ma was less than the full ma threshold of " + ma_threshold;
							}
							else
							{
								// SIXTH, is the highest reporter within the waiting period for both social outlets? If so skip, if not, continue processing.
								if(reporter.isWithinFacebookWindow(getTimestampInMillis(), which_timers, fb_wp_override_in_sec) && reporter.isWithinTwitterWindow(getTimestampInMillis(), which_timers, tw_wp_override_in_sec))
								{
									System.out.println("Frame.process(): (6) skipping timestamp " + getTimestampInMillis() + " for " + highest_ma_designation + " because it's within the last alert window of both Facebook and Twitter");
									alert_triggered_failure_message = "Reporter is on FB and TW cooldown. (This does not mean an alert would have necessarily triggered.)";
								}
								else
								{
									if(frames_in_window == null)
										populateFramesInWindow();
									// SEVENTH, are there enough frames in the window above the single threshold?
									// this check comes as late as possible to avoid going to the database for frames
									//TreeSet<Frame> frames_in_window_above_single_thresh = station_object.getFrames(getTimestampInMillis() - 6000, getTimestampInMillis(), highest_ma6_designation);
									int npst = getNumFramesInWindowAboveSingleThresh(highest_ma_designation, reporter.getHomogeneity());
									if(npst < nrpst_int) // number past single thresh < number REQUIRED past single thresh
									{
										System.out.println("Frame.process(): (7) skipping timestamp " + getTimestampInMillis() + " because not enough frames in the window were above the single thresh.");
										alert_triggered_failure_message = "Face did not surpass the single thresh of " + nrpst_int + " frames in the window";
									}
									else
									{	
										// EIGHTH, is the highest reporter within its expected time boundaries?
										if(!reporter.isWithinExpectedTimeBoundaries(timestamp_in_ms, station_object))
										{
											System.out.println("Frame.process(): (8) skipping timestamp " + getTimestampInMillis() + " for " + highest_ma_designation + " because the reporter is outside expected time boundaries");
											alert_triggered_failure_message = "Reporter outside expected time boundaries.";
											// temporary dev alert
											SimpleEmailer se = new SimpleEmailer();
											try
											{
												se.sendMail("Alert suppressed for " + highest_ma_designation + ". Outside expected time boundaries.", "nt", "cyrus7580@gmail.com", "info@huzon.tv");
											}
											catch (MessagingException e) 
											{
												e.printStackTrace();
											}
										}
										else
										{
											System.out.println("Frame.process(): (!) adding (not skipping) timestamp " + getTimestampInMillis());
											long trigger_timestamp_in_ms = timestamp_in_ms;
											double trigger_score = getScore(highest_ma_designation);
											int trigger_maw_int = maw_int;
											double trigger_ma5 = getMovingAverage5(highest_ma_designation);
											double trigger_ma6 = getMovingAverage6(highest_ma_designation);
											int trigger_numframes = frames_in_window.size();
											double trigger_delta = highest_score - second_highest_score;
											int trigger_npst = npst;
											
											
											return_jo = getAsJSONObject(true, null); // no designation specified
											return_jo.put("designation", highest_ma_designation);
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
											
											alert_triggered = true; // <---------------
											ExecutorService executor = Executors.newFixedThreadPool(300);
											Future<JSONObject> twittertask_individual = null;
											Future<JSONObject> facebooktask_individual = null;
											Future<JSONObject> twittertask_master = null;
											Future<JSONObject> facebooktask_master = null;
											
											if(!reporter.isWithinTwitterWindow(getTimestampInMillis(),which_timers, tw_wp_override_in_sec))
											{	
												twitter_triggered = true; // <---------------
												reporter.setLastAlert(getTimestampInMillis(), "twitter", which_timers); // set last alert regardless of credentials, successful posting or twitter_active
											} 
											
											if(!reporter.isWithinFacebookWindow(getTimestampInMillis(), which_timers, fb_wp_override_in_sec))
											{
												facebook_triggered = true; // <---------------
												reporter.setLastAlert(getTimestampInMillis(), "facebook", which_timers); // set last alert regardless of credentials, successful posting or facebook_active
											} 
											
											
											
											if(alert_mode.equals("live") || alert_mode.equals("test"))
											{
												if(twitter_triggered)
												{
													// can't set lock here because this is asynchronous, lock would immediately be unlocked on the other side of this next call
													if(station_object.isTwitterActiveIndividual())
														twittertask_individual = executor.submit(new SocialUploaderCallable(this, reporter, station_object, "twitter", "individual", 
																trigger_timestamp_in_ms, trigger_score, trigger_maw_int, trigger_ma5, trigger_ma6, trigger_numframes, trigger_delta, trigger_npst));
																
													else
														(new Platform()).addMessageToLog("Twitter task skipped because station is not twitter_active_individual");
													
													if(station_object.isTwitterActiveMaster())
														twittertask_master = executor.submit(new SocialUploaderCallable(this, reporter, station_object, "twitter", "master", 
																trigger_timestamp_in_ms, trigger_score, trigger_maw_int, trigger_ma5, trigger_ma6, trigger_numframes, trigger_delta, trigger_npst));
													else
														(new Platform()).addMessageToLog("Twitter task skipped because station is not twitter_active_master");
												}
												if(facebook_triggered)
												{
													// can't set lock here because this is asynchronous, lock would immediately be unlocked on the other side of this next call
													if(station_object.isFacebookActiveIndividual())
														facebooktask_individual = executor.submit(new SocialUploaderCallable(this, reporter, station_object, "facebook", "individual", 
																trigger_timestamp_in_ms, trigger_score, trigger_maw_int, trigger_ma5, trigger_ma6, trigger_numframes, trigger_delta, trigger_npst));
													else
														(new Platform()).addMessageToLog("Facebook task skipped because station is not facebook_active_individual");
													
													if(station_object.isFacebookActiveMaster())
														facebooktask_master = executor.submit(new SocialUploaderCallable(this, reporter, station_object, "facebook", "master", 
																trigger_timestamp_in_ms, trigger_score, trigger_maw_int, trigger_ma5, trigger_ma6, trigger_numframes, trigger_delta, trigger_npst));
													else
														(new Platform()).addMessageToLog("Facebook task skipped because station is not facebook_active_master");
												}
											}
											// else if simulation do do not perform any actual twitter or facebook postings.
											
											// CHECK THE RESULTS OF THE CALLABLE THREADS
											JSONObject twittertask_individual_jo = null;
											JSONObject facebooktask_individual_jo = null;
											if(twittertask_individual != null) 			// if twittertask==null, it was never initialized, twitter_triggered stays false and we don't need twitter_successful or twitter_failure_message just stay
											{
												twittertask_individual_jo = twittertask_individual.get();
												twitter_individual_successful = twittertask_individual_jo.getBoolean("twitter_successful");  // returning from social uploader, there is no "individual" or "master"
												if(!twitter_individual_successful)																// only need failure message if twitter not successful
													twitter_individual_failure_message = twittertask_individual_jo.getString("twitter_failure_message"); // returning from social uploader, there is no "individual" or "master"
											}
											if(facebooktask_individual != null)
											{
												facebooktask_individual_jo = facebooktask_individual.get();
												facebook_individual_successful = facebooktask_individual_jo.getBoolean("facebook_successful"); // returning from social uploader, there is no "individual" or "master"
												if(!facebook_individual_successful)
													facebook_individual_failure_message = facebooktask_individual_jo.getString("facebook_failure_message"); // returning from social uploader, there is no "individual" or "master"
											}
											
											JSONObject twittertask_master_jo = null;
											JSONObject facebooktask_master_jo = null;
											if(twittertask_master != null) 			// if twittertask==null, it was never initialized, twitter_triggered stays false and we don't need twitter_successful or twitter_failure_message just stay
											{
												twittertask_master_jo = twittertask_master.get();
												twitter_master_successful = twittertask_master_jo.getBoolean("twitter_successful");  // returning from social uploader, there is no "master" or "master"
												if(!twitter_master_successful)																// only need failure message if twitter not successful
													twitter_master_failure_message = twittertask_master_jo.getString("twitter_failure_message"); // returning from social uploader, there is no "master" or "master"
											}
											if(facebooktask_master != null)
											{
												facebooktask_master_jo = facebooktask_master.get();
												facebook_master_successful = facebooktask_master_jo.getBoolean("facebook_successful"); // returning from social uploader, there is no "master" or "master"
												if(!facebook_master_successful)
													facebook_master_failure_message = facebooktask_master_jo.getString("facebook_failure_message"); // returning from social uploader, there is no "master" or "master"
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			return_jo.put("alert_triggered", alert_triggered);
			if(alert_triggered)
			{
				return_jo.put("twitter_triggered", twitter_triggered);
				if(twitter_triggered)
				{
					return_jo.put("twitter_individual_successful", twitter_individual_successful);
					if(!twitter_individual_successful)
						return_jo.put("twitter_individual_failure_message", twitter_individual_failure_message);
					return_jo.put("twitter_master_successful", twitter_master_successful);
					if(!twitter_master_successful)
						return_jo.put("twitter_master_failure_message", twitter_master_failure_message);
				}
				
				return_jo.put("facebook_triggered", facebook_triggered);
				if(facebook_triggered)
				{	
					return_jo.put("facebook_individual_successful", facebook_individual_successful);
					if(!facebook_individual_successful)
						return_jo.put("facebook_individual_failure_message", facebook_individual_failure_message);
					return_jo.put("facebook_master_successful", facebook_master_successful);
					if(!facebook_master_successful)
						return_jo.put("facebook_master_failure_message", facebook_master_failure_message);
				}
			}
			else
			{
				return_jo.put("alert_triggered_failure_message", alert_triggered_failure_message);
				/*if(!a_designation_passed_ma_thresh_and_was_highest)
					alert_triggered_failure_message = "None of the designations passed the ma threshold";
				else // no alert triggered yet a designation passed the ma thresh... that means that the designation didn't pass single thresh
					alert_triggered_failure_message = "A designation passed ma thresh and was highest, but didn't pass single thresh for any of the frames in the window.";
				return_jo.put("alert_triggered_failure_message", alert_triggered_failure_message);*/
			}
		}
		catch(JSONException jsone)
		{
			jsone.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return return_jo;
	}

	// Explanation for the two functions below:
		// When a new frame comes into the backend, it is inserted first WITHOUT moving average calculations. 
		// As soon as the raw data has been inserted, a subsequent call to calculateAndSetMA6s() does the necessary fill-in.
		// populateMovingAverage6s() does the actual calculations, where calculateAndSetMA6s() is really just a database update call
	
	private void populateMovingAverages()
	{
		reporter_ma6s = new double[reporter_designations.length];
		reporter_ma5s = new double[reporter_designations.length];
		//reporter_ma4s = new double[reporter_designations.length];
		//reporter_ma3s = new double[reporter_designations.length];
		
		long ma6_window_begin_ts = getTimestampInMillis()-(6 * 1000);
		long ma5_window_begin_ts = getTimestampInMillis()-(5 * 1000);
		long ma4_window_begin_ts = getTimestampInMillis()-(4 * 1000);
		long ma3_window_begin_ts = getTimestampInMillis()-(3 * 1000);
		
		TreeSet<Frame> ma6_window_frames = station_object.getFrames(ma6_window_begin_ts, getTimestampInMillis(), null);
		TreeSet<Frame> ma5_window_frames = new TreeSet<Frame>();
		TreeSet<Frame> ma4_window_frames = new TreeSet<Frame>();
		TreeSet<Frame> ma3_window_frames = new TreeSet<Frame>();
		
		Iterator<Frame> it0 = ma6_window_frames.iterator();
		Frame currentframe = null;
		while(it0.hasNext())
		{
			currentframe = it0.next();
			if(currentframe.getTimestampInMillis() > ma5_window_begin_ts && currentframe.getTimestampInMillis() <= getTimestampInMillis())
				ma5_window_frames.add(currentframe);
			if(currentframe.getTimestampInMillis() > ma4_window_begin_ts && currentframe.getTimestampInMillis() <= getTimestampInMillis())
				ma4_window_frames.add(currentframe);
			if(currentframe.getTimestampInMillis() > ma3_window_begin_ts && currentframe.getTimestampInMillis() <= getTimestampInMillis())
				ma3_window_frames.add(currentframe);
		}
		
		int num_frames_in_m6_window = ma6_window_frames.size();
		int num_frames_in_m5_window = ma5_window_frames.size();
		//int num_frames_in_m4_window = ma4_window_frames.size();
		//int num_frames_in_m3_window = ma3_window_frames.size();
		
		int x = 0;
		
		if(num_frames_in_m6_window < 6) // not enough frames in window, set all reporter moving averages to -1 so they get put into the database as null
		{
			reporter_ma6s = null; // just set it to null
		}
		else
		{	
			double[] reporter_totals_6 = new double[reporter_designations.length];
			Iterator<Frame> it6 = ma6_window_frames.iterator();
			while(it6.hasNext())
			{
				currentframe = it6.next();
				x = 0;
				while(x < reporter_designations.length)
				{
					reporter_totals_6[x] = reporter_totals_6[x] + currentframe.getScore(reporter_designations[x]);
					x++;
				}
			}
			
			x = 0;
			while(x < reporter_totals_6.length)
			{
				reporter_ma6s[x] = reporter_totals_6[x] / num_frames_in_m6_window;
				x++;
			}
		}
		
		if(num_frames_in_m5_window < 5) // not enough frames in window, set all reporter moving averages to -1 so they get put into the database as null
		{
			reporter_ma5s = null; // just set it to null
		}
		else
		{	
			double[] reporter_totals_5 = new double[reporter_designations.length];
			Iterator<Frame> it5 = ma5_window_frames.iterator();
			while(it5.hasNext())
			{
				currentframe = it5.next();
				x = 0;
				while(x < reporter_designations.length)
				{
					reporter_totals_5[x] = reporter_totals_5[x] + currentframe.getScore(reporter_designations[x]);
					x++;
				}
			}
			
			x = 0;
			while(x < reporter_totals_5.length)
			{
				reporter_ma5s[x] = reporter_totals_5[x] / num_frames_in_m5_window;
				x++;
			}
		}
		
	/*	if(num_frames_in_m4_window < 4) // not enough frames in window, set all reporter moving averages to -1 so they get put into the database as null
		{
			reporter_ma4s = null; // just set it to null
		}
		else
		{	
			double[] reporter_totals_4 = new double[reporter_designations.length];
			Iterator<Frame> it4 = ma4_window_frames.iterator();
			while(it4.hasNext())
			{
				currentframe = it4.next();
				x = 0;
				while(x < reporter_designations.length)
				{
					reporter_totals_4[x] = reporter_totals_4[x] + currentframe.getScore(reporter_designations[x]);
					x++;
				}
			}
			
			x = 0;
			while(x < reporter_totals_4.length)
			{
				reporter_ma4s[x] = reporter_totals_4[x] / num_frames_in_m4_window;
				x++;
			}
		}
		
		if(num_frames_in_m3_window < 3) // not enough frames in window, set all reporter moving averages to -1 so they get put into the database as null
		{
			reporter_ma3s = null; // just set it to null
		}
		else
		{	
			double[] reporter_totals_3 = new double[reporter_designations.length];
			Iterator<Frame> it3 = ma3_window_frames.iterator();
			while(it3.hasNext())
			{
				currentframe = it3.next();
				x = 0;
				while(x < reporter_designations.length)
				{
					reporter_totals_3[x] = reporter_totals_3[x] + currentframe.getScore(reporter_designations[x]);
					x++;
				}
			}
			
			x = 0;
			while(x < reporter_totals_3.length)
			{
				reporter_ma3s[x] = reporter_totals_3[x] / num_frames_in_m3_window;
				x++;
			}
		}*/
		
		// now that this object has moving average data, set the highest and second_highest ma values and designations
		if(reporter_ma6s != null && reporter_ma6s.length > 0 && reporter_ma5s != null && reporter_ma5s.length > 0)
			//&& reporter_ma4s != null && reporter_ma4s.length > 0 && reporter_ma3s != null && reporter_ma3s.length > 0)
		{
			has_moving_averages = true;
			setHighestAndSecondHighestValues();
		}
		
	}
	
	public void calculateAndSetMAs() // WILL OVERWRITE EXISTING DATA
	{
		//System.out.println("Frame.setMovingAveragesForFrame(): " + image_name);
		
		populateMovingAverages();
		
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;		
		try
		{
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM frames_" + station + " WHERE timestamp_in_ms=" + timestamp_in_ms); // get the frames in the time range
			if(!rs.next())
			{
				System.out.println("Frame.setMovingAveragesForFrame(): ERROR could not find frame in table frames_" + station + " for timestamp_in_ms=" + timestamp_in_ms);
			}
			else
			{
				for(int x = 0; x < reporter_designations.length; x++)
				{
					if(reporter_ma6s == null)
						rs.updateNull(reporter_designations[x] + "_ma6");
					else
						rs.updateDouble(reporter_designations[x] + "_ma6", reporter_ma6s[x]);
					
					if(reporter_ma5s == null)
						rs.updateNull(reporter_designations[x] + "_ma5");
					else
						rs.updateDouble(reporter_designations[x] + "_ma5", reporter_ma5s[x]);
					
					/*if(reporter_ma4s == null)
						rs.updateNull(reporter_designations[x] + "_ma4");
					else
						rs.updateDouble(reporter_designations[x] + "_ma4", reporter_ma4s[x]);
					
					if(reporter_ma3s == null)
						rs.updateNull(reporter_designations[x] + "_ma3");
					else
						rs.updateDouble(reporter_designations[x] + "_ma3", reporter_ma3s[x]);*/
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
		
		
		
	double getHighestScore()
	{
		return highest_score;
	}
	
	double getSecondHighestScore()
	{
		return second_highest_score;
	}
	
	String getHighestScoreDesignation()
	{
		return highest_score_designation;
	}
	
	String getSecondHighestScoreDesignation()
	{
		return second_highest_score_designation;
	}
	
	double getHighestMA6()
	{
		if(!has_moving_averages)
			return -1;
		else
			return highest_ma6;
	}
	
	double getSecondHighestMA6()
	{
		if(!has_moving_averages)
			return -1;
		else
			return second_highest_ma6;
	}
	
	String getHighestMA6Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return highest_ma6_designation;
	}
	
	String getSecondHighestMA6Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return second_highest_ma6_designation;
	}
	
	double getHighestMA5()
	{
		if(!has_moving_averages)
			return -1;
		else
			return highest_ma5;
	}
	
	double getSecondHighestMA5()
	{
		if(!has_moving_averages)
			return -1;
		else
			return second_highest_ma5;
	}
	
	String getHighestMA5Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return highest_ma5_designation;
	}
	
	String getSecondHighestMA5Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return second_highest_ma5_designation;
	}
	
/*	double getHighestMA4()
	{
		if(!has_moving_averages)
			return -1;
		else
			return highest_ma4;
	}
	
	double getSecondHighestMA4()
	{
		if(!has_moving_averages)
			return -1;
		else
			return second_highest_ma4;
	}
	
	String getHighestMA4Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return highest_ma4_designation;
	}
	
	String getSecondHighestMA4Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return second_highest_ma4_designation;
	}
	
	double getHighestMA3()
	{
		if(!has_moving_averages)
			return -1;
		else
			return highest_ma3;
	}
	
	double getSecondHighestMA3()
	{
		if(!has_moving_averages)
			return -1;
		else
			return second_highest_ma3;
	}
	
	String getHighestMA3Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return highest_ma3_designation;
	}
	
	String getSecondHighestMA3Designation()
	{
		if(!has_moving_averages)
			return null;
		else
			return second_highest_ma3_designation;
	}*/
}
