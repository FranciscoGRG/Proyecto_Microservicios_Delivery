package com.fran.users_service.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fran.users_service.app.controllers.UserController;
import com.fran.users_service.app.dtos.AuthResponseDto;
import com.fran.users_service.app.dtos.LoginRequestDto;
import com.fran.users_service.app.dtos.RegisterRequestDto;
import com.fran.users_service.app.dtos.UpdateUserDTO;
import com.fran.users_service.app.dtos.UserDTO;
import com.fran.users_service.app.models.User;
import com.fran.users_service.app.services.JwtService;
import com.fran.users_service.app.services.UserService;

import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwJwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private final String API_URL = "/api/v1/users";
    private final String TEST_EMAIL = "test@example.com";
    private final String TEST_PASSWORD = "password123";
    private final Long USER_ID = 1L;

    private User testUser;
    private AuthResponseDto authResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setName("Test User");
        testUser.setEmail(TEST_EMAIL);
        testUser.setRol("USER");

        authResponse = new AuthResponseDto("test.token.jwt", TEST_EMAIL, "USER");
    }

    @Test
    @WithMockUser
    void register_ShouldReturnAuthResponse_AndStatus200() throws Exception {

        RegisterRequestDto requestDto = new RegisterRequestDto("New User", TEST_EMAIL, "123456789", TEST_PASSWORD);
        when(userService.register(any(RegisterRequestDto.class))).thenReturn(authResponse);

        mockMvc.perform(post(API_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.token").exists());

        verify(userService, times(1)).register(any(RegisterRequestDto.class));
    }

    @Test
    @WithMockUser
    void register_ShouldReturnBadRequest_WhenServiceThrowsRuntimeException() throws Exception {

        RegisterRequestDto requestDto = new RegisterRequestDto("New User", TEST_EMAIL, "123456789", TEST_PASSWORD);

        when(userService.register(any(RegisterRequestDto.class)))
                .thenThrow(new RuntimeException("El email ya esta en uso"));

        mockMvc.perform(post(API_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))

                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).register(any(RegisterRequestDto.class));
    }

    @Test
    @WithMockUser
    void login_ShouldReturnAuthResponse_AndStatus200() throws Exception {

        LoginRequestDto requestDto = new LoginRequestDto(TEST_EMAIL, TEST_PASSWORD);
        when(userService.login(any(LoginRequestDto.class))).thenReturn(authResponse);

        mockMvc.perform(post(API_URL + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));

        verify(userService, times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = { "USER" })
    void profile_ShouldReturnUser_AndStatus200_WhenAuthenticated() throws Exception {

        when(userService.getProfile(TEST_EMAIL)).thenReturn(testUser);

        mockMvc.perform(post(API_URL + "/profile")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));

        verify(userService, times(1)).getProfile(TEST_EMAIL);
    }

    @Test
    void profile_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {

        mockMvc.perform(post(API_URL + "/profile")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void getProfileByEmail_ShouldReturnUserDTO_AndStatus200() throws Exception {

        UserDTO userDTO = new UserDTO(USER_ID, "Test User", TEST_EMAIL, "USER");
        when(userService.getProfileByEmail(TEST_EMAIL)).thenReturn(userDTO);

        mockMvc.perform(get(API_URL + "/profile/{email}", TEST_EMAIL)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.rol").value("USER"));

        verify(userService, times(1)).getProfileByEmail(TEST_EMAIL);
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void getProfileByEmail_ShouldReturnInternalServerError_WhenNotFound() throws Exception {

        when(userService.getProfileByEmail(TEST_EMAIL))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        mockMvc.perform(get(API_URL + "/profile/{email}", TEST_EMAIL)
                .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void deleteUser_ShouldReturnNoContent_AndStatus204() throws Exception {

        doNothing().when(userService).deleteUser(USER_ID);

        mockMvc.perform(delete(API_URL + "/{id}", USER_ID)
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(USER_ID);
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void deleteUser_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {

        doThrow(new RuntimeException("El user no existe")).when(userService).deleteUser(USER_ID);

        mockMvc.perform(delete(API_URL + "/{id}", USER_ID)
                .with(csrf()))
                .andExpect(status().isInternalServerError());

        verify(userService, times(1)).deleteUser(USER_ID);
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = { "USER" })
    void updateUser_ShouldReturnUpdatedUser_AndStatus200_WhenSuccessful() throws Exception {

        UpdateUserDTO requestDto = new UpdateUserDTO("New Name", null, null);

        when(userService.findIdByEmail(TEST_EMAIL)).thenReturn(Optional.of(USER_ID));
        when(userService.updateUserProfile(eq(USER_ID), any(UpdateUserDTO.class))).thenReturn(testUser);

        mockMvc.perform(put(API_URL + "/updateProfile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));

        verify(userService, times(1)).findIdByEmail(TEST_EMAIL);
        verify(userService, times(1)).updateUserProfile(eq(USER_ID), any(UpdateUserDTO.class));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = { "USER" })
    void updateUser_ShouldReturnBadRequest_WhenRuntimeException() throws Exception {

        UpdateUserDTO requestDto = new UpdateUserDTO("New Name", "wrong_pass", "new_pass");

        when(userService.findIdByEmail(TEST_EMAIL)).thenReturn(Optional.of(USER_ID));

        when(userService.updateUserProfile(eq(USER_ID), any(UpdateUserDTO.class)))
                .thenThrow(new RuntimeException("La contraseña actual es incorrecta o esta vacia"));

        mockMvc.perform(put(API_URL + "/updateProfile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).findIdByEmail(TEST_EMAIL);
        verify(userService, times(1)).updateUserProfile(eq(USER_ID), any(UpdateUserDTO.class));
    }

    @Test
    @WithMockUser(username = TEST_EMAIL, roles = { "USER" })
    void updateUser_ShouldReturnNotFound_WhenIdNotFoundInService() throws Exception {

        UpdateUserDTO requestDto = new UpdateUserDTO("New Name", null, null);

        when(userService.findIdByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        mockMvc.perform(put(API_URL + "/updateProfile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).findIdByEmail(TEST_EMAIL);
        verify(userService, never()).updateUserProfile(any(), any());
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    void findAll_ShouldReturnListOfUsers_AndStatus200() throws Exception {
        List<User> expectedList = Arrays.asList(testUser, new User());
        when(userService.findAll()).thenReturn(expectedList);

        mockMvc.perform(get(API_URL + "/findAll")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].email").value(TEST_EMAIL));

        verify(userService, times(1)).findAll();
    }
}