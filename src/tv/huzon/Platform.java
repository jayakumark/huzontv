package tv.huzon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.TreeSet;

import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONArray;
import com.amazonaws.util.json.JSONObject;

public class Platform {

	private TreeSet<Station> stations;
	
	public Platform()
	{
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
		}
		catch(SQLException sqle) { sqle.printStackTrace(); }
		finally
		{
			try
			{
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle) { sqle.printStackTrace(); }
		}   
		
		
	}

	public JSONArray getStationsAsJSONArray()
	{
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
