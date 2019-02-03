package com.soumen.publish.topic;

import java.io.Serializable;

import javax.ejb.Local;
import javax.ejb.Remote;

public interface RemoteTopic {
		
 @Remote
 public interface R extends RemoteTopic {}
 @Local
 public interface L extends RemoteTopic {}

 public void publishMessage(Serializable message );
}
