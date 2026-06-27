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

    @ResponseBody
    @PostMapping("/api/send-otp")
    public Map<String, String> sendOtp(@RequestParam("email") String email) {
        try {
            String otp = otpService.generateAndStoreOtp(email);
            String body = "<p>Your OTP is:</p><h1 style='color: #e04a32; font-size: 42px; letter-spacing: 6px; margin: 20px 0;'>" + otp + "</h1><p>This OTP expires in 10 minutes.</p>";
            emailService.sendHtmlEmail(email, "Your StaffLaunch OTP", "Registration OTP", body, null, null);
        } catch (Exception e) {
            System.out.println("Failed to send OTP email");
        }
        return Map.of("status", "success");
    }

    @ResponseBody
    @PostMapping("/api/verify-otp")
    public Map<String, Boolean> verifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp) {
        boolean isValid = otpService.verifyOtp(email, otp);
        return Map.of("valid", isValid);
    }

    @GetMapping("/") public String root() { return "redirect:/home"; }
    @GetMapping("/home") public String home() { return "home"; }
    @GetMapping("/login") public String login() { return "login"; }

    // CRITICAL FIX: Handles anonymous users safely so the page loads without 500 errors
    @GetMapping("/signup") 
    public String signup(Model model) { 
        model.addAttribute("employee", new Employee());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        model.addAttribute("isAdmin", isAdmin);
        return "add-employee"; 
    }

    @PostMapping("/add-user")
    public String register(@Valid @ModelAttribute("employee") Employee employee, BindingResult result) {
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
                
                try {
                    String adminBody = "<p>Employee <b>" + existing.getFirstName() + " " + existing.getLastName() + "</b> has completed their registration and is requesting platform access.</p>";
                    emailService.sendHtmlEmail("yashaswiniumesh157@gmail.com", "New Access Request", "Access Request Submitted", adminBody, "Review Approvals", "https://stafflaunch-portal.onrender.com/employees/approvals");
                } catch (Exception e) { System.out.println("Email trigger failed safely."); }
                
                return "redirect:/login?success";
            }
            result.rejectValue("email", "error.user", "Email already exists.");
            return "add-employee";
        }
        
        employee.setPassword(encoder.encode(employee.getPassword()));
        employee.setRole("ROLE_USER");
        repository.save(employee);
        
        try {
            String adminBody = "<p>A new user, <b>" + employee.getFirstName() + " " + employee.getLastName() + "</b>, has requested platform access.</p>";
            emailService.sendHtmlEmail("yashaswiniumesh157@gmail.com", "New Access Request", "Access Request Submitted", adminBody, "Review Approvals", "https://stafflaunch-portal.onrender.com/employees/approvals");
        } catch (Exception e) { System.out.println("Email trigger failed safely."); }
        
        return "redirect:/login?pending";
    }

    @PostMapping("/employees/add-manual")
    public String adminAddManual(@ModelAttribute("employee") Employee employee) {
        employee.setRole("ROLE_USER");
        employee.setEnabled(true);
        employee.setAssetAcknowledged(true);
        
        if (employee.getAssets() != null && !employee.getAssets().isEmpty()) {
            bundleRepo.findByDescription(employee.getAssets()).ifPresent(employee::setAssetBundle);
        }
        repository.save(employee);
        
        try {
            String body = "<p>Hello " + employee.getFirstName() + ",</p><p>Your basic profile has been created in the StaffLaunch directory by the IT Administrator.</p><p>Please complete your registration and set your secure password by clicking the link below.</p>";
            emailService.sendHtmlEmail(employee.getEmail(), "Action Required: Complete Registration", "Welcome to the Team!", body, "Complete Sign Up", "https://stafflaunch-portal.onrender.com/signup");
        } catch (Exception e) { System.out.println("Email trigger failed safely."); }
        
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
            long pendingTickets = techIssueRepository.findAll().stream().filter(t -> t.getStatus().equals("Pending")).count();
            model.addAttribute("pendingTickets", pendingTickets);
            return "admin-dashboard";
        }
        model.addAttribute("myIssues", techIssueRepository.findByEmployeeIdOrderByReportDateDesc(user.getId()));
        return "employee-dashboard";
    }

    @PostMapping("/employees/update/{id}")
    public String update(@PathVariable("id") long id, 
                         @ModelAttribute("employee") Employee employee, 
                         @RequestParam(value = "oldPassword", required = false) String oldPassword, 
                         @RequestParam(value = "newPassword", required = false) String newPassword, 
                         @RequestParam(value = "confirmPassword", required = false) String confirmPassword) {
        
        Employee existing = repository.findById(id).orElseThrow();
        boolean credentialsChanged = false;

        if (employee.getUsername() != null && !employee.getUsername().equals(existing.getUsername())) { existing.setUsername(employee.getUsername()); credentialsChanged = true; }
        if (employee.getEmail() != null && !employee.getEmail().equals(existing.getEmail())) { existing.setEmail(employee.getEmail()); credentialsChanged = true; }
        if (employee.getFirstName() != null) existing.setFirstName(employee.getFirstName());
        if (employee.getLastName() != null) existing.setLastName(employee.getLastName());
        if (employee.getPhoneNumber() != null) existing.setPhoneNumber(employee.getPhoneNumber());
        if (employee.getDob() != null) existing.setDob(employee.getDob());
        
        if (employee.getAssets() != null && !employee.getAssets().isEmpty()) {
            bundleRepo.findByDescription(employee.getAssets()).ifPresent(existing::setAssetBundle);
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUsername = auth.getName();
        
        boolean isSelfUpdate = existing.getUsername().equals(currentUsername) || existing.getEmail().equals(currentUsername);

        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (!isAdmin || isSelfUpdate) {
                if (oldPassword != null && !encoder.matches(oldPassword, existing.getPassword())) return "redirect:/employees/dashboard?error=wrongpassword";
                if (!newPassword.equals(confirmPassword)) return "redirect:/employees/dashboard?error=passwordmismatch";
            }
            existing.setPassword(encoder.encode(newPassword));
            credentialsChanged = true;
        }
        
        repository.save(existing);
        
        if (isAdmin && !isSelfUpdate) {
            return "redirect:/employees/list";
        }
        
        if (credentialsChanged && isSelfUpdate) {
            return "redirect:/login?logout"; 
        }
        
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
        issue.setResolutionType("Awaiting Support");
        issue.setReportDate(LocalDateTime.now());
        techIssueRepository.save(issue);
        
        try {
            String adminBody = "<p>Employee <b>" + user.getFirstName() + " " + user.getLastName() + "</b> has raised a new IT Support Ticket.</p><br><p><b>Category:</b> " + issueCategory + "<br><b>Description:</b> " + description + "</p>";
            emailService.sendHtmlEmail("yashaswiniumesh157@gmail.com", "New Support Ticket Raised", "New IT Ticket", adminBody, "View Tickets", "https://stafflaunch-portal.onrender.com/admin/tickets");
        } catch (Exception e) { System.out.println("Email trigger failed safely."); }

        return "redirect:/employees/dashboard?issueReported";
    }

    @PostMapping("/employees/cancel-issue/{id}")
    public String cancelIssue(@PathVariable("id") long id) {
        TechIssue issue = techIssueRepository.findById(id).orElseThrow();
        issue.setStatus("Withdrawn");
        issue.setResolutionType("Cancelled by User");
        techIssueRepository.save(issue);
        return "redirect:/employees/dashboard?issueCancelled";
    }

    @GetMapping("/admin/tickets")
    public String adminTickets(Model model) {
        model.addAttribute("tickets", techIssueRepository.findAllByOrderByReportDateDesc());
        return "admin-tech-issues";
    }

    @PostMapping("/admin/resolve-ticket/{id}")
    public String resolveTicket(@PathVariable("id") long id, @RequestParam("resolutionType") String resolutionType) {
        TechIssue issue = techIssueRepository.findById(id).orElseThrow();
        issue.setStatus("Closed");
        issue.setResolutionType(resolutionType);
        techIssueRepository.save(issue);

        try {
            if (issue.getEmployee() != null && issue.getEmployee().getEmail() != null) {
                String body = "<p>Hello " + issue.getEmployee().getFirstName() + ",</p><p>Your IT Support Ticket regarding <b>'" + issue.getIssueCategory() + "'</b> has been resolved by the Administrator.</p><br><p><b>Resolution Action:</b> " + resolutionType + "</p><p>If you need further assistance, please open a new ticket from your dashboard.</p>";
                emailService.sendHtmlEmail(issue.getEmployee().getEmail(), "IT Support Ticket Resolved", "Ticket Resolved", body, "View Dashboard", "https://stafflaunch-portal.onrender.com/employees/dashboard");
            }
        } catch (Exception e) { 
            System.out.println("Email trigger failed safely: " + e.getMessage()); 
        }

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
        
        try {
            String body = "<p>Hello " + e.getFirstName() + ",</p><p>Great news! Your employee account has been officially <b>approved</b> by the IT Administrator.</p><p>You can now log into the StaffLaunch Enterprise Portal to view your dashboard, acknowledge your IT assets, and access the resource library.</p>";
            emailService.sendHtmlEmail(e.getEmail(), "Account Approved - Welcome to StaffLaunch!", "Account Approved", body, "Login Now", "https://stafflaunch-portal.onrender.com/login");
        } catch (Exception ex) { System.out.println("Email trigger failed safely."); }

        return "redirect:/employees/approvals";
    }

    @GetMapping("/employees/edit/{id}")
    public String showEdit(@PathVariable("id") long id, Model model) {
        Employee employee = repository.findById(id).orElseThrow();
        model.addAttribute("employee", employee);
        return "update-employee";
    }

    @GetMapping("/employees/delete/{id}")
    public String delete(@PathVariable("id") long id) {
        Employee emp = repository.findById(id).orElseThrow();
        if (!"admin".equals(emp.getUsername())) {
            repository.deleteById(id);
        }
        return "redirect:/employees/list";
    }

    public static class Article {
        public String title; public String imageUrl; public String htmlContent;
        public Article(String title, String imageUrl, String htmlContent) { this.title = title; this.imageUrl = imageUrl; this.htmlContent = htmlContent; }
    }
    
    private static final List<String> ARTICLE_ORDER = Arrays.asList("welcome-manual", "engineering-excellence", "diversity-equity-inclusion", "agile-fundamentals", "data-privacy");

    private final Map<String, Article> knowledgeBase = new HashMap<>() {{
        put("welcome-manual", new Article("The Welcome Manual", "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=1200&q=80", "<h4>Welcome to the Team!</h4><p>We are thrilled to have you onboard.</p>"));
        put("engineering-excellence", new Article("Engineering Excellence", "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80", "<h4>Our Code Philosophy</h4><p>We operate on a simple principle: <i>We build it, we run it.</i></p>"));
        put("diversity-equity-inclusion", new Article("Diversity, Equity & Inclusion (DEI)", "https://images.unsplash.com/photo-1531497865144-0464ef8fb9a9?auto=format&fit=crop&w=1200&q=80", "<h4>Building a Workspace for Everyone</h4><p>We are committed to fostering an environment where everyone feels valued and heard.</p>"));
        put("agile-fundamentals", new Article("Agile Fundamentals", "https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1200&q=80", "<h4>How We Work</h4><p>We utilize a modified Agile Scrum methodology to ensure rapid delivery.</p>"));
        put("data-privacy", new Article("Data Privacy & Security Protocols", "https://images.unsplash.com/photo-1611224923853-80b023f02d71?auto=format&fit=crop&w=1200&q=80", "<h4>Protecting Our Users</h4><p>Security is everyone's responsibility.</p>"));
    }};

    @GetMapping("/employees/resources/{slug}")
    public String viewArticle(@PathVariable String slug, Model model) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        Article article = knowledgeBase.get(slug);
        
        if (article == null) return "redirect:/employees/dashboard"; 
        
        boolean isRead = user.getReadArticles() != null && user.getReadArticles().contains(slug);
        int currentIndex = ARTICLE_ORDER.indexOf(slug);
        String nextSlug = ARTICLE_ORDER.get((currentIndex + 1) % ARTICLE_ORDER.size());
        String prevSlug = ARTICLE_ORDER.get((currentIndex - 1 + ARTICLE_ORDER.size()) % ARTICLE_ORDER.size());
        
        model.addAttribute("article", article); 
        model.addAttribute("slug", slug); 
        model.addAttribute("isRead", isRead); 
        model.addAttribute("nextSlug", nextSlug); 
        model.addAttribute("prevSlug", prevSlug);
        
        return "article";
    }
    
    @PostMapping("/employees/resources/{slug}/mark-read")
    public String markArticleRead(@PathVariable String slug) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Employee user = repository.findByEmailOrUsername(currentUsername).orElseThrow();
        
        if (!user.getReadArticles().contains(slug)) { 
            user.getReadArticles().add(slug); 
            repository.save(user); 
        }
        return "redirect:/employees/resources/" + slug;
    }
}