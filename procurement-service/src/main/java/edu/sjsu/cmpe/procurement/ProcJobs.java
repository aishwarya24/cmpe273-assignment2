package edu.sjsu.cmpe.procurement;

import java.util.ArrayList;

import javax.jms.Connection;
//import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
//import org.fusesource.stomp.jms.message.StompJmsMessage;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
//import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;


@Every("300s")
public class ProcJobs extends Job {
	
	@Override
	public void doJob() {
		 System.out.println("Jobs Working");
         ProcJobs jobs = new ProcJobs();
         try {
                 String booksLost = jobs.consumer();
                 if (booksLost!=("[]"))
                         jobs.HttpPOST(booksLost);
                 ArrayList<String> booksReceived = new ArrayList<String>();
                 booksReceived = HttpGET();
                 jobs.Publisher(booksReceived);
         } catch (Exception e) {
                 System.out.println("Unexpected error!");
         }
         
 }
		
		
		
	public void Publisher(ArrayList<String> booksReceived) throws JMSException{
		    String user = "admin";
	        String password = "password";
	        String host = "54.215.210.214";
	        System.out.println("PUBLISHER MODE");
	        int port = 61613;
	        String dest1 = "/topic/62108.book.all";
	        String dest2 = "/topic/62108.book.computer";

	        StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	        factory.setBrokerURI("tcp://" + host + ":" + port);

	        Connection connection = factory.createConnection(user, password);
	        connection.start();
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        Destination dest_library_a = new StompJmsDestination(dest1);
	        Destination dest_library_b = new StompJmsDestination(dest2);
	        MessageProducer producer_a = session.createProducer(dest_library_a);
	        MessageProducer producer_b = session.createProducer(dest_library_b);
	        
	        String textdata;
	        for (int i = 0; i < booksReceived.size(); i++) {
	                textdata = booksReceived.get(i);
	                TextMessage msg = session.createTextMessage(textdata);
	                msg.setLongProperty("id", System.currentTimeMillis());
	                System.out.println("Sent the respective message to library_a");
	                producer_a.send(msg);                
	                if (textdata.split(":")[2].equals("\"computer\"")) {
	                        System.out.println("Sent the respective message to library_b");
	                        producer_b.send(msg);
	                System.out.println(textdata);        
	                }
	        }
	       
	        connection.close();

				
	}



	public ArrayList<String> HttpGET() throws JSONException{
		 System.out.println("HTTP GET MODE");
		
		  Client client = Client.create();
	        WebResource webResource = client.resource("http://54.215.210.214:9000/orders/62108");
	        ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
	        if (response.getStatus() != 200) {
	                throw new RuntimeException("Failed : HTTP error code : "
	                                + response.getStatus());
	        }
	        System.out.println(response.toString());
	        String output = response.getEntity(String.class);
	        System.out.println(output+"\n");
	        JSONObject object = new JSONObject(output);
	        JSONArray ship = object.getJSONArray("shipped_books");
	        int n = ship.length();
	        ArrayList<String> booksReceived = new ArrayList<String>();
	        for (int i =0; i<n ; i++){
	                JSONObject getbooks = ship.getJSONObject(i);
	                booksReceived.add(getbooks.getLong("isbn")+":\""+getbooks.getString("title")+"\":\""+getbooks.getString("category")+"\":\""+getbooks.getString("coverimage")+"\"");
	        }
	        System.out.println(booksReceived);
	        return booksReceived;
	}



	public void HttpPOST(String booksLost) {
		System.out.println("HTTP POST MODE");
		
		  Client client = Client.create(); 
	        System.out.println(booksLost);
	        String msg = "{\"id\" : \"62108\",\"order_book_isbns\":"+booksLost+"}";
	        WebResource webResource = client.resource("http://54.215.210.214:9000/orders");
	        ClientResponse response = webResource.type("application/json").post(ClientResponse.class, msg);
	        if (response.getStatus()!=200) {
	                throw new RuntimeException("Falied: HTTP error : " +response.getStatus());        
	        }
	        System.out.println(response.toString());
	        System.out.println(response.getEntity(String.class));

	}



	public String consumer() throws JMSException, InterruptedException{
		
	        String user = "admin";
	        String password = "password";
	        String host = "54.215.210.214";
	       System.out.println("CONSUMER MODE");
	        int port = 61613;
	        String queue = "/queue/62108.book.orders";
	       

	        StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	        factory.setBrokerURI("tcp://" + host + ":" + port);
	        Connection connection = factory.createConnection(user, password);
	        connection.start();
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        Destination dest = new StompJmsDestination(queue);
	        ArrayList<String> tempString = new ArrayList<String>();
	        MessageConsumer consumer = session.createConsumer(dest);
	        System.out.println("Waiting for messages from " + queue + "...");
	        while(true) {
	                System.out.println("first check");
	            Message msg = consumer.receive(5000);
	            if(msg==null){
	                System.out.println("next check");
	                break;
	            }
	            if( msg instanceof  TextMessage ) {
	            String body = ((TextMessage) msg).getText();
	            tempString.add(body.split(":")[1]);
	                System.out.println(body.split(":")[1]);
	                } 
	            else {
	                System.out.println("Unexpected message type: "+msg.getClass());

	            }            
	        }
	        String ltIsbn = tempString.toString();
	        connection.close();
	        return ltIsbn;
	        }	
		
}
