package tv.huzon;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
	
	public Platform()
	{
		System.err.println("Platform init()");
		try {
		        Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
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
			String hr_timestamp = year + "-" + month + "-" + day + " " + hour24 + ":" + minute + ":" + second + " " + ms;			
			
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
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
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
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
			(new Platform()).addMessageToLog("SQLException in Platform.populateStations: message=" +sqle.getMessage());
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
	

	
	long createAlertInDB(Station station_object, String social_type, String designation, String image_name)
	{
		long returnval = -1L;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO alerts (`social_type`,`designation`,`image_url`,`station`) "
	                    + " VALUES('" + social_type + "','" + designation + "','" + image_name + "','" + station_object.getCallLetters() + "')");
			stmt.executeUpdate(
	                    "INSERT INTO alerts (`social_type`,`designation`,`image_url`,`station`) "
	                    + " VALUES('" + social_type + "','" + designation + "','" + image_name + "','" + station_object.getCallLetters() + "')",
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
			(new Platform()).addMessageToLog("SQLException in Platform.createAlertInDB: message=" +sqle.getMessage());
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
				(new Platform()).addMessageToLog("SQLException in Platform.createAlertInDB: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
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
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
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
			(new Platform()).addMessageToLog("SQLException in Platform.updateAlertText: message=" +sqle.getMessage());
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
				(new Platform()).addMessageToLog("SQLException in Platform.updateAlertText: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
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
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
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
			(new Platform()).addMessageToLog("SQLException in Platform.updateSocialItemID: message=" +sqle.getMessage());
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
				(new Platform()).addMessageToLog("SQLException in Platform.updateSocialItemID: Error occurred when closing rs, stmt and con. message=" +sqle.getMessage());
			}
		}  	
		return returnval;
	}

	boolean putRedirectHitInDB(String station, long alert_id, String referrer, String ip_address, String designation)
	{
		boolean returnval = false;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
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
			(new Platform()).addMessageToLog("SQLException in Platform.putRedirectHitInDB: Error putting redirect hit in db. message=" +sqle.getMessage());
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
