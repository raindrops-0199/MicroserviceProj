package com.heima.freemarker.controller;

import com.heima.freemarker.entity.Student;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class FreemarkerDemoController {

    @GetMapping("/test")
    public String test(Model model) {
        // name
        model.addAttribute("name", "freemarker-demo");
        // stu
        Student student = new Student();
        student.setName("Jason");
        student.setAge(18);
        model.addAttribute("stu", student);

        return "01-basic";
    }
}
