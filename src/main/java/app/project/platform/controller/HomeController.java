package app.project.platform.controller;

import app.project.platform.entity.Content;
import app.project.platform.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ContentRepository contentRepository;

    @GetMapping("/")
    public String home(Model model) {

        List<Content> recentContents = contentRepository.findTop3ByOrderByCreatedDateDesc();

        model.addAttribute("recentContents", recentContents);

        return "home";
    }

}
