package com.soumen.remote.mdb;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
@Singleton
@Startup
public class Ripper {

	private static Logger logger = Logger.getLogger(Ripper.class);
	
	@PostConstruct
	public void init() {		
		
		logger.info("Setting the Remote MDB");
		TopicRemoteMDB.getInstance();		
			
	}
}
