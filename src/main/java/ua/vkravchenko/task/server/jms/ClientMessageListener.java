package ua.vkravchenko.task.server.jms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.type.MapType;
import org.springframework.beans.factory.annotation.Autowired;

import ua.vkravchenko.task.server.entity.User;
import ua.vkravchenko.task.server.repository.UserRepository;

public class ClientMessageListener implements MessageListener {

	// Служит для отсылки сообщений в очередь ActiveMQ
	private MessageSender messageSender;
	private static final Logger logger = Logger.getLogger(ClientMessageListener.class);
	private ObjectWriter objectWriter = new ObjectMapper().writer();
	
	@Autowired
	private UserRepository userRepository;
	
	@Override
	public void onMessage(Message message) {
		logger.debug("Received message from queue [" + message +"]");
		
		String responseMessage = null;
		String status = null;
		
		if (message instanceof TextMessage) {
			try {
				// Получаем JSON из входящего сообщения и преобразовываем в HashMap
				// которая содержит комманду и информацию о нужном пользователе
				String json = ((TextMessage) message).getText();
				ObjectMapper objectMapper = new ObjectMapper();
				
				MapType mapType = objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class);
				Map<String, String> map = objectMapper.readValue(json, mapType);
				
				String command = map.get("command");
				String jsonUser = map.get("value");
				User user = objectMapper.readValue(jsonUser, User.class);
				
				// Выбираем действие в зависимости от комманды
				if ("add".equals(command)) {
					if (user.getName().equals("") || user.getSurname().equals("")) {
						status = "FAIL";
						responseMessage = "You must specify user's name and surname";
					} else {
						userRepository.save(user);
						status = "OK";
						responseMessage = "User successfully added";
					}
				} else if ("remove".equals(command)) {
					if (userRepository.exists(user.getId())) {
						userRepository.delete(user.getId());
						responseMessage = "User successfully removed";
					} else {
						responseMessage = "There is no such user";
					}
					status = "OK";
					
				} else if ("get".equals(command)) {
					
					List<User> userList = new ArrayList<User>();
					int id = user.getId();
					String name = user.getName();
					String surname = user.getSurname();
					
					// Ищем пользователей в зависимости от того, какая информация о них указана
					if (id != - 1 && !name.equals("") && !surname.equals("")) {
						userList = userRepository.findByIdAndNameAndSurname(id, name, surname);
					} else if (id != -1 && !name.equals("")) {
						userList = userRepository.findByIdAndName(id, name);
					} else if (id != -1 && !surname.equals("")) {
						userList = userRepository.findByIdAndSurname(id, surname);
					} else if (!name.equals("") && !surname.equals("")) {
						userList = userRepository.findByNameAndSurname(name, surname);
					} else if (id != -1) {
						userList.add(userRepository.findOne(id));
					} else if (!name.equals("")) {
						userList = userRepository.findByName(name);
					} else if (!surname.equals("")) {
						userList = userRepository.findBySurname(surname);
					} else if (id == -1 && name.equals("") && surname.equals("")) {
						userList = userRepository.findAll();
					}
					
					if (userList.isEmpty()) {
						status = "FAIL";
						responseMessage = "Unable to find such user";
					} else {
						status = "OK";
						responseMessage = objectWriter.writeValueAsString(userList);
					}
					
				} else {
					status = "FAIL";
					responseMessage = "Command type was not specified";
				}
				
				// Отсылаем результат
				sendMessage(status, responseMessage);
				
			} catch (JMSException jmsEx_p) {
				
				responseMessage = "An error occurred extracting message";
				status = "FAIL";
				try {
					sendMessage(status, responseMessage);
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			} catch (JsonParseException e) {
				responseMessage = "JsonParseException";
				status = "FAIL";
				try {
					sendMessage(status, responseMessage);
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			} catch (JsonMappingException e) {
				responseMessage = "JsonMappingException";
				status = "FAIL";
				try {
					sendMessage(status, responseMessage);
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			} catch (IOException e) {
				responseMessage = "IOException";
				status = "FAIL";
				try {
					sendMessage(status, responseMessage);
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			}
		} else {
			responseMessage = "Message is not of expected type";
			status = "FAIL";
			try {
				sendMessage(status, responseMessage);
			} catch (Exception e1) {
				e1.printStackTrace();
			} 
		}
		
	}
	
	private void sendMessage(String status, String responseMessage, List<User> users) throws JsonGenerationException, JsonMappingException, IOException, JMSException {
		
		// Кладем полученную информацию в HashMap (статус запроса и информация о 
		// пользователе/сообщение об ошибке), преобразовываем её в JSON и отправляем.
		Map<String, String> responseMap = new HashMap<String, String>();
		responseMap.put("message", responseMessage);
		responseMap.put("status", status);
		if (users != null) {
			String usersJson = objectWriter.writeValueAsString(users);
			responseMap.put("users", usersJson);
		}
		String result = objectWriter.writeValueAsString(responseMap);
		messageSender.sendMessage(result);
		
	}
	
	private void sendMessage(String status, String responseMessage) throws JsonGenerationException, JsonMappingException, IOException, JMSException {
		sendMessage(status, responseMessage, null);
		
	}

	public void setMessageSender(MessageSender messageSender) {
		this.messageSender = messageSender;
	}
	
}
