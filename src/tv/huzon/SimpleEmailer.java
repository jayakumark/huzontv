//package mypackage1;

package tv.huzon;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
    
public class SimpleEmailer {

   
  public SimpleEmailer()
  {
    
  }
  
  public void sendMail(String subject, String body, String to, String from) throws MessagingException
  {
	  final String username = "AKIAIGTCD3TYQ6BLHWAA";
	  final String password = "AunSYtfjq3D1p7KQq0iV71kULV4bQAkrahDaO2l3vxJ6";

	  Properties props = new Properties();
	  
	  props.put("mail.smtp.auth", "true");
	  props.put("mail.smtp.starttls.enable", "true");
	  props.put("mail.smtp.host", "email-smtp.us-east-1.amazonaws.com");
	  props.put("mail.smtp.port", "25");
	  
	  Session session = Session.getInstance(props,
			  new javax.mail.Authenticator() {
		  protected PasswordAuthentication getPasswordAuthentication() {
			  return new PasswordAuthentication(username, password);
		  }
	  });

		  Message message = new MimeMessage(session);
		  message.setFrom(new InternetAddress(from));
		  message.setRecipients(Message.RecipientType.TO,
				  InternetAddress.parse(to));
		  message.setSubject(subject);
		  message.setText(body);

		  Transport.send(message);

		  System.out.println("Done");

  }
  
  public static void main(String[] args) {
	  
		SimpleEmailer se = new SimpleEmailer();
		try {
			se.sendMail("hoozon subj", "hoozon body", "cyrus7580@gmail.com", "info@hoozon.tv");
		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
       
 
      
}
