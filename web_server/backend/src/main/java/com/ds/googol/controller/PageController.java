package com.ds.googol.controller;

import gateway_client_dependencies.*;

// Anotations
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.ui.Model;




@Controller
public class PageController {
    
    @GetMapping("/index")
	public String index() {
		
		return "index";
	}

	@PostMapping("/index")
	public String index(@RequestParam("option") String option, Model model) {
		
		System.out.println(option);
		return "index";
	}
}