package ua.vkravchenko.task.server.service;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ua.vkravchenko.task.server.entity.User;
import ua.vkravchenko.task.server.repository.UserRepository;

@Transactional
@Service
public class InitDbService {

	@Autowired
	private UserRepository userRepository;
	
	// задаем начального пользователя
	@PostConstruct
	public void init() {
		User user = new User();
		user.setName("John");
		user.setSurname("Doe");
		userRepository.save(user);
	}
	
	
}
