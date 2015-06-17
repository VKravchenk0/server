package ua.vkravchenko.task.server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ua.vkravchenko.task.server.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	public List<User> findByIdAndNameAndSurname(Integer id, String name,
			String surname);

	public List<User> findByIdAndName(Integer id, String name);

	public List<User> findByIdAndSurname(Integer id, String surname);

	public List<User> findByNameAndSurname(String name, String surname);

	public List<User> findByName(String name);

	public List<User> findBySurname(String surname);
	
}
