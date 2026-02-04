package com.eazybytes.eazyschool.controller;

import com.eazybytes.eazyschool.model.Address;
import com.eazybytes.eazyschool.model.Person;
import com.eazybytes.eazyschool.model.Profile;
import com.eazybytes.eazyschool.repository.PersonRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Controller("profileControllerBean")
public class ProfileController {

    @Autowired
    private PersonRepository personRepository;

    @Value("${eazyschool.upload.dir:uploads}")
    private String uploadDir;

    @RequestMapping("/displayProfile")
    public ModelAndView displayMessages(Model model, Authentication authentication) {

        Person person = personRepository.readByEmail(authentication.getName());
        if (person == null) {
            return new ModelAndView("redirect:/login?logout=true");
        }

        Profile profile = new Profile();
        profile.setName(person.getName());
        profile.setMobileNumber(person.getMobileNumber());
        profile.setEmail(person.getEmail());

        // ✅ mjafton null-check (s’ka nevojë addressId > 0)
        if (person.getAddress() != null) {
            profile.setAddress1(person.getAddress().getAddress1());
            profile.setAddress2(person.getAddress().getAddress2());
            profile.setCity(person.getAddress().getCity());
            profile.setState(person.getAddress().getState());
            profile.setZipCode(person.getAddress().getZipCode());
        }

        ModelAndView mv = new ModelAndView("profile.html");
        mv.addObject("profile", profile);
        mv.addObject("profileImage", person.getProfileImage());
        return mv;
    }

    @PostMapping(value = "/updateProfile", consumes = "multipart/form-data")
    public String updateProfile(
            @Valid @ModelAttribute("profile") Profile profile,
            org.springframework.validation.BindingResult br,
            @RequestParam(name = "profilePhoto", required = false) MultipartFile profilePhoto,
            Authentication authentication,
            RedirectAttributes ra) throws Exception {

        if (br.hasErrors()) {
            return "profile.html";
        }

        Person person = personRepository.readByEmail(authentication.getName());
        if (person == null) return "redirect:/login?logout=true";

        // -------- basic fields --------
        person.setName(profile.getName());
        person.setMobileNumber(profile.getMobileNumber());

        // -------- email unique check --------
        String newEmail = profile.getEmail();
        String oldEmail = person.getEmail();
        if (newEmail != null && !newEmail.equalsIgnoreCase(oldEmail)) {
            if (personRepository.existsByEmail(newEmail)) {
                br.rejectValue("email", "email.exists", "This email is already in use.");
                return "profile.html";
            }
            person.setEmail(newEmail);
            // ⚠️ rekomandim: pas ndryshimit të email-it, user-i duhet të bëjë login prap
            ra.addFlashAttribute("successMessage", "Email updated. Please login again.");
            personRepository.save(person);
            return "redirect:/login?logout=true";
        }

        // -------- address: mos e fshi me null --------
        Address addr = person.getAddress();
        if (addr == null) {
            addr = new Address();
            person.setAddress(addr);
        }

        // vetëm nëse vjen vlerë jo-bosh, përditëso
        if (notBlank(profile.getAddress1())) addr.setAddress1(profile.getAddress1());
        if (profile.getAddress2() != null) addr.setAddress2(profile.getAddress2());
        if (notBlank(profile.getCity())) addr.setCity(profile.getCity());
        if (notBlank(profile.getState())) addr.setState(profile.getState());
        if (notBlank(profile.getZipCode())) addr.setZipCode(profile.getZipCode());

        // -------- photo upload --------
        if (profilePhoto != null && !profilePhoto.isEmpty()) {
            String contentType = profilePhoto.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                br.reject("photo.invalid", "Please upload an image file.");
                return "profile.html";
            }

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String original = profilePhoto.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf("."))
                    : "";

            String fileName = UUID.randomUUID() + ext;

            Files.copy(profilePhoto.getInputStream(),
                    uploadPath.resolve(fileName),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            person.setProfileImage(fileName);
        }

        personRepository.save(person);
        ra.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/displayProfile";
    }

    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

}
