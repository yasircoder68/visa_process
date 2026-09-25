package mca.visareg.dashboard;

import mca.visareg.dashboard.client.DashboardRegistrationClient;
import mca.visareg.dashboard.client.DashboardVisaClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardVisaClient visaClient;

    @MockBean
    private DashboardRegistrationClient registrationClient;

    @Test
    void testPublicPagesAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/apply"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/track"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedPagesRedirectToLogin() throws Exception {
        mockMvc.perform(get("/visa-officer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/frro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "visaofficer01", roles = {"VISA_OFFICER"})
    void testVisaOfficerCanAccessVisaPortal() throws Exception {
        mockMvc.perform(get("/visa-officer"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "frroofficer02", roles = {"FRRO_OFFICER"})
    void testFrroOfficerCanAccessFrroPortal() throws Exception {
        mockMvc.perform(get("/frro"))
                .andExpect(status().isOk());
    }
}
