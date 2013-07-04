package tv.huzon;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.mail.MessagingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.DefaultHttpClient;


import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class Platform {

	private TreeSet<Station> stations;
	String dbName = System.getProperty("RDS_DB_NAME"); 
	String userName = System.getProperty("RDS_USERNAME"); 
	String password = System.getProperty("RDS_PASSWORD"); 
	String hostname = System.getProperty("RDS_HOSTNAME");
	String port = System.getProperty("RDS_PORT");
	
	public Platform()
	{
		try {
		        Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		stations = null; 
	}

	public String getAlertMode()
	{
		String returnstring = null;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM global_vars WHERE `k`='alert_mode'");
			if(rs.next())
				returnstring = rs.getString("v");
			else
				addMessageToLog("Platform.getAlertMode: no row found for global_vars key='alert_mode'");
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.getAlertMode: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.getAlertMode: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return returnstring;
	}
	
	public double getSingleModifier()
	{
		String returnstring = null;
		double returndouble = 0.0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM global_vars WHERE `k`='singlemodifier'");
			if(rs.next())
				returnstring = rs.getString("v");
			else
				addMessageToLog("Platform.getSingleModifier: no row found for global_vars key='singlemodifier'");
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.getSingleModifier: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.getSingleModifier: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		returndouble = Double.parseDouble(returnstring);
		return returndouble;
	}
	
	public double getMAModifier()
	{
		String returnstring = null;
		double returndouble = 0.0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM global_vars WHERE `k`='mamodifier'");
			if(rs.next())
				returnstring = rs.getString("v");
			else
				addMessageToLog("Platform.getMAModifier: no row found for global_vars key='mamodifier'");
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.getMAModifier: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.getMAModifier: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		returndouble = Double.parseDouble(returnstring);
		return returndouble;
	}
	
	public int getMAWindow()
	{
		String returnstring = null;
		int returnint = 0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM global_vars WHERE `k`='mawindow'");
			if(rs.next())
				returnstring = rs.getString("v");
			else
				addMessageToLog("Platform.getMAWindow: no row found for global_vars key='mawindow'");
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.getMAWindow: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.getMAWindow: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		returnint = Integer.parseInt(returnstring);
		return returnint;
	}
	
	public int getNRPST() // number required past single threshold
	{
		String returnstring = null;
		int returnint = 0;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM global_vars WHERE `k`='nrpst'");
			if(rs.next())
				returnstring = rs.getString("v");
			else
				addMessageToLog("Platform.getNRPST: no row found for global_vars key='nrpst'");
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.getNRPST: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.getNRPST: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		returnint = Integer.parseInt(returnstring);
		return returnint;
	}
	
	public void addMessageToLog(String message)
	{
		Connection con = null;
		Statement stmt = null;
		try
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeZone(TimeZone.getTimeZone("America/Louisville"));
			long timestamp_in_ms = cal.getTimeInMillis(); // we know that the most recent image has a timestamp of right now. It can't "survive" there for more than a few seconds
			// make the filename human-readable
			String year = new Integer(cal.get(Calendar.YEAR)).toString();
			String month = new Integer(cal.get(Calendar.MONTH) + 1).toString();
			if(month.length() == 1) { month = "0" + month; }
			String day = new Integer(cal.get(Calendar.DAY_OF_MONTH)).toString();
			if(day.length() == 1) { day = "0" + day;} 
			String hour24 = new Integer(cal.get(Calendar.HOUR_OF_DAY)).toString();
			if(hour24.length() == 1) { hour24 = "0" + hour24;} 
			String minute = new Integer(cal.get(Calendar.MINUTE)).toString();
			if(minute.length() == 1) { minute = "0" + minute;} 
			String second = new Integer(cal.get(Calendar.SECOND)).toString();
			if(second.length() == 1) { second = "0" + second;} 
			String ms = new Long(timestamp_in_ms%1000).toString();
			if(ms.length() == 1) { ms = "00" + ms;} 
			if(ms.length() == 2) { ms = "0" + ms;} 
			String hr_timestamp = year + "-" + month + "-" + day + " " + hour24 + ":" + minute + ":" + second + " " + ms;			
			
			System.out.println("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO messages (`timestamp_hr`,`message`) "
	                    + " VALUES('" + hr_timestamp + "','" + message + "')");
			stmt.executeUpdate("INSERT INTO messages (`timestamp_hr`,`message`) "
                    	+ " VALUES('" + hr_timestamp + "','" + message + "')");
		    stmt.close();
		    con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			SimpleEmailer se = new SimpleEmailer();
			try {
				se.sendMail("SQLException in Platform.addMessageToLog(): message=" + sqle.getMessage(), "nt", "cyrus7580@gmail.com", "info@huzon.tv");
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		finally
		{
			try
			{
				if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return;
	}
	
	public boolean populateStations()
	{
		boolean returnval = false;
		stations = new TreeSet<Station>();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement();
			rs = stmt.executeQuery("SELECT call_letters FROM `stations`");
			Station currentstation = null;
			while(rs.next())
			{
				currentstation = new Station(rs.getString("call_letters"));
				stations.add(currentstation);
			}
			rs.close();
			stmt.close();
			con.close();
			returnval = true;
		}
		catch(SQLException sqle) 
		{ 
			addMessageToLog("SQLException in Platform.populateStations: message=" +sqle.getMessage());
			sqle.printStackTrace(); 
		}
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle) { sqle.printStackTrace(); }
		}   
		return returnval;
	}
	
	public JSONArray getStationsAsJSONArray()
	{
		if(stations == null)
		{
			boolean stationpopsuccessful = populateStations();
			if(!stationpopsuccessful)
			{
				System.out.println("Platform.getStationsAsJSONArray(): There was an problem populating the stations value");
				return null;
			}
		}
		
		JSONArray return_ja = new JSONArray();
		if(stations.isEmpty())
			return return_ja;
		else
		{
			Iterator<Station> it = stations.iterator();
			Station currentstation = null;
			JSONObject station_jo = new JSONObject();
			while(it.hasNext())
			{
				currentstation = it.next();
				station_jo = currentstation.getAsJSONObject();
				return_ja.put(station_jo);
			}
			return return_ja;
		}
	}
	

	
	long createAlertInDB(Station station_object, String social_type, String designation, String image_name, User postinguser)
	{
		long returnval = -1L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO alerts (`social_type`,`designation`,`image_url`,`station`,`created_by`) "
	                    + " VALUES('" + social_type + "','" + designation + "','" + image_name + "','" + station_object.getCallLetters() + "','" + postinguser.getDesignation() + "')");
			stmt.executeUpdate("INSERT INTO alerts (`social_type`,`designation`,`image_url`,`station`,`created_by`) "
                    + " VALUES('" + social_type + "','" + designation + "','" + image_name + "','" + station_object.getCallLetters() + "','" + postinguser.getDesignation() + "')",
	                    Statement.RETURN_GENERATED_KEYS);
			
		    rs = stmt.getGeneratedKeys();

		    if (rs.next()) {
		        returnval = rs.getLong(1);
		    } else {
		    	System.out.println("Endpoint.createAlertInDB(): error getting auto_increment value from row just entered.");
		    }
		    rs.close();
		    stmt.close();
		    con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.createAlertInDB: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.createAlertInDB: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return returnval;
	}
	
	boolean updateAlertText(long alert_id_long, String actual_text)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id='" + alert_id_long + "'"); 
			if(rs.next())
			{
				rs.updateString("actual_text", actual_text);
				rs.updateRow();
				returnval = true;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.updateAlertText: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.updateAlertText: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return returnval;
	}

	boolean updateSocialItemID(long alert_id_long, String social_item_id_string)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id='" + alert_id_long + "'"); 
			if(rs.next())
			{
				rs.updateString("social_item_id", social_item_id_string);
				rs.updateRow();
				returnval = true;
			}
			rs.close();
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.updateSocialItemID: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.updateSocialItemID: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return returnval;
	}
	
	
	
	JSONArray getMostRecentAlerts(int num_to_get)
	{
		JSONArray alerts_ja = new JSONArray();
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE deletion_timestamp IS NULL ORDER BY creation_timestamp DESC LIMIT 0," + num_to_get);
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
			addMessageToLog("SQLException in Platform.getMostRecentAlerts: message=" +sqle.getMessage());
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
				addMessageToLog("SQLException in Platform.getMostRecentAlerts: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return alerts_ja;
	}

	boolean putRedirectHitInDB(String station, long alert_id, String referrer, String ip_address, String designation)
	{
		boolean returnval = false;
		Connection con = null;
		Statement stmt = null;
		try
		{
			
			con = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password);
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO redirects_" + station + " (`alert_id`,`referrer`,`ip_address`,`designation`, `station`) " +
					"VALUES('" + alert_id + "','" + referrer + "','" + ip_address + "','" + designation + "','" + station + "')");
			stmt.executeUpdate("INSERT INTO redirects_" + station + " (`alert_id`,`referrer`,`ip_address`,`designation`, `station`) " +
					"VALUES('" + alert_id + "','" + referrer + "','" + ip_address + "','" + designation + "','" + station + "')");
			returnval = true;
			stmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.putRedirectHitInDB: Error putting redirect hit in db. message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("RedirectServlet.putRedirectHitInDB(): Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
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
						rs.getString("url"), rs.getInt("frame_rate"), station, reporter_designations, 
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
	
	public boolean isNumeric(String incoming_string) // only works for whole numbers, positive and negative
	{
		  int x=0;
		  while(x < incoming_string.length())
		  {
			  if((x==0 && incoming_string.substring(0,1).equals("-")) ||  // OK if first element is "-"
					  incoming_string.substring(x,x+1).equals("0") || incoming_string.substring(x,x+1).equals("1") ||
					  incoming_string.substring(x,x+1).equals("2") || incoming_string.substring(x,x+1).equals("3") ||
					  incoming_string.substring(x,x+1).equals("4") || incoming_string.substring(x,x+1).equals("5") ||
					  incoming_string.substring(x,x+1).equals("6") || incoming_string.substring(x,x+1).equals("7") ||
					  incoming_string.substring(x,x+1).equals("8") || incoming_string.substring(x,x+1).equals("9"))
			  {
				  // ok
			  }
			  else
			  {
				  return false;
			  }
			  x++;
		  }
		  return true;
	}	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Platform p = new Platform();
		p.addMessageToLog("test message3");
	}

}
