package mca.visareg.dashboard.controller;

import mca.visareg.contracts.registration.CompleteRegistrationResponse;
import mca.visareg.contracts.registration.RegistrationInfo;
import mca.visareg.dashboard.client.DashboardRegistrationClient;
import mca.visareg.dashboard.exception.DashboardServiceException;
import mca.visareg.dashboard.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/frro")
public class FrroOfficerController {

    private final DashboardRegistrationClient registrationClient;

    public FrroOfficerController(DashboardRegistrationClient registrationClient) {
        this.registrationClient = registrationClient;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String officerId = userDetails != null ? userDetails.getOfficerId() : null;
        model.addAttribute("officerId", officerId);
        model.addAttribute("officerUsername", userDetails != null ? userDetails.getUsername() : "FRRO Officer");

        try {
            List<RegistrationInfo> registrations = registrationClient.listRegistrations(officerId);
            model.addAttribute("registrations", registrations);
        } catch (DashboardServiceException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        return "frro";
    }

    @PostMapping("/verify")
    public String verifyRegistration(@RequestParam("registrationId") String registrationId,
                                     RedirectAttributes redirectAttributes) {
        try {
            CompleteRegistrationResponse response = registrationClient.completeRegistration(registrationId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration " + registrationId + " verified successfully! Status is now REGISTERED.");
        } catch (DashboardServiceException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to complete registration " + registrationId + ": " + e.getMessage());
        }
        return "redirect:/frro";
    }

    @GetMapping("/map")
    public String viewMap(@RequestParam(value = "registrationId", required = false) String registrationId,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          Model model) {
        String officerId = userDetails != null ? userDetails.getOfficerId() : null;
        model.addAttribute("officerId", officerId);

        try {
            List<RegistrationInfo> registrations = registrationClient.listRegistrations(officerId);
            model.addAttribute("registrations", registrations);

            if (registrationId != null && !registrationId.isBlank()) {
                registrations.stream()
                        .filter(r -> r.getId().equalsIgnoreCase(registrationId.trim()))
                        .findFirst()
                        .ifPresent(r -> model.addAttribute("selectedReg", r));
            }
        } catch (DashboardServiceException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }

        return "map";
    }
}
