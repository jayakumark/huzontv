package tv.huzon;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.mail.MessagingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RedirectServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) throws ServletException
	{
		System.err.println("RedirectServlet init()");
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
			Alert alert_object = new Alert(Long.parseLong(id));
			Station station_object = new Station(alert_object.getStation());
			
			if(alert_object.getID() == -1L) // means it wasn't found
			{
				out.println("Sorry. Unable to find the specified alert id in the database. Can't redirect.");
			}
			else
			{
				String ip_address = request.getRemoteAddr();
				String referrer = request.getHeader("referer");
				String user_agent = request.getHeader("User-Agent");
				if(referrer == null)
					referrer = "";
				if(user_agent == null)
					user_agent = "";
				/*Enumeration<String> headerNames = request.getHeaderNames();
			    while(headerNames.hasMoreElements()) 
			    {
			      String headerName = headerNames.nextElement();
			      System.out.println(headerName + "\t" + request.getHeader(headerName));
			    }*/
				if(referrer == null)
				{
					referrer = "";
				}
				Platform p = new Platform();
				boolean successful = p.putRedirectHitInDB(station_object.getCallLetters(), Long.parseLong(id), referrer, user_agent, ip_address, alert_object.getDesignation());
				if(!successful)
				{
					(new Platform()).addMessageToLog("Failed to put redirect hit into DB alert_timestamp=" + alert_object.getTimestamp() + " alert_id=" + alert_object.getID());
				}
				response.sendRedirect(station_object.getLiveStreamURL());
			}
		}
		return;
	}
}
