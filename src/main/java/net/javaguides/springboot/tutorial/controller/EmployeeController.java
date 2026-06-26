package net.javaguides.springboot.tutorial.controller;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import net.javaguides.springboot.tutorial.entity.Employee;
import net.javaguides.springboot.tutorial.entity.TechIssue;
import net.javaguides.springboot.tutorial.entity.HardwareBundle;
import net.javaguides.springboot.tutorial.repository.EmployeeRepository;
import net.javaguides.springboot.tutorial.repository.TechIssueRepository;
import net.javaguides.springboot.tutorial.repository.HardwareBundleRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class EmployeeController {

    private final EmployeeRepository repository;
    private final TechIssueRepository techIssueRepository;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final OtpService otpService;
    private final HardwareBundleRepository bundleRepo;

    public EmployeeController(EmployeeRepository repository, TechIssueRepository techIssueRepository, PasswordEncoder encoder, EmailService emailService, OtpService otpService, HardwareBundleRepository bundleRepo) {
        this.repository = repository;
        this.techIssueRepository = techIssueRepository;
        this.encoder = encoder;
        this.emailService = emailService;
        this.otpService = otpService;
        this.bundleRepo = bundleRepo;
    }

    @PostConstruct
    public void seedAdmin() {
        try {
            if (bundleRepo.count() == 0) {
                bundleRepo.save(new HardwareBundle("Power User", "Workstation: MacBook Pro M3 + 27-inch Dell Monitor"));
                bundleRepo.save(new HardwareBundle("Standard", "Workstation: Dell Latitude + Wireless Mouse"));
                bundleRepo.save(new HardwareBundle("Design Bundle", "Mobile: iPad Air + Apple Pencil"));
                bundleRepo.save(new HardwareBundle("Remote Bundle", "Remote: Lenovo ThinkPad + Global VPN Router"));
            }
            if (repository.findByEmailOrUsername("admin").isEmpty()) {
                Employee admin = new Employee();
                admin.setFirstName("Admin");
                admin.setUsername("admin");
                admin.setEmail("admin@drait.edu.in");
                admin.setPassword(encoder.encode("kodnest123"));
                admin.setRole("ROLE_ADMIN");
                admin.setEnabled(true);
                repository.save(admin);
            }
        } catch (Exception e) {
            System.out.println("Initialization skipped: " + e.getMessage());
        }
    }

    // --- OTP API ENDPOINTS ---
    @ResponseBody
    @PostMapping("/api/send-otp")
    public Map<String, String> sendOtp(@RequestParam("email") String email) {
        String otp = otpService.generateAndStoreOtp(email);
        String body = "<p>Your OTP is:</p><h1 style='color: #e04a32; font-size: 42px; letter-spacing: 6px; margin: 20px 0;'>" + otp + "</h1><p>This OTP expires in 10 minutes.</p>";
        emailService.sendHtmlEmail(email, "Your StaffLaunch OTP", "Registration OTP", body, null, null);
        return Map.of("status", "success");
    }

    @ResponseBody
    @PostMapping("/api/verify-otp")
    public Map<String, Boolean> verifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
        boolean isValid = otpService.verifyOtp(email, otp);
        return Map.of("valid", isValid);
    }

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

    @GetMapping("/home")
    public String home() { return "home"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/signup")
    public String signup(Employee employee) { return "add-employee"; }

    @PostMapping("/add-user")
    public String register(@Valid Employee employee, BindingResult result) {
        if (result.hasErrors()) return "add-employee";
        Optional<Employee> existingUser = repository.findByEmail(employee.getEmail());
        if (existingUser.isPresent()) {
            Employee existing = existingUser.get();
            if (existing.isEnabled() && (existing.getPassword() == null || existing.getPassword().isEmpty())) {
                existing.setUsername(employee.getUsername());
                existing.setPassword(encoder.encode(employee.getPassword()));
                existing.setPhoneNumber(employee.getPhoneNumber());
                existing.setDob(employee.getDob());
                existing.setAssetAcknowledged(true);
                repository.save(existing);
                String adminBody = "<p>Employee <b>" + existing.getFirstName() + "</b> has completed their registration.</p>";
                emailService.sendHtmlEmail("yashaswiniumesh157@gmail.com", "New Access Request", "Access Request Submitted", adminBody, "Review Approvals", "https://stafflaunch-portal.onrender.com/employees/approvals");
                return "redirect:/login?success";
            }
            result.rejectValue("email", "error.user", "Email already exists.");
            return "add-employee";
        }
        employee.setPassword(encoder.encode(employee.getPassword()));
        employee.setRole("ROLE_USER");
        repository.save(employee);
        String adminBody = "<p>A new user, <b>" + employee.getFirstName() + "</b>, has requested platform access.</p>";
        emailService.sendHtmlEmail("yashaswiniumesh157@gmail.com", "New Access Request", "Access Request Submitted", adminBody, "Review Approvals", "https://stafflaunch-portal.onrender.com/employees/approvals");
        return "redirect:/login?pending";
    }

    @PostMapping("/employees/add-manual")
    public String adminAddManual(Employee employee) {
        employee.setRole("ROLE_USER");
        employee.setEnabled(true);
        employee.setAssetAcknowledged(true);
        repository.save(employee);
        String body = "<p>Hello " + employee.getFirstName() + ", your profile is ready. Please complete your registration:</p>";
        emailService.sendHtmlEmail(employee.getEmail(), "Action Required: Complete Registration", "Welcome to the Team!", body, "Complete Sign Up", "https://stafflaunch-portal.onrender.com/signup");
        return "redirect:/employees/list";
    }

    @GetMapping("/employees/dashboard")
    public String dashboard(Model model) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        model.addAttribute("user", user);
        if (user.getRole().equals("ROLE_ADMIN")) {
            var all = repository.findAll();
            model.addAttribute("pendingCount", all.stream().filter(e -> !e.isEnabled()).count());
            model.addAttribute("activeCount", all.stream().filter(Employee::isEnabled).count());
            model.addAttribute("pendingTickets", techIssueRepository.findAll().stream().filter(t -> t.getStatus().equals("Pending")).count());
            return "admin-dashboard";
        }
        model.addAttribute("myIssues", techIssueRepository.findByEmployeeIdOrderByReportDateDesc(user.getId()));
        return "employee-dashboard";
    }

    @PostMapping("/employees/update/{id}")
    public String update(@PathVariable("id") long id, @ModelAttribute("employee") Employee employee) {
        Employee existing = repository.findById(id).orElseThrow();
        if (employee.getFirstName() != null) existing.setFirstName(employee.getFirstName());
        if (employee.getLastName() != null) existing.setLastName(employee.getLastName());
        if (employee.getPhoneNumber() != null) existing.setPhoneNumber(employee.getPhoneNumber());
        if (employee.getDob() != null) existing.setDob(employee.getDob());
        
        // Link new asset bundle object
        if (employee.getAssets() != null) {
            bundleRepo.findByDescription(employee.getAssets()).ifPresent(existing::setAssetBundle);
        }
        
        repository.save(existing);
        return "redirect:/employees/dashboard?profileUpdated"; 
    }

    @GetMapping("/employees/list")
    public String list(Model model) {
        model.addAttribute("employees", repository.findAll().stream().filter(Employee::isEnabled).collect(Collectors.toList()));
        return "index";
    }

    @PostMapping("/employees/acknowledge-asset")
    public String acknowledge() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        user.setAssetAcknowledged(true);
        repository.save(user);
        return "redirect:/employees/dashboard?assetSuccess";
    }

    @PostMapping("/employees/claim-asset")
    public String claimAsset(@RequestParam("assets") String assets) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        bundleRepo.findByDescription(assets).ifPresent(user::setAssetBundle);
        user.setAssetAcknowledged(true); 
        repository.save(user);
        return "redirect:/employees/dashboard?claimSuccess";
    }

    @GetMapping("/employees/report-issue")
    public String showReportForm(Model model) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        model.addAttribute("user", user);
        return "report-issue";
    }

    @PostMapping("/employees/submit-issue")
    public String submitIssue(@RequestParam("asset") String asset, @RequestParam("issueCategory") String issueCategory, @RequestParam("description") String description) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        TechIssue issue = new TechIssue();
        issue.setEmployee(user);
        issue.setAsset(asset);
        issue.setIssueCategory(issueCategory);
        issue.setDescription(description);
        issue.setStatus("Pending");
        issue.setReportDate(LocalDateTime.now());
        techIssueRepository.save(issue);
        String adminBody = "<p>Employee <b>" + user.getFirstName() + "</b> has raised a ticket.</p>";
        emailService.sendHtmlEmail("yashaswiniumesh157@gmail.com", "New Support Ticket", "New IT Ticket", adminBody, "View Tickets", "https://stafflaunch-portal.onrender.com/admin/tickets");
        return "redirect:/employees/dashboard?issueReported";
    }

    @PostMapping("/admin/resolve-ticket/{id}")
    public String resolveTicket(@PathVariable("id") long id, @RequestParam("resolutionType") String resolutionType) {
        TechIssue issue = techIssueRepository.findById(id).orElseThrow();
        issue.setStatus("Closed");
        issue.setResolutionType(resolutionType);
        techIssueRepository.save(issue);
        String body = "<p>Your ticket regarding <b>'" + issue.getIssueCategory() + "'</b> has been resolved.</p>";
        emailService.sendHtmlEmail(issue.getEmployee().getEmail(), "IT Ticket Resolved", "Ticket Resolved", body, "View Dashboard", "https://stafflaunch-portal.onrender.com/employees/dashboard");
        return "redirect:/admin/tickets?resolved";
    }

    @GetMapping("/employees/approvals")
    public String showApprovals(Model model) {
        model.addAttribute("pendingUsers", repository.findAll().stream().filter(e -> !e.isEnabled()).collect(Collectors.toList()));
        return "approvals";
    }

    @GetMapping("/employees/approve/{id}")
    public String approve(@PathVariable("id") long id) {
        Employee e = repository.findById(id).orElseThrow();
        e.setEnabled(true);
        repository.save(e);
        String body = "<p>Your account has been <b>approved</b>.</p>";
        emailService.sendHtmlEmail(e.getEmail(), "Account Approved", "Account Approved", body, "Login Now", "https://stafflaunch-portal.onrender.com/login");
        return "redirect:/employees/approvals";
    }

    @GetMapping("/employees/delete/{id}")
    public String delete(@PathVariable("id") long id) {
        Employee emp = repository.findById(id).orElseThrow();
        if (!"admin".equals(emp.getUsername())) repository.deleteById(id);
        return "redirect:/employees/list";
    }

    // --- KNOWLEDGE BASE ---
    private final Map<String, Article> knowledgeBase = new HashMap<>() {{
        put("welcome-manual", new Article("The Welcome Manual", "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1200&q=80", "<h4>Welcome!</h4>"));
        // ... (keep the rest of your map as is)
    }};

    @GetMapping("/employees/resources/{slug}")
    public String viewArticle(@PathVariable String slug, Model model) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        model.addAttribute("article", knowledgeBase.get(slug));
        model.addAttribute("isRead", user.getReadArticles().contains(slug));
        return "article";
    }
    
    @PostMapping("/employees/resources/{slug}/mark-read")
    public String markArticleRead(@PathVariable String slug) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        if (!user.getReadArticles().contains(slug)) { user.getReadArticles().add(slug); repository.save(user); }
        return "redirect:/employees/resources/" + slug;
    }
}