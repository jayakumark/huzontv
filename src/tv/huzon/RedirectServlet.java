package tv.huzon;

import java.io.IOException;
import java.io.PrintWriter;

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
				if(referrer == null)
				{
					referrer = "";
				}
				Platform p = new Platform();
				boolean successful = p.putRedirectHitInDB(alert_object.getStation(), Long.parseLong(id), referrer, ip_address, alert_object.getDesignation());
				if(!successful)
				{
					SimpleEmailer se = new SimpleEmailer();
					try {
						se.sendMail("Failed to put redirect hit into DB", "alert_timestamp=" + alert_object.getTimestamp() + " alert_id=" + alert_object.getID(), "cyrus7580@gmail.com", "info@huzon.tv");
					} catch (MessagingException e) {
						e.printStackTrace();
					}
				}
				response.sendRedirect(station_object.getLiveStreamURL());
			}
		}
		return;
	}
}
