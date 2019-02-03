# Consuming messages from a remote WILDFLY JMS Server

In this article we are going to discuss about consuming messages from a remote WILDFLY JMS Server. The article is divided into two sections, in the first we are going to discuss about the use cases where remote message driven bean (MDB) can be used to solve the functionality in an easy and effective way and in the second we will delve into the technicalities of configuring remote MDB with WILDFLY. All the necessary details will be available in the git hub. I hope you find this article useful, if there are any errors I'd be more than happy to fix them. 

a)	Section 1:

Data can be sent from one application to other, which are hosted in different machines, through messaging, webservices etc.  Messaging has many advantages such as asynchronous, reliable communication, decoupling or disconnected operation , and mediation.
Let us consider this use case , The application (application 2) data should be modified or refreshed whenever there is a change in the application 1 , and due to scalability reasons there are multiple instances of application 2. High level deployment is depicted below.

One way: Application 1 can maintain the list of application2 servers and whenever there is a need to send data to application server, it can go over all the servers and send data via webservices. But this becomes a headache when new machines are spawned on demand or removed due to maintenance or other issues. Application 1 should continuously update the server details and handle the webservice push exception in case of server unavailability. 

![Alt text](/images/Deployment.png?raw=true "Listeners")

Better Way: We can use a JMS – Broker, producer and consumer to resolve the use case in a better way. Producer can send data to the channel without worrying about the consumers. Broker can manage the durable or Non-Durable subscribers. So, when a new server is spawned it is going to inform the broker that I have subscribed to a topic and ready to receive message and when a server is removed from the cluster, Broker holds the data for a durable subscriber so once the server is back it can process those data whereas for the non-durable subscriber the data is lost. We can use Request – reply or Publish-Subscribe integration pattern. In the example code, Publish -Subscribe integration pattern is used.

![Alt text](/images/RemoteTopic.png?raw=true "Listeners")

b) Section 2:

We need Wildfly 14 application server to host the application and Java 8, and maven to build the application.

1st Step: Let’s Download Wildfly 14.0.1. Final version from the wildfly download site. After download, unzip the package and run the application server.
2nd Step: We are going to add a Topic and login details in the activemq messaging subsystem.
-	Open the default standalone file in the \wildfly-14.0.1.Final\standalone\configuration folder and add the topic and login details. The updated standalone is shown below ( highlighted are the new addition)
  a) Activemq user is added ( Credentials are : user1/Test_1234) 
  b) Topic (remoteTopic) is added. We need to have both the entries ("java:/topic/remoteTopic , java:jboss/exported/jms/topic/remoteTopic") for local and remote access.

<subsystem xmlns="urn:jboss:domain:messaging-activemq:4.0">
            <server name="default">
                <cluster user="${jboss.messaging.cluster.user:user1}" password="${jboss.messaging.cluster.password:Test_1234}"/>
                <security-setting name="#">
                    <role name="guest" send="true" consume="true" create-non-durable-queue="true" delete-non-durable-queue="true"/>
                </security-setting>
                <address-setting name="#" dead-letter-address="jms.queue.DLQ" expiry-address="jms.queue.ExpiryQueue" max-size-bytes="10485760" page-size-bytes="2097152" message-counter-history-day-limit="10"/>
                <http-connector name="http-connector" socket-binding="http" endpoint="http-acceptor"/>
                <http-connector name="http-connector-throughput" socket-binding="http" endpoint="http-acceptor-throughput">
                    <param name="batch-delay" value="50"/>
                </http-connector>
                <in-vm-connector name="in-vm" server-id="0">
                    <param name="buffer-pooling" value="false"/>
                </in-vm-connector>
                <http-acceptor name="http-acceptor" http-listener="default"/>
                <http-acceptor name="http-acceptor-throughput" http-listener="default">
                    <param name="batch-delay" value="50"/>
                    <param name="direct-deliver" value="false"/>
                </http-acceptor>
                <in-vm-acceptor name="in-vm" server-id="0">
                    <param name="buffer-pooling" value="false"/>
                </in-vm-acceptor>
                <jms-queue name="ExpiryQueue" entries="java:/jms/queue/ExpiryQueue"/>
                <jms-queue name="DLQ" entries="java:/jms/queue/DLQ"/>
				<jms-topic name="remoteTopic" entries="java:/topic/remoteTopic java:jboss/exported/jms/topic/remoteTopic"/>               
                <connection-factory name="InVmConnectionFactory" entries="java:/ConnectionFactory" connectors="in-vm"/>
                <connection-factory name="RemoteConnectionFactory" entries="java:jboss/exported/jms/RemoteConnectionFactory" connectors="http-connector"/>
                <pooled-connection-factory name="activemq-ra" entries="java:/JmsXA java:jboss/DefaultJMSConnectionFactory" connectors="in-vm" transaction="xa"/>
            </server>
        </subsystem>

The standalone is available in the git hub.

3rd Step: Compile the code and deploy both the war in two application servers configured in different machine.
 After successful compilation , remote-mdb.war and topic-publisher.war will be generated . We need to deploy topic-publisher.war in machine 1 with wildfly server and remote-mdb.war in machine 2 with widlfy server. ( Note-The application can be deployed in same wildly and machine .)
remote-mdb.war should know the details of the topic /channel , so there is a core property to define the remote topic details. i.e <property name="com.soumen.topic.providerurl" value="http-remoting://<IPAddress>:<Port Number>"/> ( eg: http-remoting://10.0.0.1:8080)

Once the deployment is done, in machine1 – through Wildfly CLI command , check the Topic details through below command. We can see that it has one subscriber. Deploying multiple instances of remote-mdb war will proportionately increase the subscriptions count.

/subsystem=messaging-activemq/server=default/jms-topic=remoteTopic:read-resource (include-runtime=true, include-defaults=true)

{
    "outcome" => "success",
    "result" => {
        "delivering-count" => 0,
        "durable-message-count" => 0,
        "durable-subscription-count" => 0,
        "entries" => [
            "java:/topic/remoteTopic",
            "java:jboss/exported/jms/topic/remoteTopic"
        ],
        "legacy-entries" => undefined,
        "message-count" => 0L,
        "messages-added" => 0L,
        "non-durable-message-count" => 0,
        "non-durable-subscription-count" => 1,
        "subscription-count" => 1,
        "temporary" => false,
        "topic-address" => "jms.topic.remoteTopic"
    }
}

Let us go through the code to find out how the application is sending data to remote Topic.

The remote topic (remoteTopic) and connection factory (java:/JmsXA) configured in the standalone are injected here.

    @Resource(mappedName = "java:/topic/remoteTopic")
	private Topic remoteTopic;
	
	@Resource (mappedName = "java:/JmsXA")
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
	
Now let us see the remote mdb consumer. The default BASE_PROVIDER_URL is  "http-remoting://localhost:8080" . It can be seen that the code is using the  RemoteConnectionFactory , remoteTopic and user credential to make a connection with remote Topic and consume data. 


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

For Testing purpose , data can be pushed by calling a servlet. The end point of servlet is http://localhost:8080/topic-publisher/pushData ( POST Method)

After pushing we can check the Topic through CLI or admin portal to see the quantity of message added to the topic.


The code can be downloaded from github.


Note:
References - Enterprise Integration pattern by Gregor Hohpe
