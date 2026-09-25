package mca.visareg.dashboard.controller;

import mca.visareg.contracts.registration.CreateRegistrationResponse;
import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.contracts.visa.SubmitApplicationResponse;
import mca.visareg.dashboard.client.DashboardRegistrationClient;
import mca.visareg.dashboard.client.DashboardVisaClient;
import mca.visareg.dashboard.exception.DashboardServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicController {

    private final DashboardVisaClient visaClient;
    private final DashboardRegistrationClient registrationClient;

    public PublicController(DashboardVisaClient visaClient, DashboardRegistrationClient registrationClient) {
        this.visaClient = visaClient;
        this.registrationClient = registrationClient;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "login";
    }

    @GetMapping("/apply")
    public String applyForm() {
        return "apply";
    }

    @PostMapping("/apply")
    public String handleApply(@RequestParam("applicantName") String applicantName,
                              @RequestParam("passportNumber") String passportNumber,
                              @RequestParam("visaType") String visaType,
                              @RequestParam("portOfArrival") String portOfArrival,
                              Model model) {
        try {
            SubmitApplicationResponse response = visaClient.submitApplication(
                    applicantName.trim(),
                    passportNumber.trim(),
                    visaType.trim(),
                    portOfArrival.trim()
            );
            model.addAttribute("successMessage", response.getMessage());
            model.addAttribute("applicationId", response.getApplicationId());
            return "apply-success";
        } catch (DashboardServiceException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("applicantName", applicantName);
            model.addAttribute("passportNumber", passportNumber);
            model.addAttribute("visaType", visaType);
            model.addAttribute("portOfArrival", portOfArrival);
            return "apply";
        }
    }

    @GetMapping("/track")
    public String trackApplication(@RequestParam(value = "id", required = false) String id, Model model) {
        if (id != null && !id.trim().isEmpty()) {
            try {
                ApplicationInfo app = visaClient.getApplication(id.trim());
                if (app != null) {
                    model.addAttribute("visaApp", app);
                } else {
                    model.addAttribute("errorMessage", "No application found with ID: " + id.trim());
                }
            } catch (DashboardServiceException e) {
                model.addAttribute("errorMessage", e.getMessage());
            }
            model.addAttribute("searchId", id.trim());
        }
        return "track";
    }

    @GetMapping("/register")
    public String registerForm(@RequestParam(value = "applicationId", required = false) String applicationId, Model model) {
        if (applicationId != null) {
            model.addAttribute("applicationId", applicationId);
        }
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam("applicationId") String applicationId,
                                 @RequestParam("address") String address,
                                 Model model) {
        try {
            CreateRegistrationResponse response = registrationClient.createRegistration(
                    applicationId.trim(),
                    address.trim()
            );
            model.addAttribute("registration", response.getRegistration());
            model.addAttribute("successMessage", response.getMessage());
            return "register-success";
        } catch (DashboardServiceException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("applicationId", applicationId);
            model.addAttribute("address", address);
            return "register";
        }
    }
}
