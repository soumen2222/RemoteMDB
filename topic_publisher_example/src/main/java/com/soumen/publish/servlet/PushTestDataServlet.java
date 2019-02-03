/**
 * 
 */
package com.soumen.publish.servlet;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.soumen.publish.topic.RemoteTopic;

public class PushTestDataServlet extends HttpServlet {
	
	private static final long serialVersionUID = 4984769595399582487L;

	@EJB
	RemoteTopic.L remoteTopic;
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
	
		remoteTopic.publishMessage("TestData");
	}
	
	
	
	
}
