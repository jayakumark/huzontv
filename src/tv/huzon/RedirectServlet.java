package tv.huzon;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;


public class RedirectServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException
	{
		System.err.println("Endpoint init()");
		 try {
		        Class.forName("com.mysql.jdbc.Driver");
		    } catch (ClassNotFoundException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    } 
		super.init(config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		System.out.println("tv.huzon.RedirectServlet.doPost(): entering...");
		response.setContentType("text/html; charset=UTF-8;");
		PrintWriter out = response.getWriter();
		out.println("Sorry. This servlet only speaks GET.");
		return;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		System.out.println("tv.huzon.RedirectServlet.doGet(): entering...");
		response.setContentType("text/html; charset=UTF-8;");
		PrintWriter out = response.getWriter();
		String id = request.getParameter("id");
		if(id == null)
		{
			out.println("Sorry. This page requires an \"id\" value to send you to the correct livestream. Please check the link and try again.");
		}
		else
		{
			JSONObject alert_object = getAlert(Long.parseLong(id));
			if(alert_object == null)
			{
				out.println("Sorry. Unable to find the specified alert id in the database.");
			}
			else
			{
				String ip_address = request.getRemoteAddr();
				String referrer = request.getHeader("referer");
				if(referrer == null)
				{
					referrer = "";
				}
				try
				{
					@SuppressWarnings("unused")
					boolean successful = putRedirectHitInDB(alert_object.getString("station"), Long.parseLong(id), referrer, ip_address, alert_object.getString("designation"));
				}
				catch(JSONException jsone)
				{
					jsone.printStackTrace();
					System.out.println("tv.huzon.RedirectServlet.doGet(): jsonexception trying to get station string from alert_object when putting redirect hit into database jsone.getMessage()=" + jsone.getMessage());
				}
				try
				{
					response.sendRedirect("http://" + alert_object.getString("livestream_url"));
				}
				catch(JSONException jsone)
				{
					jsone.printStackTrace();
					System.out.println("tv.huzon.RedirectServlet.doGet(): jsonexception trying to get livestream_url from alert_object jsone.getMessage()=" + jsone.getMessage());
				}
			}
		}
		return;
	}

	JSONObject getAlert(long alert_id)
	{
		JSONObject return_jo = null;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement();
			// get next frame in database, up to one day from now.
			rs = stmt.executeQuery("SELECT * FROM alerts WHERE id=" + alert_id + " LIMIT 1"); 
			if(rs.next())
			{
				return_jo = new JSONObject();
				try {
					return_jo.put("id", rs.getLong("id"));
					return_jo.put("social_type", rs.getString("social_type"));
					return_jo.put("image_name", rs.getString("image_name"));
					return_jo.put("creation_timestamp", rs.getTimestamp("creation_timestamp"));
					return_jo.put("designation", rs.getString("designation"));
					return_jo.put("station", rs.getString("station"));
					return_jo.put("livestream_url", rs.getString("livestream_url"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return_jo = null;
				}
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
				if (rs  != null){ rs.close(); } if (stmt  != null) { stmt.close(); } if (con != null) { con.close(); }
			}
			catch(SQLException sqle)
			{ 
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return return_jo; // -1L indicates no frame found
	}
	
	
	boolean putRedirectHitInDB(String station, long alert_id, String referrer, String ip_address, String designation)
	{
		boolean returnval = false;
		ResultSet rs = null;
		Connection con = null;
		Statement stmt = null;
		try
		{
			con = DriverManager.getConnection("jdbc:mysql://huzon.cvl3ft3gx3nx.us-east-1.rds.amazonaws.com/huzon?user=huzon&password=6SzLvxo0B");
			stmt = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			System.out.println("INSERT INTO redirects_" + station + " (`alert_id`,`referrer`,`ip_address`,`designation`) " +
					"VALUES('" + alert_id + "','" + referrer + "','" + ip_address + "','" + designation + "')");
			stmt.executeUpdate("INSERT INTO redirects_" + station + " (`alert_id`,`referrer`,`ip_address`,`designation`) " +
					"VALUES('" + alert_id + "','" + referrer + "','" + ip_address + "','" + designation + "')");
			returnval = true;
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
				System.out.println("Problem closing resultset, statement and/or connection to the database."); 
			}
		}  	
		return returnval;
	}
	
}
