package tv.huzon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Callable;

import javax.mail.MessagingException;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;

public class TwitterUploaderCallable implements Callable<JSONObject> {

	Frame frame2upload;
	User reporter;
	Station station_object;
	//boolean twitter_successful;
	//String twitter_failure_message;
	boolean simulation;
	
	public TwitterUploaderCallable(Frame inc_frame2upload, User inc_reporter, Station inc_station_object, boolean inc_simulation)
	{
		frame2upload = inc_frame2upload;
		station_object = inc_station_object;
		reporter = inc_reporter;
		//twitter_successful = false;
		//twitter_failure_message = "Init failure";
		simulation = inc_simulation;
	}
	
	/*public boolean getTwitterSuccessful()
	{
		return twitter_successful;
	}
	
	public String getTwitterFailureMessage()
	{
		return twitter_failure_message;
	}*/
	
	public String getMissingCredentialsEmailMessage()
	{
		String message = "";
		
			message = 
				reporter.getDisplayName() + 
				",\n\nAn alert triggered for you with huzon.tv. However, our system was unable to actually fire the alert because your Twitter account " + 
				"has become disconnected from huzon.tv. This can happen for several reasons:" +
				"\n\n- You disabled the huzon.tv app in your Twitter configuration" +
				"\n- Your Twitter account was never linked to huzon.tv in the first place"+
				"\n\nPlease go to https://www.huzon.tv/registration.html to link your Twitter account to huzon.tv and enable automated alerts. " +
				"Thanks!\n\nhuzon.tv staff\n\nPS: Here's the image that would have posted: " + frame2upload.getURL();
			return message;
	}	
	
	@Override
	public JSONObject call() {
		SimpleEmailer se = new SimpleEmailer();
		JSONObject return_jo = new JSONObject();
		try
		{
			if(simulation)
			{
				// send emails to admin on simulation since no other action will happen, ok to turn this off if you want
				se.sendMail("Tweet triggered for " + reporter.getDesignation(), "url=" + frame2upload.getURL(), "cyrus7580@gmail.com", "info@huzon.tv");
			}
			
			// check to see that reporter has twitter credentials on file. If not, email the reporter and send email to admin.
			if(reporter.getTwitterAccessToken() == null || reporter.getTwitterAccessToken().equals("") || reporter.getTwitterAccessTokenSecret() == null || reporter.getTwitterAccessTokenSecret().equals(""))
			{
				return_jo.put("twitter_successful", false);
				return_jo.put("twitter_failure_message", "user_had_no_credentials");
				
				reporter.resetTwitterCredentialsInDB(); // at least one credential was missing, wipe out both tat and tats in db
				if(!simulation)
				{	
					String emailmessage = getMissingCredentialsEmailMessage();
					// send to reporter and to admin
					se.sendMail("Action required: huzon.tv Twitter alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
					se.sendMail(reporter.getDesignation() + " was notified of missing Twitter credentials", "The following email was sent to a reporter due to missing twitter credentials:\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
				}
			}
			else // user appears to have twitter credentials
			{
				if(!simulation) // go ahead with the tweet
				{	
					URL image_url = new URL(frame2upload.getURL());
				    ReadableByteChannel rbc = Channels.newChannel(image_url.openStream());
				    String tmpdir = System.getProperty("java.io.tmpdir");
				    System.out.println("TEMP DIR=" + tmpdir);
				    FileOutputStream fos = new FileOutputStream(tmpdir + "/image_for_twitter.jpg"); // fixme this might have conflicts with multiple stations
				    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				    File imagefile = new File(tmpdir + "/image_for_twitter.jpg");
					
					Twitter twitter = new Twitter();
					Platform p = new Platform();
					long redirect_id = p.createAlertInDB(station_object, "twitter", reporter.getDesignation(), frame2upload.getURL());
					String message = station_object.getMessage("twitter", frame2upload.getTimestampInMillis(), redirect_id, reporter);
					JSONObject twit_jo = twitter.updateStatusWithMedia(reporter.getTwitterAccessToken(), reporter.getTwitterAccessTokenSecret(), message, imagefile);
					
					if(twit_jo.has("response_status") && twit_jo.getString("response_status").equals("error")) // if an error was produced
					{
						return_jo.put("twitter_successful", false);
						return_jo.put("twitter_failure_message", twit_jo.getString("message"));

						if(twit_jo.has("twitter_code") && (twit_jo.getInt("twitter_code") == 32 || twit_jo.getInt("twitter_code") == 89)) // and it was due to bad credentials
						{
							reporter.resetTwitterCredentialsInDB(); // the credentials are no good anymore. Delete them to allow the user to start over. (Link is in email below)
							String emailmessage = getMissingCredentialsEmailMessage();
							// send to reporter and to admin
							se.sendMail("Action required: huzon.tv Twitter alert was unable to fire. Please link your accounts.", emailmessage, reporter.getEmail(), "info@huzon.tv");
							se.sendMail(reporter.getDesignation() + " was notified of a disconnected Twitter account", "The following email was sent to a reporter due to an unlinked Twitter account:\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
						}
						else if(twit_jo.has("twitter_code"))
						{
							String emailmessage = getMissingCredentialsEmailMessage();
							// send mail to admin
							se.sendMail(reporter.getDesignation() + " unknown twitter error", "There was an unknown error trying to tweet. twit_jo=" + twit_jo + "\n\nHere's the email that DID NOT go out.\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
						}
						else
						{
							String emailmessage = getMissingCredentialsEmailMessage();
							// send mail to admin
							se.sendMail(reporter.getDesignation() + " some other twitter error", "There was some other error trying to tweet which DID NOT produce a twitter_code. twit_jo=" + twit_jo + "\n\nHere's the email that DID NOT go out.\n\n" + emailmessage, "cyrus7580@gmail.com", "info@huzon.tv");
						}
							
					}
					else
					{
						return_jo.put("twitter_successful", true); // the twitter post was successful, regardless of the two following db updates.
						se.sendMail("Tweet successful for " + reporter.getDesignation(), "url=" + frame2upload.getURL(), "cyrus7580@gmail.com", "info@huzon.tv");
						// if either of these fail, alert the admin within the functions themselves
						boolean alert_text_update_successful = p.updateAlertText(redirect_id, message);
						boolean social_id_update_successful = p.updateSocialItemID(redirect_id,twit_jo.getString("id"));
					}
				}
				else
				{
					return_jo.put("twitter_successful", false);
					return_jo.put("twitter_failure_message", "simulation");
				}
			}
		}
		catch(MalformedURLException murle)
		{
			murle.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return return_jo;
	}
}
