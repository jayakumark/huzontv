package tv.huzon;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.mail.MessagingException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

public class Platform {

	private TreeSet<Station> stations;
	
	DataSource datasource;
	public Platform()
	{
		try {
			Context envCtx = (Context) new InitialContext().lookup("java:comp/env");
			datasource = (DataSource) envCtx.lookup("jdbc/huzondb");
		}
		catch (NamingException e) {
			e.printStackTrace();
		}
		stations = null; 
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
			String timestamp_hr = year  + month + day + "_" + hour24 + minute + second + "_" + ms;			
			
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			//System.out.println("INSERT INTO messages (`timestamp_hr`,`message`) "
	           //         + " VALUES('" + hr_timestamp + "','" + message + "')");
			stmt.executeUpdate("INSERT INTO messages (`timestamp_hr`,`message`) "
                    	+ " VALUES('" + timestamp_hr + "','" + message + "')");
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
			con = datasource.getConnection();
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
			Calendar cal = Calendar.getInstance();
			long timestamp_in_ms = cal.getTimeInMillis();
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
			String timestamp_hr = year  + month + day + "_" + hour24 + minute + second + "_" + ms;	
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO alerts (`timestamp_in_ms`, `timestamp_hr`, `social_type`,`designation`,`image_url`,`station`,`created_by`) "
	                    + " VALUES('" + timestamp_in_ms + "','" + timestamp_hr + "','" + social_type + "','" + designation + "','" + image_name + "','" + station_object.getCallLetters() + "','" + postinguser.getDesignation() + "')");
			stmt.executeUpdate("INSERT INTO alerts (`timestamp_in_ms`, `timestamp_hr`, `social_type`,`designation`,`image_url`,`station`,`created_by`) "
	                    + " VALUES('" + timestamp_in_ms + "','" + timestamp_hr + "','" + social_type + "','" + designation + "','" + image_name + "','" + station_object.getCallLetters() + "','" + postinguser.getDesignation() + "')",
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
			con = datasource.getConnection();
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
			con = datasource.getConnection();
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
	
	boolean updateRedirectUltimateDestinationValue(String station, long redirect_id, String ultimate_destination)
	{
		boolean returnval = false;
		Connection con = null;
		PreparedStatement pstmt = null;
		String updateString = null;
		try
		{
			con = datasource.getConnection();
			// only update records where the ultimate_destination is not set. Otherwise, the original value could get wiped out.
			updateString = "UPDATE redirects_" + station + " SET `ultimate_destination`='" + ultimate_destination + "' WHERE (id='" + redirect_id + "' AND ultimate_destination='')"; 
			pstmt = con.prepareStatement(updateString);
			pstmt.executeUpdate();
			if(pstmt.getUpdateCount() == 1) // the update actually occurred
				returnval = true; 
			else
			{
				addMessageToLog("Platform.updateRedirectUltimateDestinationValue(): Tried to set redirect ultimate destination to " + ultimate_destination + " but failed.");
			}
			pstmt.close();
			con.close();
		}
		catch(SQLException sqle)
		{
			sqle.printStackTrace();
			addMessageToLog("SQLException in Platform.updateRedirectUltimateDestinationValue(): message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (pstmt != null) { pstmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				addMessageToLog("SQLException in Platform.updateRedirectUltimateDestinationValue(): Error occurred when closing con. message=" +sqle.getMessage());
			}
		}   		
		return returnval;
	}
	
	long putRedirectHitInDB(String station, long alert_id, String referrer, String user_agent, String ip_address, String designation)
	{
		long returnval = -1L;
		Connection con = null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			Calendar cal = Calendar.getInstance();
			long timestamp_in_ms = cal.getTimeInMillis();
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
			String timestamp_hr = year  + month + day + "_" + hour24 + minute + second + "_" + ms;	
			
			con = datasource.getConnection();
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO redirects_" + station + " (`timestamp_in_ms`, `timestamp_hr`, `alert_id`,`referrer`,`user_agent`,`ip_address`,`designation`, `station`) " +
					"VALUES('" + timestamp_in_ms + "','" + timestamp_hr + "','" + alert_id + "','" + referrer + "','" + user_agent + "','" + ip_address + "','" + designation + "','" + station + "')");
			stmt.executeUpdate("INSERT INTO redirects_" + station + " (`timestamp_in_ms`, `timestamp_hr`, `alert_id`,`referrer`,`user_agent`,`ip_address`,`designation`, `station`) " +
					"VALUES('" + timestamp_in_ms + "','" + timestamp_hr + "','" + alert_id + "','" + referrer + "','" + user_agent + "','" + ip_address + "','" + designation + "','" + station + "')",
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
			addMessageToLog("SQLException in Platform.putRedirectHitInDB: Error putting redirect hit in db. message=" +sqle.getMessage());
		}
		finally
		{
			try
			{
				if (rs  != null) { rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("RedirectServlet.putRedirectHitInDB(): Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
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
	}

}
