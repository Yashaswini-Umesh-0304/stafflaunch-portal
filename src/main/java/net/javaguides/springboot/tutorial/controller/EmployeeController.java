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

    // --- OTP API ---
    @ResponseBody
    @PostMapping("/api/send-otp")
    public Map<String, String> sendOtp(@RequestParam("email") String email) {
        String otp = otpService.generateAndStoreOtp(email);
        String body = "<p>Your OTP is:</p><h1 style='color: #e04a32; font-size: 42px;'>" + otp + "</h1>";
        emailService.sendHtmlEmail(email, "Your StaffLaunch OTP", "Registration OTP", body, null, null);
        return Map.of("status", "success");
    }

    @ResponseBody
    @PostMapping("/api/verify-otp")
    public Map<String, Boolean> verifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
        return Map.of("valid", otpService.verifyOtp(email, otp));
    }

    // --- DASHBOARD & ACTIONS ---
    @GetMapping("/") public String root() { return "redirect:/home"; }
    @GetMapping("/home") public String home() { return "home"; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/signup") public String signup(Employee employee) { return "add-employee"; }

    @PostMapping("/add-user")
    public String register(@Valid Employee employee, BindingResult result) {
        if (result.hasErrors()) return "add-employee";
        employee.setPassword(encoder.encode(employee.getPassword()));
        employee.setRole("ROLE_USER");
        repository.save(employee);
        return "redirect:/login?pending";
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
        existing.setFirstName(employee.getFirstName());
        existing.setLastName(employee.getLastName());
        existing.setPhoneNumber(employee.getPhoneNumber());
        if (employee.getAssets() != null) bundleRepo.findByDescription(employee.getAssets()).ifPresent(existing::setAssetBundle);
        repository.save(existing);
        return "redirect:/employees/dashboard?profileUpdated"; 
    }

    @GetMapping("/employees/list") public String list(Model model) { model.addAttribute("employees", repository.findAll().stream().filter(Employee::isEnabled).collect(Collectors.toList())); return "index"; }

    @PostMapping("/employees/delete/{id}")
    public String delete(@PathVariable("id") long id) {
        Employee emp = repository.findById(id).orElseThrow();
        if (!"admin".equals(emp.getUsername())) repository.deleteById(id);
        return "redirect:/employees/list";
    }

    // --- KNOWLEDGE BASE ---
    public static class Article {
        public String title; public String imageUrl; public String htmlContent;
        public Article(String title, String imageUrl, String htmlContent) { this.title = title; this.imageUrl = imageUrl; this.htmlContent = htmlContent; }
    }
    
    private static final List<String> ARTICLE_ORDER = Arrays.asList("welcome-manual", "engineering-excellence", "diversity-equity-inclusion", "agile-fundamentals", "data-privacy");

    private final Map<String, Article> knowledgeBase = new HashMap<>() {{
        put("welcome-manual", new Article("The Welcome Manual", "url", "<h4>Welcome!</h4>"));
        put("engineering-excellence", new Article("Engineering Excellence", "url", "<h4>Philosophy</h4>"));
        put("diversity-equity-inclusion", new Article("DEI", "url", "<h4>Inclusion</h4>"));
        put("agile-fundamentals", new Article("Agile", "url", "<h4>Methodology</h4>"));
        put("data-privacy", new Article("Privacy", "url", "<h4>Security</h4>"));
    }};

    @GetMapping("/employees/resources/{slug}")
    public String viewArticle(@PathVariable String slug, Model model) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        model.addAttribute("article", knowledgeBase.get(slug));
        model.addAttribute("isRead", repository.findByEmailOrUsername(user).get().getReadArticles().contains(slug));
        return "article";
    }

    @PostMapping("/employees/resources/{slug}/mark-read")
    public String markRead(@PathVariable String slug) {
        Employee user = repository.findByEmailOrUsername(SecurityContextHolder.getContext().getAuthentication().getName()).get();
        if (!user.getReadArticles().contains(slug)) { user.getReadArticles().add(slug); repository.save(user); }
        return "redirect:/employees/resources/" + slug;
    }
}