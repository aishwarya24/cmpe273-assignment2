package edu.sjsu.cmpe.library.stompmsg;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;

import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;

public class Listener {
        
        private String apolloUser;
        private String apolloPassword;
        private String apolloHost;
        private int apolloPort;
        private String stompTopic;
        private final LibraryServiceConfiguration configuration;
        private BookRepositoryInterface bookRepository;
        private final Book dummyBook = new Book();

        public Listener(LibraryServiceConfiguration config, BookRepositoryInterface bookRepository) {
                this.configuration = config;
                this.bookRepository = bookRepository;
                apolloUser = configuration.getApolloUser();
                apolloPassword = configuration.getApolloPassword();
                apolloHost = configuration.getApolloHost();
                apolloPort = configuration.getApolloPort();
                stompTopic = configuration.getStompTopicName();
                
        }
        
        public Runnable listener() throws JMSException {
        	System.out.println("LISTENER MODE");
            long isbn;
            String bookTitle;
            String bookCategory;
            String webURL;
            Book tempBook = new Book();
            ArrayList<String> arrivals = new ArrayList<String>();
            StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
            factory.setBrokerURI("tcp://" + apolloHost + ":" + apolloPort);
            System.currentTimeMillis();
            System.out.println("Waiting for messages...");
            while (true) {
                  
                Connection connection = factory.createConnection(apolloUser, apolloPassword);
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination dest = new StompJmsDestination(stompTopic);
                MessageConsumer consumer = session.createConsumer(dest);
                    while (true) {
                            Message msg = consumer.receive(500);
                                if (msg == null)
                                        break;
                                if (msg instanceof TextMessage) {
                                        String body = ((TextMessage) msg).getText();
                                        arrivals.add(body);

                                } else {
                                        System.out.println("Unexpected message type: "
                                                        + msg.getClass());
                                }
                                
                        }
                        connection.close();
                        if(!arrivals.isEmpty()) {
                       
                        for(String arrival:arrivals){
                               
                       isbn = Long.parseLong(arrival.split(":")[0]);
                                bookTitle = arrival.split(":")[1].replaceAll("^\"|\"$", "");
                                bookCategory = arrival.split(":")[2].replaceAll("^\"|\"$", "");
                                webURL = arrival.split(":\"")[3];
                                webURL = webURL.substring(0, webURL.length()-1);
                                tempBook = bookRepository.getBookByISBN(isbn);
                                System.out.println("temporaryBook: "+tempBook);
                                System.out.println("dummyBook: "+dummyBook);
                                
                                if (tempBook.getIsbn()==0) {
                                        System.out.println("reachable");
                                        tempBook.setIsbn(isbn);
                                        tempBook.setCategory(bookCategory);
                                        tempBook.setTitle(bookTitle);
                                        try {
                                                tempBook.setCoverimage(new URL(webURL));
                                        } catch (MalformedURLException e) {
                                                e.printStackTrace();
                                        }
                                        bookRepository.addNewBook(tempBook);
                                        
                                }
                                else {
                                        System.out.println(" successful!!! transforming status to available");
                                        tempBook.setStatus(Status.available);
                                }
                                
                        }
                        arrivals.clear();
                        
            }
                }


}
}