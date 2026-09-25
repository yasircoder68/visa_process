package mca.visareg.dashboard.controller;

import mca.visareg.contracts.visa.ApplicationInfo;
import mca.visareg.contracts.visa.IssueETAResponse;
import mca.visareg.dashboard.client.DashboardVisaClient;
import mca.visareg.dashboard.exception.DashboardServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/visa-officer")
public class VisaOfficerController {

    private final DashboardVisaClient visaClient;

    public VisaOfficerController(DashboardVisaClient visaClient) {
        this.visaClient = visaClient;
    }

    @GetMapping
    public String dashboard(@RequestParam(value = "status", required = false) String filterStatus,
                            Model model) {
        try {
            List<ApplicationInfo> applications = visaClient.listApplications(filterStatus);
            model.addAttribute("applications", applications);
            model.addAttribute("selectedStatus", filterStatus);
        } catch (DashboardServiceException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "visa-officer";
    }

    @PostMapping("/approve")
    public String approveApplication(@RequestParam("applicationId") String applicationId,
                                     RedirectAttributes redirectAttributes) {
        try {
            visaClient.updateApplicationStatus(applicationId, "APPROVED");
            IssueETAResponse etaResponse = visaClient.issueETA(applicationId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Application " + applicationId + " approved successfully! ETA issued: " +
                            (etaResponse.getApplication() != null ? etaResponse.getApplication().getEtaNumber() : applicationId));
        } catch (DashboardServiceException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to approve application " + applicationId + ": " + e.getMessage());
        }
        return "redirect:/visa-officer";
    }

    @PostMapping("/reject")
    public String rejectApplication(@RequestParam("applicationId") String applicationId,
                                    RedirectAttributes redirectAttributes) {
        try {
            visaClient.updateApplicationStatus(applicationId, "REJECTED");
            redirectAttributes.addFlashAttribute("infoMessage",
                    "Application " + applicationId + " has been REJECTED.");
        } catch (DashboardServiceException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to reject application " + applicationId + ": " + e.getMessage());
        }
        return "redirect:/visa-officer";
    }
}
