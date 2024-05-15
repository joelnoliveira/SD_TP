package com.ds.googol.controller;

import jakarta.annotation.Resource;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
// Anotations
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.ui.Model;

import com.ds.googol.beans.WebClient;




@Controller
public class PageController {
    
	private int id_counter = Integer.MAX_VALUE;

	@Resource(name = "sessionScopedNumberGenerator")
    private WebClient client;
	
	@Value("${registryName}")
	private String registryName;// get the value from the application.properties file

	@Value("${characterLimit}")// get the value from the application.properties file
	private int characterLimit;

    @GetMapping("/index")
	public String index() {
		
		return "index";
	}
	
	@Bean
	@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
	public WebClient sessionScopedNumberGenerator() {
		try {
			return new WebClient(id_counter--, registryName, characterLimit);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Gateway is down, cannot create WebClient. Please try again later");
		}
	}

	@PostMapping("/index")
	public String index(@RequestParam("option") String option, Model model) {
		
		switch (option) {

			case "0":
				return "redirect:/add-url";
			case "1":
				return "redirect:/search-page";
			case "2":
				return "redirect:/search-page?type=url";
			default:
				break;
		}
		return "redirect:/index";
	}

	@GetMapping("/add-url")
	public String addURL(Model model) {
		model.addAttribute("title", "Add URL");
		model.addAttribute("actionDescription", "Type the URL you want to add:");
		model.addAttribute("actionUrl", "/add-url");
		return "search_or_add";
	}

	@PostMapping("/add-url")
	public String addURL(@RequestParam("input") String url, Model model) {
		
		if(client.addURL(url))
			model.addAttribute("message", "URL added successfully");
		else
			model.addAttribute("message", "URL could not be added");
		return "message";
	}

	@GetMapping("/search-page")
	public String searchTerms(Model model, @RequestParam(name = "type", required=false, defaultValue = "term") String type){

		if(type.equals("term")){
			model.addAttribute("title", "Terms Search");
			model.addAttribute("actionDescription", "Type the terms you want to search for:");
			model.addAttribute("actionUrl", "/search");

		}else if(type.equals("url")){
			model.addAttribute("title", "Links to URL Search");
			model.addAttribute("actionDescription", "Type the URL you want to search for:");
			model.addAttribute("actionUrl", "/search?type=url");

		}
		else{
			model.addAttribute("error", "Bad request");
			model.addAttribute("status", "400");
			model.addAttribute("message", "The request could not be understood by the server due to malformed syntax: type="+type);
			return "error";
		}
		return "search_or_add";
	}

	@GetMapping("/search")
	public String termsResult(@RequestParam("input") String input,
	@RequestParam("batchCount") int batchCount, @RequestParam(name = "type", required=false, defaultValue = "term") String type, Model model) {

		int previous, next;
		List<String> result;


		if(batchCount <= 0){
			batchCount = 0;
			previous = -1;
		}else previous = 1;

		if(type.equals("term")){
			
			input = input.toLowerCase();
			result = client.searchByTerms(input, batchCount);
		}
		else if(type.equals("url"))result = client.consultURL(input, batchCount);
		else{
			model.addAttribute("error", "Bad request");
			model.addAttribute("status", "400");
			model.addAttribute("message", "The request could not be understood by the server due to malformed syntax: type="+type);
			return "error";
		}

		// check if there are no errors
		if(result == null){
			model.addAttribute("error", "Internal Server Error");
			model.addAttribute("status", "500");
			model.addAttribute("message", "The server encountered an unexpected condition which prevented it from fulfilling the request");
			return "error";
		}

		for(int i = 0; i < result.size(); i++){
			
			String string = result.get(i);
			// tokenize the string
			
			if(type.equals("term")){

				String[] tokens = string.split("\n");
				StringBuilder sb = new StringBuilder();
	
				sb.append(tokens[0]+"<br>");
				if(tokens.length > 1)sb.append("<a href=\""+tokens[1]+"\">"+ tokens[1] +"</a><br>");
	
				for(int j = 2; j < tokens.length; j++){
					sb.append(tokens[j]+"<br>");
				}
	
				result.set(i, sb.toString());

			}else{

				result.set(i, "<a href=\""+ string.replace("\n", "") +"\">"+ string +"</a><br>");
			}
		}
		
		if(result.size() < 10){

			next = -1;
			if(result.size() == 0)result.add("No results were found");
		}
		else next = 1;
		
		model.addAttribute("termsResult", result);
		model.addAttribute("input", input);
		model.addAttribute("batchCount", batchCount);
		model.addAttribute("previous", previous);
		model.addAttribute("next", next);
		model.addAttribute("actionUrl", "/search?input=" + input + "&batchCount="+ batchCount+"&type="+type);
		
		return "results";
	}

	@PostMapping("/search")
	public String termsResultPost(@RequestParam(name = "action",required=false, defaultValue = "Previous") String action,
	@RequestParam("input") String terms,
	@RequestParam(name = "batchCount", required=false, defaultValue = "0") int batchCount, @RequestParam(name = "type", required=false, defaultValue = "term") String type, Model model) {

		if ("Previous".equals(action)) {
            // Previous button was pressed
			batchCount -= 1;
			if(batchCount <= 0)batchCount = 0;
        } else {
            // Next button was pressed
			batchCount += 1;
        }

		return "redirect:/search?input=" + terms + "&batchCount="+ batchCount + "&type=" + type;
	}

	@GetMapping("/counter")
	public String counter(Model model) {
		System.out.println("User Id: " + client.getId());
		return "index";
	}
}