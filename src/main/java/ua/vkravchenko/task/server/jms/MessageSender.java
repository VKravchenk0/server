package ua.vkravchenko.task.server.jms;

import javax.jms.JMSException;
import javax.jms.Queue;

import org.apache.log4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageSender {
	
	private JmsTemplate jmsTemplate;
	
	private Queue outgoingQueue;
	
	private static final Logger logger = Logger .getLogger(MessageSender.class);

	public void sendMessage(String message) throws JMSException {
		logger.debug("About to put message on queue. Queue[" + outgoingQueue.toString() + "] Message[" + message + "]");
		jmsTemplate.convertAndSend(outgoingQueue, message);
	}
	
	public void setJmsTemplate(JmsTemplate tmpl) {
		this.jmsTemplate = tmpl;
	}
	
	public void setOutgoingQueue(Queue queue) {
		this.outgoingQueue = queue;
	}

}
