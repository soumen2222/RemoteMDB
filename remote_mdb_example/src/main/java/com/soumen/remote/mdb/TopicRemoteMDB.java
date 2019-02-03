
package com.soumen.remote.mdb;

import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.jboss.logging.Logger;

public class TopicRemoteMDB implements MessageListener {

	private static Logger log = Logger.getLogger(TopicRemoteMDB.class);
	private static TopicRemoteMDB instance = new TopicRemoteMDB();
	private static final String BASE_PROVIDER_URL = "http-remoting://localhost:8080";
	private Topic remoteTopic;
	private TopicConnectionFactory topicConnectionFactory;
	private TopicConnection connection ;
	private TopicSession session;
	private MessageConsumer subscriber;
	private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";	
	
	
	@Override
	public void onMessage(Message message) {
		if (message instanceof ObjectMessage) {
			log.info("Got Message in Remote MDB for Topic - toipc/remoteTopic ");
			try {
				Object obj = ((ObjectMessage) message).getObject();				
					if(obj instanceof String){
						log.info("Received the message: " + (String)obj);						
					}
				}catch (JMSException e) {
				log.error("Failed to Receive message from Remote Topic -toipc/remoteTopic");
			}
		}
	}

	private TopicRemoteMDB() {
		try {
			String providerURL = System.getProperty("com.soumen.topic.providerurl");

			if (providerURL == null) {
				providerURL = BASE_PROVIDER_URL;
			}

			final Properties env = new Properties();
			env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
			env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, providerURL));

			InitialContext iniCtx = new InitialContext(env);
			topicConnectionFactory = (TopicConnectionFactory) iniCtx.lookup("jms/RemoteConnectionFactory");
			remoteTopic = (Topic) iniCtx.lookup("jms/topic/remoteTopic");
			connection = topicConnectionFactory.createTopicConnection("user1", "Test_1234");
			session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
			subscriber = session.createConsumer(remoteTopic);
			subscriber.setMessageListener(this);
			connection.start();
		} catch (Exception e ) {
			log.error("JMS Exception: Fail to subscribe to topic - toipc/remoteTopic" + e.getMessage());
			try {
				connection.close();
				session.close();
				subscriber.close();
			} catch (JMSException e1) {
				log.error("JMS Exception: Fail to Close connection for topic - toipc/remoteTopic" + e1.getMessage());				
			}
		} 
	}

	public static TopicRemoteMDB getInstance() {
		return instance;
	}

}