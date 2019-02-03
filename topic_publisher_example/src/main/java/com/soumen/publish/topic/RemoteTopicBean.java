package com.soumen.publish.topic;

import java.io.Serializable;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import org.jboss.logging.Logger;

@Stateless
public class RemoteTopicBean implements RemoteTopic.L,RemoteTopic.R {
	
	private static Logger log = Logger.getLogger(RemoteTopicBean.class);
	
	@Resource(mappedName = "java:/topic/remoteTopic")
	private Topic remoteTopic;
	
	@Resource(mappedName = "java:/JmsXA")
	private TopicConnectionFactory topicConnectionFactory;
	
	@Override
	public void publishMessage(Serializable message) {
		try (TopicConnection  connection = topicConnectionFactory.createTopicConnection()) {
			try (TopicSession session = connection.createTopicSession(false,Session.AUTO_ACKNOWLEDGE)){
				try (MessageProducer messageProducer = session.createProducer(remoteTopic) ){
					messageProducer.send(session.createObjectMessage(message));		
				}
			}			
		} catch (JMSException e) {
			log.error("Fail to publish message to Topic -"+ e.getMessage(),e);
		}
	}
}
