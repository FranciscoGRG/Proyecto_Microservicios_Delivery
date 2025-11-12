package com.fran.users_service.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fran.users_service.app.dtos.AuthResponseDto;
import com.fran.users_service.app.dtos.LoginRequestDto;
import com.fran.users_service.app.dtos.RegisterRequestDto;
import com.fran.users_service.app.dtos.UpdateUserDTO;
import com.fran.users_service.app.dtos.UserDTO;
import com.fran.users_service.app.models.User;
import com.fran.users_service.app.repositories.IUserRepository;
import com.fran.users_service.app.services.JwtService;
import com.fran.users_service.app.services.UserService;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private IUserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private User testUser;
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final String ENCODED_PASSWORD = "encodedPasswordHash";
    private final String TEST_TOKEN = "jwt.test.token";
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setName("Test User");
        testUser.setEmail(TEST_EMAIL);
        testUser.setPhone("123456789");
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setRol("USER");
    }

    @Test
    void findAll_ShouldReturnAllUsers() {

        List<User> expectedUsers = Arrays.asList(testUser, new User());
        when(repository.findAll()).thenReturn(expectedUsers);

        List<User> actualUsers = userService.findAll();

        assertNotNull(actualUsers);
        assertEquals(2, actualUsers.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void register_ShouldReturnAuthResponse_WhenSuccessful() {

        RegisterRequestDto request = new RegisterRequestDto("New User", TEST_EMAIL, "987654321", TEST_PASSWORD);
        when(repository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(repository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(testUser.getEmail(), testUser.getRol())).thenReturn(TEST_TOKEN);

        AuthResponseDto response = userService.register(request);

        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.token());
        assertEquals(TEST_EMAIL, response.email());
        assertEquals("USER", response.rol());
        verify(repository, times(1)).existsByEmail(TEST_EMAIL);
        verify(repository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode(TEST_PASSWORD);
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {

        RegisterRequestDto request = new RegisterRequestDto("New User", TEST_EMAIL, "987654321", TEST_PASSWORD);
        when(repository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(request);
        });
        assertEquals("El email ya esta en uso", exception.getMessage());
        verify(repository, times(1)).existsByEmail(TEST_EMAIL);
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenSuccessful() {

        LoginRequestDto request = new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD);
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtService.generateToken(testUser.getEmail(), testUser.getRol())).thenReturn(TEST_TOKEN);

        AuthResponseDto response = userService.login(request);

        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.token());
        verify(repository, times(1)).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, times(1)).matches(TEST_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {

        LoginRequestDto request = new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD);
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(request);
        });
        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(repository, times(1)).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_ShouldThrowException_WhenPasswordIncorrect() {

        LoginRequestDto request = new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD);
        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(request);
        });
        assertEquals("Usuario o contraseña incorrectos", exception.getMessage());
        verify(repository, times(1)).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, times(1)).matches(TEST_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    void getProfile_ShouldReturnUser_WhenFound() {

        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        User user = userService.getProfile(TEST_EMAIL);

        assertNotNull(user);
        assertEquals(TEST_EMAIL, user.getEmail());
        verify(repository, times(1)).findByEmail(TEST_EMAIL);
    }

    @Test
    void getProfile_ShouldThrowException_WhenUserNotFound() {

        when(repository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getProfile(TEST_EMAIL));
        verify(repository, times(1)).findByEmail(TEST_EMAIL);
    }

    @Test
    void getProfileByEmail_ShouldReturnUserDTO_WhenFound() {

        UserDTO userDTO = new UserDTO(USER_ID, "Test User", TEST_EMAIL, "USER");
        when(repository.findUserDTOByEmail(TEST_EMAIL)).thenReturn(Optional.of(userDTO));

        UserDTO actualDTO = userService.getProfileByEmail(TEST_EMAIL);

        assertNotNull(actualDTO);
        assertEquals(TEST_EMAIL, actualDTO.getEmail());
        verify(repository, times(1)).findUserDTOByEmail(TEST_EMAIL);
    }

    @Test
    void deleteUser_ShouldSucceed_WhenUserFound() {

        when(repository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        doNothing().when(repository).delete(testUser);

        userService.deleteUser(USER_ID);

        verify(repository, times(1)).findById(USER_ID);
        verify(repository, times(1)).delete(testUser);
    }

    @Test
    void deleteUser_ShouldThrowException_WhenUserNotFound() {

        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(USER_ID);
        });
        assertEquals("El user no existe", exception.getMessage());
        verify(repository, times(1)).findById(USER_ID);
        verify(repository, never()).delete(any(User.class));
    }

    @Test
    void updatedUser_ShouldUpdateAndReturnUser_WhenFound() {

        User updatedData = new User();
        updatedData.setName("Admin Updated Name");
        updatedData.setEmail("new@example.com");
        updatedData.setPhone("999");
        updatedData.setRol("ADMIN");
        updatedData.setPassword("newPassword");

        when(repository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(updatedData.getPassword())).thenReturn("newEncodedHash");
        when(repository.save(any(User.class))).thenReturn(updatedData);

        User result = userService.updatedUser(USER_ID, updatedData);

        assertEquals("Admin Updated Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("ADMIN", result.getRol());
        verify(passwordEncoder, times(1)).encode("newPassword");
        verify(repository, times(1)).save(testUser); // Verifica que guarde la instancia existente
    }

    @Test
    void updateUserProfile_ShouldUpdateNameOnly_WhenSuccessful() {

        UpdateUserDTO updateDto = new UpdateUserDTO("New Name", null, null);
        when(repository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(repository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUserProfile(USER_ID, updateDto);

        assertEquals("New Name", result.getName());

        assertEquals(ENCODED_PASSWORD, result.getPassword());
        verify(repository, times(1)).findById(USER_ID);
        verify(repository, times(1)).save(testUser);
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateUserProfile_ShouldUpdatePassword_WhenSuccessful() {

        String newPass = "super_secure_new";
        UpdateUserDTO updateDto = new UpdateUserDTO(null, TEST_PASSWORD, newPass);

        when(repository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode(newPass)).thenReturn("newEncodedHash");
        when(repository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUserProfile(USER_ID, updateDto);

        verify(passwordEncoder, times(1)).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(passwordEncoder, times(1)).encode(newPass);
        verify(repository, times(1)).save(testUser);
    }

    @Test
    void updateUserProfile_ShouldThrowException_WhenCurrentPasswordIncorrect() {
        String newPass = "super_secure_new";
        UpdateUserDTO updateDto = new UpdateUserDTO(null, "wrong_password", newPass);

        when(repository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", ENCODED_PASSWORD)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserProfile(USER_ID, updateDto);
        });
        assertEquals("La contraseña actual es incorrecta o esta vacia", exception.getMessage());
        verify(passwordEncoder, times(1)).matches("wrong_password", ENCODED_PASSWORD);
        verify(passwordEncoder, never()).encode(anyString());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_ShouldThrowException_WhenUserNotFound() {
        UpdateUserDTO updateDto = new UpdateUserDTO("New Name", null, null);
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> userService.updateUserProfile(USER_ID, updateDto));
        verify(repository, times(1)).findById(USER_ID);
        verify(repository, never()).save(any(User.class));
    }

    @Test
    void findIdByEmail_ShouldReturnId_WhenFound() {
        when(repository.findIdByEmail(TEST_EMAIL)).thenReturn(Optional.of(USER_ID));

        Optional<Long> result = userService.findIdByEmail(TEST_EMAIL);

        assertTrue(result.isPresent());
        assertEquals(USER_ID, result.get());
        verify(repository, times(1)).findIdByEmail(TEST_EMAIL);
    }

    @Test
    void findIdByEmail_ShouldReturnEmptyOptional_WhenNotFound() {
        when(repository.findIdByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        Optional<Long> result = userService.findIdByEmail(TEST_EMAIL);

        assertTrue(result.isEmpty());
        verify(repository, times(1)).findIdByEmail(TEST_EMAIL);
    }
}
