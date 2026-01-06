package app.project.platform.controller;

import app.project.platform.dto.ContentDTO;
import app.project.platform.entity.Content;
import app.project.platform.repository.ContentRepository;
import app.project.platform.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/content/")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @GetMapping("list")
    public String getList(@Valid ContentDTO contentDTO
            , BindingResult bindingResult
            , Model model) {

        if (bindingResult.hasErrors()) {
            return "redirect:/";
        }

        Page<Content> paging = contentService.getList(contentDTO);

        model.addAttribute("paging", paging);

        return "content/list";

    }

    @GetMapping("view")
    public String getView(@Valid ContentDTO contentDTO, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "content/list";
        }

        return "content/view";
    }

}
