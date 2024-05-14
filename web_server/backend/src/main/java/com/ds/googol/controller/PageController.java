package com.ds.googol.controller;

import jakarta.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
// Anotations
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.ui.Model;

import com.ds.googol.beans.WebClient;




@Controller
public class PageController {
    
	private int id_counter = 0;

	@Resource(name = "sessionScopedNumberGenerator")
    private WebClient client;
	
	@Value("${registryName}")
	private String registryName;

	@Value("${characterLimit}")
	private int characterLimit;
	
    @GetMapping("/index")
	public String index() {
		
		return "index";
	}
	
	@Bean
	@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public WebClient sessionScopedNumberGenerator() {
		return new WebClient(id_counter++, registryName, characterLimit);
	}

	@PostMapping("/index")
	public String index(@RequestParam("option") String option, Model model) {
		
		System.out.println(option);
		return "redirect:/index";
	}

	@GetMapping("/search-terms")
	public String searchTerms(Model model) {
		model.addAttribute("title", "Terms Search");
		model.addAttribute("actionDescription", "Type the terms you want to search for:");
		model.addAttribute("actionUrl", "/search-terms");
		return "search_or_add";
	}

	@PostMapping("/search-terms")
	public String searchTermsResult(@RequestParam("input") String terms, Model model) {
		
		
		model.addAttribute("termsResult", client.searchByTerms(terms,0));
		return "terms_result";
	}



	@GetMapping("/counter")
	public String counter(Model model) {
		System.out.println("User Id: " + client.getId());
		return "index";
	}
}