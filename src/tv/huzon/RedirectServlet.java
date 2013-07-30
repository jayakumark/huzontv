package tv.huzon;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

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
		out.println("<html>");
		out.println("<head>");
		out.println("<link rel=\"icon\" type=\"image/png\" href=\"images/huzon_logo_16x16.png\" />");
		out.println("<title>huzon.tv redirect page</title>");
		out.println("<style>");
		out.println("  body{ ");
		out.println("      	 background-color: white; ");
		out.println("         font-family: arial, helvetica;");
		out.println("         font-size: 11px; ");
		out.println("         margin:0px; ");
		out.println("         border:0px solid;");
		out.println("   }");
		out.println("   td");
		out.println("   {");
		out.println("   		text-align:center; font-size:36px; padding-top:25px; padding-bottom:25px");
		out.println("   }");
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		String id = request.getParameter("id");
		if(id == null)
		{
			out.println("Sorry. This page requires an \"id\" value to send you to the correct livestream. Please check the link and try again.");
		}
		else
		{
			Alert alert_object = new Alert(Long.parseLong(id));
			Station station_object = new Station(alert_object.getStation());
			String ip_address = request.getRemoteAddr();
			String referrer = request.getHeader("referer");
			String user_agent = request.getHeader("User-Agent");
			if(alert_object.getID() == -1L) // means it wasn't found
			{
				out.println("Sorry. Unable to find the specified alert id in the database. Can't redirect.");
			}
			else
			{
				// if redirect value exists, then the user came to the page, link was > limit old, and made a choice
				// if redirect value does not exist, this is the user's first visit
				// 			is the link > limit? If so, show options
				// 			if not, auto-redirect to livestream
				Platform p = new Platform();
				String redirect = request.getParameter("redirect");
				if(redirect != null)
				{
					String redirect_id = request.getParameter("redirect_id");
					if(redirect_id != null && p.isNumeric(redirect_id))
					{
						// 6 possible choices
						if(redirect.equals("homepage") || redirect.equals("recent_newscasts") || redirect.equals("clips") || redirect.equals("iphone_app") || redirect.equals("android_app") || redirect.equals("livestream_eventual"))
						{
							p.updateRedirectUltimateDestinationValue(station_object.getCallLetters(), Long.parseLong(redirect_id), redirect); // which redirect entry, the nonce info integrity check, and the ultimate redirect value
							if(redirect.equals("homepage"))
								response.sendRedirect(station_object.getHomepageURL());
							else if(redirect.equals("recent_newscasts"))
								response.sendRedirect(station_object.getRecentNewscastsURL());
							else if(redirect.equals("clips"))
								response.sendRedirect(station_object.getClipsURL());
							else if(redirect.equals("iphone_app"))
								response.sendRedirect(station_object.getiPhoneAppURL());
							else if(redirect.equals("android_app"))
								response.sendRedirect(station_object.getAndroidAppURL());
							else if(redirect.equals("livestream_eventual"))
								response.sendRedirect(station_object.getLiveStreamURL());
						}
						else
						{
							p.addMessageToLog("WARNING: Unable to update ultimate_destination or redirect user properly redirect value " + redirect + " wasn't valid. Sending to live stream as fallback. (redirect_id was not null and was numeric)");
							response.sendRedirect(station_object.getLiveStreamURL()); // this was a fail. Just send to live stream. 
						}
					}
					else // redirect_id was null or non-numeric // attempt to redirect based on redirect value only
					{
						if(redirect.equals("homepage"))
							response.sendRedirect(station_object.getHomepageURL());
						else if(redirect.equals("recent_newscasts"))
							response.sendRedirect(station_object.getRecentNewscastsURL());
						else if(redirect.equals("clips"))
							response.sendRedirect(station_object.getClipsURL());
						else if(redirect.equals("iphone_app"))
							response.sendRedirect(station_object.getiPhoneAppURL());
						else if(redirect.equals("android_app"))
							response.sendRedirect(station_object.getAndroidAppURL());
						else if(redirect.equals("livestream_eventual"))
							response.sendRedirect(station_object.getLiveStreamURL());
						else
						{
							p.addMessageToLog("WARNING: Unable to update ultimate_destination or redirect user properly redirect value " + redirect + " wasn't valid. Sending to live stream as fallback. (redirect_id was null or non-numeric)");
							response.sendRedirect(station_object.getLiveStreamURL()); // this was a fail. Just send to live stream. 
						}
							
					}
				}
				else // this is the user's first visit via this particular link
				{	
					if(referrer == null)
						referrer = "";
					if(user_agent == null)
						user_agent = "";

					// create the redirect hit
					long redirect_id = p.putRedirectHitInDB(station_object.getCallLetters(), Long.parseLong(id), referrer, user_agent, ip_address, alert_object.getDesignation());
					if(redirect_id == -1)
					{
						(new Platform()).addMessageToLog("Failed to put redirect hit into DB. Crapped out by sending user to live stream. alert_timestamp=" + alert_object.getTimestamp() + " alert_id=" + alert_object.getID());
						response.sendRedirect(station_object.getLiveStreamURL()); // this was a fail. Just send to live stream. 
					}
					else
					{	
						// if there is no live stream, show interstitial page with options
						if(!station_object.isLiveStreaming()) 
						{
							out.println("<table style=\"width:600px;margin-right:auto;margin-left:auto\">");
							out.println("	<tr>");
							out.println("		<td><img src=\"images/" + station_object.getLogoFilename() + "\"></td>");
							out.println("	</tr>");
							out.println("	<tr>");
							out.println("		<td style=\"font-size:18px\"><span style=\"color:red\">NOTE: The link you clicked is old and the live stream is not currently active. Where would you like to go instead?</span></td>");
							out.println("	</tr>");
							out.println("	<tr>");
							out.println("		<td><a href=\"" + station_object.getLiveStreamURLAlias() + "?id=" + id + "&redirect=homepage&redirect_id=" + redirect_id + "\">"+ station_object.getShortDisplayName() + " homepage</a></td>");
							out.println("	</tr>");
							if(!station_object.getRecentNewscastsURL().isEmpty())
							{	
								out.println("	<tr>");
								out.println("		<td><a href=\"" + station_object.getLiveStreamURLAlias() + "?id=" + id + "&redirect=recent_newscasts&redirect_id=" + redirect_id + "\">Recent newscasts</a></td>");
								out.println("	</tr>");
							}
							if(!station_object.getClipsURL().isEmpty())
							{
								out.println("	<tr>");
								out.println("			<td><a href=\"" + station_object.getLiveStreamURLAlias() + "?id=" + id + "&redirect=clips&redirect_id=" + redirect_id + "\">Recent video clips</a></td>");
								out.println("	</tr>");
							}
							if(!station_object.getiPhoneAppURL().isEmpty() && user_agent.indexOf("iPhone") != -1)
							{
								out.println("	<tr>");
								out.println("		<td><a href=\"" + station_object.getLiveStreamURLAlias() + "?id=" + id + "&redirect=iphone_app&redirect_id=" + redirect_id + "\">iPhone app</a></td>");
								out.println("	</tr>");
							}
							if(!station_object.getAndroidAppURL().isEmpty() && user_agent.indexOf("Android") != -1)
							{
								out.println("	<tr>");
								out.println("		<td><a href=\"" + station_object.getLiveStreamURLAlias() + "?id=" + id + "&redirect=android_app&redirect_id=" + redirect_id + "\">Android app</a></td>");
								out.println("	</tr>");
							}
							out.println("	<tr>");
							out.println("		<td><a href=\"" + station_object.getLiveStreamURLAlias() + "?id=" + id + "&redirect=livestream_eventual&redirect_id=" + redirect_id + "\">Take me to the live stream anyway</a></td>");
							out.println("	</tr>");
							out.println("</table>");
						}
						else // link is not old, do immediate redirect
						{
							p.updateRedirectUltimateDestinationValue(station_object.getCallLetters(), redirect_id, "livestream_immediate"); // which redirect entry, the nonce info integrity check, and the ultimate redirect value
							response.sendRedirect(station_object.getLiveStreamURL());
						}
					}
				}
			}
		}
		out.println("</body>");
		out.println("</html>");
		return;
	}
}
